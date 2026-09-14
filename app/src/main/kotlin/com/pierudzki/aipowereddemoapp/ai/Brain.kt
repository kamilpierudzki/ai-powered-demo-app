package com.pierudzki.aipowereddemoapp.ai

import android.content.Context
import com.google.ai.edge.litertlm.Content
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

class Brain {

    // Written only by the turn holding navigationMutex, under conversationLock; read by close()
    // under conversationLock. Invariant at every monitor boundary: null or alive.
    private var navigationConversation: Conversation? = null

    private val _answer = MutableStateFlow<Answer>(ShowWelcomeScreen)
    val answer: StateFlow<Answer> = _answer.asStateFlow()

    private val navigationConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.2)

    private val navigationToolProvider = tool(NavigationTools())

    private val engineHolder = EngineHolder()
    private val screenTexts = ScreenTextsGenerator(engineHolder)

    // Serializes navigation turns. It protects three Brain-private invariants: a single live
    // navigation conversation, a single in-flight Conversation.sendMessage (a blocking JNI call
    // with no locking of its own), and a "Current screen" prefix that reflects the previous
    // turn's result. Non-reentrant: NavigationTools callbacks run inside sendMessage, i.e. while
    // the lock is held, so a tool must never dispatch an action back into onNewInputAction.
    private val navigationMutex = Mutex()

    @Volatile
    private var closed = false

    // viewModelScope is already cancelled by the time the owning ViewModel reaches onCleared(),
    // so teardown needs a scope of its own that survives clear(). The work it carries is bounded
    // by at most one in-flight navigation turn.
    private val teardownScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Guards the short, non-blocking native calls that create, close or cancel the navigation
    // conversation, so close() (which runs on Main without navigationMutex) can never call
    // cancelProcess() on a conversation the in-flight turn is deleting at the same instant.
    // Never held across sendMessage. Lock order: navigationMutex (if held) -> conversationLock.
    private val conversationLock = Any()

    val engineState: StateFlow<EngineState> = engineHolder.state

    val paramsTexts: StateFlow<ParamsSettingScreenTexts> get() = screenTexts.paramsTexts
    val calculationTexts: StateFlow<CalculationScreenTexts> get() = screenTexts.calculationTexts
    val successTexts: StateFlow<ResultScreenTexts> get() = screenTexts.successTexts
    val failureTexts: StateFlow<ResultScreenTexts> get() = screenTexts.failureTexts

    suspend fun initializeEngine(context: Context) = engineHolder.initialize(context)

    /**
     * Stops accepting actions and closes the navigation conversation and the engine once the
     * in-flight navigation turn (if any) has released [navigationMutex], so the navigation
     * conversation and the engine are never deleted while a navigation turn is still inside
     * sendMessage. cancelProcess() is a best-effort attempt to shorten that turn and is taken
     * under [conversationLock], so it cannot run against a conversation the turn is closing.
     * Screen-text conversations (ScreenTextsGenerator) run outside both locks and are not covered.
     */
    fun close() {
        closed = true
        synchronized(conversationLock) {
            try {
                navigationConversation?.takeIf { it.isAlive }?.cancelProcess()
            } catch (e: Exception) {
                android.util.Log.d("Brain", "close(): cancelProcess failed: ${e.message}")
            }
        }
        teardownScope.launch {
            navigationMutex.withLock {
                resetNavigationConversation()
                engineHolder.close()
            }
        }
    }

    /**
     * Runs one navigation turn for [action]. Turns are serialized through [navigationMutex]:
     * an action flagged [Action.isDroppableWhenBusy] is dropped instead of queued when another
     * turn is in flight, every other action waits for its turn.
     *
     * Returns false when the action was dropped, either because the Brain was busy or because it
     * has already been closed. The lock is taken before switching to Dispatchers.IO so the drop
     * decision is made synchronously, in call order, on the caller's dispatcher.
     */
    suspend fun onNewInputAction(action: Action): Boolean {
        if (closed) return false
        if (action.isDroppableWhenBusy) {
            if (!navigationMutex.tryLock()) return false
            try {
                navigate(action)
            } finally {
                navigationMutex.unlock()
            }
        } else {
            navigationMutex.withLock { navigate(action) }
        }
        return true
    }

    private suspend fun navigate(action: Action) = withContext(Dispatchers.IO) {
        val activeEngine = engineHolder.engine ?: return@withContext
        try {
            if (action.startsFreshNavigationConversation) {
                resetNavigationConversation()
            }
            val conversation = ensureNavigationConversation(activeEngine)
            val message = "Current screen: ${_answer.value.destination.id}.\n${action.prompt}"
            android.util.Log.d("Brain", "Action: message: $message")
            val response = conversation.sendMessage(message)
            android.util.Log.d("Brain", "Action response: $response")
        } catch (e: Exception) {
            android.util.Log.d("Brain", "Action error: ${e.message}")
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
                    NavigationPrompt.build(calculationTimeLimitSeconds = CALCULATION_TIME_LIMIT_SECONDS)
                ),
                tools = listOf(navigationToolProvider),
                automaticToolCalling = true,
                samplerConfig = navigationConfig,
            ),
        ).also { created -> synchronized(conversationLock) { navigationConversation = created } }
    }

    suspend fun generateParamsTexts(language: String) = screenTexts.generateParamsTexts(language)

    suspend fun generateCalculationTexts(language: String) =
        screenTexts.generateCalculationTexts(language)

    suspend fun generateSuccessTexts(language: String) = screenTexts.generateSuccessTexts(language)

    suspend fun generateFailureTexts(language: String) = screenTexts.generateFailureTexts(language)

    private inner class NavigationTools : ToolSet {

        @Tool(description = "Show the welcome screen with the button that starts the app.")
        fun showWelcomeScreen(): String {
            _answer.value = ShowWelcomeScreen.also {
                android.util.Log.d("Brain", "Tool calling, $it")
            }
            return "Showing the welcome screen."
        }

        @Tool(description = "Show the parameters screen where the user sets the app language and the N value for the Fibonacci sequence.")
        fun showParamsScreen(
            @ToolParam(description = "The current or updated N value for the Fibonacci sequence.") n: Int,
            @ToolParam(description = "The current or updated app language, for example English or Polish.") appLanguage: String,
        ): String {
            _answer.value = ShowParamsSettingScreen(n = n, appLanguage = appLanguage).also {
                android.util.Log.d("Brain", "Tool calling, $it")
            }
            return "Showing the parameters screen."
        }

        @Tool(description = "Show the calculation screen that runs the Fibonacci calculation for N and shows the produced values.")
        fun showCalculationScreen(
            @ToolParam(description = "The N value for the Fibonacci sequence to compute.") n: Int,
            @ToolParam(description = "The current app language, for example English or Polish.") appLanguage: String,
        ): String {
            _answer.value = ShowCalculationScreen(n = n, appLanguage = appLanguage).also {
                android.util.Log.d("Brain", "Tool calling, $it")
            }
            return "Showing the calculation screen."
        }

        @Tool(description = "Show the success screen, used when the Fibonacci calculation finished within the allowed time limit.")
        fun showSuccessScreen(
            @ToolParam(description = "The current app language, for example English or Polish.") appLanguage: String,
        ): String {
            _answer.value = ShowSuccessScreen(appLanguage = appLanguage).also {
                android.util.Log.d("Brain", "Tool calling, $it")
            }
            return "Showing the success screen."
        }

        @Tool(description = "Show the failure screen, used when the Fibonacci calculation ran longer than the allowed time limit and was interrupted.")
        fun showFailureScreen(
            @ToolParam(description = "The current app language, for example English or Polish.") appLanguage: String,
        ): String {
            _answer.value = ShowFailureScreen(appLanguage = appLanguage).also {
                android.util.Log.d("Brain", "Tool calling, $it")
            }
            return "Showing the failure screen."
        }
    }
}
