package com.pierudzki.aipowereddemoapp.ai

import android.content.Context
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.google.ai.edge.litertlm.tool
import com.pierudzki.aipowereddemoapp.ai.action.Action
import com.pierudzki.aipowereddemoapp.ai.action.CALCULATION_TIME_LIMIT_SECONDS
import com.pierudzki.aipowereddemoapp.ai.answer.Answer
import com.pierudzki.aipowereddemoapp.ai.answer.ShowCalculationScreen
import com.pierudzki.aipowereddemoapp.ai.answer.ShowFailureScreen
import com.pierudzki.aipowereddemoapp.ai.answer.ShowParamsSettingScreen
import com.pierudzki.aipowereddemoapp.ai.answer.ShowSuccessScreen
import com.pierudzki.aipowereddemoapp.ai.answer.ShowWelcomeScreen
import com.pierudzki.aipowereddemoapp.ai.prompt.NavigationPrompt
import com.pierudzki.aipowereddemoapp.core.CalculationScreenTexts
import com.pierudzki.aipowereddemoapp.core.ParamsSettingScreenTexts
import com.pierudzki.aipowereddemoapp.core.ResultScreenTexts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The Kotlin harness around the on-device model; it runs the model's two personalities. The
 * navigation personality lives here: one long, low-temperature conversation with the screen tools
 * in [NavigationTools], fed by [Action]s and answered through [answer]. The copywriting personality
 * is delegated to [Copywriter]: short, high-temperature, tool-less conversations that produce each
 * screen's texts. Both share the single engine in [EngineHolder] and are closed together by [close].
 */
class Agent {

    // Written only by the turn holding navigationMutex, under conversationLock; read by close()
    // under conversationLock. Invariant at every monitor boundary: null or alive.
    private var navigationConversation: Conversation? = null

    private val _answer = MutableStateFlow<Answer>(ShowWelcomeScreen)
    val answer: StateFlow<Answer> = _answer.asStateFlow()

    private val navigationConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.2)

    private val navigationToolProvider = tool(NavigationTools())

    private val engineHolder = EngineHolder()
    private val copywriter = Copywriter(engineHolder)

    // Serializes navigation turns. It protects three Agent-private invariants: a single live
    // navigation conversation, a single in-flight Conversation.sendMessage (a blocking JNI call
    // with no locking of its own), and a "Current screen" prefix that reflects the previous
    // turn's result. Non-reentrant: NavigationTools callbacks run inside sendMessage, i.e. while
    // the lock is held, so a tool must never dispatch an action back into onNewInputAction.
    private val navigationMutex = Mutex()

    @Volatile
    private var closed = false

    // viewModelScope is already cancelled by the time the owning ViewModel reaches onCleared(),
    // so teardown needs a scope of its own that survives clear(). The work it carries is bounded
    // by at most one in-flight navigation turn plus at most one in-flight text generation per
    // screen.
    private val teardownScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Guards the short, non-blocking native calls that create, close or cancel the navigation
    // conversation, so close() (which runs on Main without navigationMutex) can never call
    // cancelProcess() on a conversation the in-flight turn is deleting at the same instant.
    // Never held across sendMessage. Lock order: navigationMutex (if held) -> conversationLock.
    private val conversationLock = Any()

    val engineState: StateFlow<EngineState> = engineHolder.state

    val paramsTexts: StateFlow<ParamsSettingScreenTexts> get() = copywriter.paramsTexts
    val calculationTexts: StateFlow<CalculationScreenTexts> get() = copywriter.calculationTexts
    val successTexts: StateFlow<ResultScreenTexts> get() = copywriter.successTexts
    val failureTexts: StateFlow<ResultScreenTexts> get() = copywriter.failureTexts

    suspend fun initializeEngine(context: Context) = engineHolder.initialize(context)

    /**
     * Stops accepting actions and closes the navigation conversation and the engine once the
     * in-flight navigation turn (if any) has released [navigationMutex], so the navigation
     * conversation and the engine are never deleted while a navigation turn is still inside
     * sendMessage. cancelProcess() is a best-effort attempt to shorten that turn and is taken
     * under [conversationLock], so it cannot run against a conversation the turn is closing.
     * Screen-text conversations take no Agent lock. [Copywriter.close] refuses new ones and asks
     * in-flight ones to stop, and the teardown waits for them ([Copywriter.awaitIdle]) before the
     * engine is closed, so no conversation of either kind is inside sendMessage when the engine
     * goes away.
     */
    fun close() {
        closed = true
        synchronized(conversationLock) {
            try {
                navigationConversation?.takeIf { it.isAlive }?.cancelProcess()
            } catch (e: Exception) {
                android.util.Log.d("Agent", "close(): cancelProcess failed: ${e.message}")
            }
        }
        copywriter.close()
        teardownScope.launch {
            // Text generations take no Agent lock; wait for them before taking navigationMutex so
            // a screen's lock is never held together with the navigation lock.
            copywriter.awaitIdle()
            navigationMutex.withLock {
                resetNavigationConversation()
                engineHolder.close()
            }
        }
    }

    /**
     * Runs one navigation turn for [action]. Turns are serialized through [navigationMutex]:
     * an action flagged [Action.isDroppableWhenBusy] is dropped instead of queued when another
     * turn is in flight, every other action waits for its turn. Once [close] has been called,
     * every action is dropped as well.
     *
     * A dropped action is silently ignored: nothing is sent to the model and [answer] is left
     * unchanged. The lock is taken before switching to Dispatchers.IO so the drop decision is
     * made synchronously, in call order, on the caller's dispatcher.
     */
    suspend fun onNewInputAction(action: Action) {
        if (closed) return
        if (action.isDroppableWhenBusy) {
            if (!navigationMutex.tryLock()) return
            try {
                navigate(action)
            } finally {
                navigationMutex.unlock()
            }
        } else {
            navigationMutex.withLock { navigate(action) }
        }
    }

    private suspend fun navigate(action: Action) = withContext(Dispatchers.IO) {
        val activeEngine = engineHolder.engine ?: return@withContext
        try {
            if (action.startsFreshNavigationConversation) {
                resetNavigationConversation()
            }
            val conversation = ensureNavigationConversation(activeEngine)
            val message = "Current screen: ${_answer.value.destination.id}. ${action.message}"
            android.util.Log.d("Agent", "USER message=\"$message\"")
            val response = conversation.sendMessage(message)
            if (response.toString().isNotEmpty()) {
                android.util.Log.d("Agent", "AGENT response=\"$response\"")
            }
        } catch (e: Exception) {
            android.util.Log.d("Agent", "AGENT error=\"${e.message}\"")
        }
    }

    private fun resetNavigationConversation() = synchronized(conversationLock) {
        navigationConversation?.close()
        navigationConversation = null
    }

    private fun ensureNavigationConversation(engine: Engine): Conversation {
        navigationConversation?.takeIf { it.isAlive }?.let { return it }
        // A conversation that is not alive is already closed; closing it again would throw.
        return engine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(
                    NavigationPrompt.withTimeLimit(CALCULATION_TIME_LIMIT_SECONDS)
                ),
                tools = listOf(navigationToolProvider),
                automaticToolCalling = true,
                samplerConfig = navigationConfig,
            ),
        ).also { created -> synchronized(conversationLock) { navigationConversation = created } }
    }

    suspend fun generateParamsTexts(language: String) = copywriter.generateParamsTexts(language)

    suspend fun generateCalculationTexts(language: String) =
        copywriter.generateCalculationTexts(language)

    suspend fun generateSuccessTexts(language: String) = copywriter.generateSuccessTexts(language)

    suspend fun generateFailureTexts(language: String) = copywriter.generateFailureTexts(language)

    private inner class NavigationTools : ToolSet {

        private fun printLog(message: String, answer: String) {
            android.util.Log.d("Agent", "Agent calls a tool=\"$message\" ($answer)")
        }

        @Tool(description = "Show the welcome screen with the button that starts the app.")
        fun showWelcomeScreen(): String {
            _answer.value = ShowWelcomeScreen
            return "Show the welcome screen.".also {
                printLog(message = it, answer = _answer.value.toString())
            }
        }

        @Tool(description = "Show the parameters screen where the user sets the app language and the N value for the Fibonacci sequence.")
        fun showParamsScreen(
            @ToolParam(description = "The current or updated N value for the Fibonacci sequence.") n: Int,
            @ToolParam(description = "The current or updated app language, for example English or Polish.") appLanguage: String,
        ): String {
            _answer.value = ShowParamsSettingScreen(n = n, appLanguage = appLanguage)
            return "Show the parameters screen.".also {
                printLog(message = it, answer = _answer.value.toString())
            }
        }

        @Tool(description = "Show the calculation screen that runs the Fibonacci calculation for N and shows the produced values.")
        fun showCalculationScreen(
            @ToolParam(description = "The N value for the Fibonacci sequence to compute.") n: Int,
            @ToolParam(description = "The current app language, for example English or Polish.") appLanguage: String,
        ): String {
            _answer.value = ShowCalculationScreen(n = n, appLanguage = appLanguage)
            return "Show the calculation screen.".also {
                printLog(message = it, answer = _answer.value.toString())
            }
        }

        @Tool(description = "Show the success screen, used when the Fibonacci calculation finished within the allowed time limit.")
        fun showSuccessScreen(
            @ToolParam(description = "The current app language, for example English or Polish.") appLanguage: String,
        ): String {
            _answer.value = ShowSuccessScreen(appLanguage = appLanguage)
            return "Show the success screen.".also {
                printLog(message = it, answer = _answer.value.toString())
            }
        }

        @Tool(description = "Show the failure screen, used when the Fibonacci calculation ran longer than the allowed time limit and was interrupted.")
        fun showFailureScreen(
            @ToolParam(description = "The current app language, for example English or Polish.") appLanguage: String,
        ): String {
            _answer.value = ShowFailureScreen(appLanguage = appLanguage)
            return "Show the failure screen.".also {
                printLog(message = it, answer = _answer.value.toString())
            }
        }
    }
}
