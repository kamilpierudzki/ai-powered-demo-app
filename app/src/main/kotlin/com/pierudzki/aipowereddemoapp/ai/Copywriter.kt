package com.pierudzki.aipowereddemoapp.ai

import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.pierudzki.aipowereddemoapp.ai.prompt.ScreenTextsPrompts
import com.pierudzki.aipowereddemoapp.core.AppDestination
import com.pierudzki.aipowereddemoapp.core.CalculationScreenTexts
import com.pierudzki.aipowereddemoapp.core.ParamsSettingScreenTexts
import com.pierudzki.aipowereddemoapp.core.ResultScreenTexts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/**
 * The model's second personality, owned by [Agent]. The model writes the copy; this class is the
 * Kotlin harness that asks for it: the same on-device model that navigates the app is asked here,
 * at `temperature = 1.0` and without tools, to write one screen's texts (title, hints, buttons,
 * message) in the language the user typed. Every request is a fresh single-turn conversation whose
 * system instruction is that screen's prompt from [ScreenTextsPrompts]; the reply is a minified
 * JSON object.
 *
 * Parsing is lenient on purpose: LiteRT-LM 0.13.1 has no constrained-decoding / response-format
 * option, so the reply is scanned for its outermost `{...}` (code fences and chatter around it are
 * ignored) and a missing, null or blank field falls back to [FALLBACK_TEXT] individually. When no
 * JSON object can be found, the engine is gone or the native call throws, the whole screen shows
 * [FALLBACK_TEXT] and the failure is logged under [TAG].
 *
 * No caching, on purpose: every time a screen is shown it asks the model for fresh texts, so the
 * copy is worded a little differently on each visit. That is the point of the demo; a per-language
 * cache would be the obvious optimization for a real product, not for this one. A failed attempt
 * shows the [FALLBACK_TEXT] marker and the next visit simply asks again. Requests for the same
 * screen are serialized by a per-screen [Mutex] taken before switching to Dispatchers.IO (like
 * `Agent.onNewInputAction`), so they are handled in call order and the last requested language is
 * the one that ends up on screen; a request that arrives while one is in flight (for example after
 * a rotation) waits and then generates again, on purpose. Different screens still generate in
 * parallel with each other and with navigation: nothing here takes any Agent lock, so text
 * generation never delays a navigation decision. While the model writes, the screen shows its
 * "Loading..." placeholders (the params screen additionally disables its language controls).
 *
 * Lifecycle: [Agent] creates it and, in `Agent.close()`, calls [close] (refuse new requests, ask
 * in-flight conversations to stop) and then [awaitIdle] before closing the engine, so the engine
 * is never freed under a running `sendMessage`.
 */
class Copywriter(
    private val engineHolder: EngineHolder,
) {

    private val creativeConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0)

    // Set once by close() (synchronously, on Main); read under each slot's lock, so no generation
    // starts after teardown began and none publishes a reply that cancelProcess() cut short.
    @Volatile
    private var closed = false

    // Guards the short native cancelProcess() in close() against the generating thread deleting
    // the same conversation at the same instant (same pattern and reason as Agent.conversationLock;
    // the two locks are never nested). Never held across sendMessage.
    private val conversationLock = Any()

    private val params = Slot(
        screen = AppDestination.PARAMS,
        initial = PARAMS_LOADING,
        fallback = PARAMS_FALLBACK,
        prompt = ScreenTextsPrompts::paramsTexts,
    ) { json ->
        ParamsSettingScreenTexts(
            languageHint = json.stringOr("languageHint"),
            changeLanguageButton = json.stringOr("changeLanguageButton"),
            nHint = json.stringOr("nHint"),
            saveNButton = json.stringOr("saveNButton"),
            title = json.stringOr("title"),
            loading = false,
        )
    }

    private val calculation = Slot(
        screen = AppDestination.CALCULATION,
        initial = CALCULATION_LOADING,
        fallback = CALCULATION_FALLBACK,
        prompt = ScreenTextsPrompts::calculationTexts,
    ) { json ->
        CalculationScreenTexts(
            title = json.stringOr("title"),
            message = json.stringOr("message"),
        )
    }

    private val success = Slot(
        screen = AppDestination.SUCCESS,
        initial = RESULT_LOADING,
        fallback = RESULT_FALLBACK,
        prompt = ScreenTextsPrompts::successTexts,
        parse = ::parseResultTexts,
    )

    private val failure = Slot(
        screen = AppDestination.FAILURE,
        initial = RESULT_LOADING,
        fallback = RESULT_FALLBACK,
        prompt = ScreenTextsPrompts::failureTexts,
        parse = ::parseResultTexts,
    )

    // Declared after the four slots on purpose: property initializers run in declaration order.
    private val slots = listOf(params, calculation, success, failure)

    val paramsTexts: StateFlow<ParamsSettingScreenTexts> = params.texts
    val calculationTexts: StateFlow<CalculationScreenTexts> = calculation.texts
    val successTexts: StateFlow<ResultScreenTexts> = success.texts
    val failureTexts: StateFlow<ResultScreenTexts> = failure.texts

    /** Asks the model for fresh params screen texts in [language]; every call runs an inference, on purpose. */
    suspend fun generateParamsTexts(language: String) = params.generate(language)

    suspend fun generateCalculationTexts(language: String) = calculation.generate(language)

    suspend fun generateSuccessTexts(language: String) = success.generate(language)

    suspend fun generateFailureTexts(language: String) = failure.generate(language)

    /**
     * Refuses new generations and asks the in-flight ones to stop (best effort, like
     * `Agent.close()`). Not suspending, so `Agent.close()` can call it synchronously on Main
     * before it launches the teardown coroutine.
     */
    fun close() {
        closed = true
        synchronized(conversationLock) { slots.forEach { it.cancelInFlight() } }
    }

    /**
     * Suspends until no generation is inside sendMessage any more. Call after [close] and before
     * the engine is closed. Takes no Agent lock, so the caller may take navigationMutex afterwards.
     */
    suspend fun awaitIdle() {
        slots.forEach { it.awaitIdle() }
    }

    /** One screen's texts: its flow, placeholder, fallback, prompt, parser and in-flight conversation. */
    private inner class Slot<T>(
        private val screen: AppDestination,
        private val initial: T,
        private val fallback: T,
        private val prompt: (language: String) -> String,
        private val parse: (JSONObject) -> T,
    ) {
        private val _texts = MutableStateFlow(initial)
        // Created once: collectAsStateWithLifecycle keys its producer on the flow instance.
        val texts: StateFlow<T> = _texts.asStateFlow()

        // One generation per screen at a time: results are published in call order (the last
        // requested language wins), awaitIdle() waits on it, and inFlight can stay a single field.
        private val mutex = Mutex()

        // Non-null only while sendMessage runs. Read and written under conversationLock.
        private var inFlight: Conversation? = null

        suspend fun generate(language: String) {
            // Lock first, switch dispatcher second (like Agent.onNewInputAction): callers on
            // Main.immediate reach the lock in call order, so the last requested language wins.
            mutex.withLock {
                if (closed) return
                _texts.value = initial // every visit starts from "Loading...": no cache, on purpose
                val generated = withContext(Dispatchers.IO) { askModel(language) }
                if (closed) return // cut short by cancelProcess(): never publish a truncated reply
                _texts.value = generated ?: fallback
            }
        }

        // Blocking; runs on Dispatchers.IO. Deliberately not suspend: there is no suspension point
        // inside the try, so the catch below can never swallow a kotlinx CancellationException.
        // (If a suspend call is ever added here, rethrow CancellationException before the generic catch.)
        private fun askModel(language: String): T? {
            val tag = "${screen.id}($language)"
            val activeEngine = engineHolder.engine ?: run {
                android.util.Log.d(TAG, "$tag: engine not available, using fallback")
                return null
            }
            return try {
                activeEngine.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(prompt(language)),
                        automaticToolCalling = false,
                        samplerConfig = creativeConfig,
                    ),
                ).use { conversation ->
                    synchronized(conversationLock) { inFlight = conversation }
                    try {
                        val raw = conversation.sendMessage(REQUEST_MESSAGE).text()
                        android.util.Log.d(TAG, "$tag: response: $raw")
                        extractJsonObject(raw)?.let(parse) ?: run {
                            android.util.Log.d(TAG, "$tag: no JSON object in response, using fallback")
                            null
                        }
                    } finally {
                        // Cleared before use{} closes the conversation, so close() can never call
                        // cancelProcess() on a conversation that is being deleted.
                        synchronized(conversationLock) { inFlight = null }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.d(TAG, "$tag: error, using fallback: ${e.message}")
                null
            }
        }

        /** Caller holds [conversationLock]. Best effort, like Agent.close(). */
        fun cancelInFlight() {
            try {
                inFlight?.takeIf { it.isAlive }?.cancelProcess()
            } catch (e: Exception) {
                android.util.Log.d(TAG, "${screen.id}: cancelProcess failed: ${e.message}")
            }
        }

        /** Returns once the in-flight generation (if any) has released the lock. */
        suspend fun awaitIdle() = mutex.withLock { }
    }

    private companion object {
        const val TAG = "Copywriter"
        // The user turn that triggers the generation; the task itself is the system instruction.
        const val REQUEST_MESSAGE = "Propose texts."
        // Shown on every visit while the model writes that screen's texts (see the class KDoc).
        const val LOADING_TEXT = "Loading..."
        // An explicit marker rather than real copy: in a demo about model-written texts a failed
        // generation should be visible at a glance. Details are in Logcat under TAG.
        const val FALLBACK_TEXT = "Text generation failed"

        val PARAMS_LOADING = ParamsSettingScreenTexts(
            languageHint = LOADING_TEXT,
            changeLanguageButton = LOADING_TEXT,
            nHint = LOADING_TEXT,
            saveNButton = LOADING_TEXT,
            title = LOADING_TEXT,
            loading = true,
        )

        val PARAMS_FALLBACK = ParamsSettingScreenTexts(
            languageHint = FALLBACK_TEXT,
            changeLanguageButton = FALLBACK_TEXT,
            nHint = FALLBACK_TEXT,
            saveNButton = FALLBACK_TEXT,
            title = FALLBACK_TEXT,
            loading = false,
        )

        val CALCULATION_LOADING = CalculationScreenTexts(title = LOADING_TEXT, message = LOADING_TEXT)
        val CALCULATION_FALLBACK = CalculationScreenTexts(title = FALLBACK_TEXT, message = FALLBACK_TEXT)

        // Shared by the success and failure screens: both show the same marker on failure.
        val RESULT_LOADING = ResultScreenTexts(title = LOADING_TEXT, message = LOADING_TEXT)
        val RESULT_FALLBACK = ResultScreenTexts(title = FALLBACK_TEXT, message = FALLBACK_TEXT)

        fun parseResultTexts(json: JSONObject): ResultScreenTexts = ResultScreenTexts(
            title = json.stringOr("title"),
            message = json.stringOr("message"),
        )

        fun Message.text(): String = contents.contents
            .filterIsInstance<Content.Text>()
            .joinToString("") { it.text }
            .trim()

        /**
         * The prompt asks for a bare minified object, but fences or chatter around it are
         * tolerated. Returns null when there is no `{...}` at all or it is not valid JSON.
         */
        fun extractJsonObject(raw: String): JSONObject? {
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            if (start < 0 || end <= start) return null
            return try {
                JSONObject(raw.substring(start, end + 1))
            } catch (e: JSONException) {
                null
            }
        }

        /**
         * Missing key, JSON null, non-string value or blank string -> [fallback], per field.
         * (org.json's optString would turn a JSON null into the literal string "null".)
         */
        fun JSONObject.stringOr(key: String, fallback: String = FALLBACK_TEXT): String =
            (opt(key) as? String)?.takeIf { it.isNotBlank() } ?: fallback
    }
}
