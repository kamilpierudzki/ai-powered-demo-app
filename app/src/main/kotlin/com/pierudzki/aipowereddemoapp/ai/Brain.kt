package com.pierudzki.aipowereddemoapp.ai

import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.pierudzki.aipowereddemoapp.ai.action.Action
import com.pierudzki.aipowereddemoapp.ai.answer.Answer
import com.pierudzki.aipowereddemoapp.ai.answer.ShowCalculationScreen
import com.pierudzki.aipowereddemoapp.ai.answer.ShowFailureScreen
import com.pierudzki.aipowereddemoapp.ai.answer.ShowParamsSettingScreenAndRefreshTexts
import com.pierudzki.aipowereddemoapp.ai.answer.ShowSuccessScreen
import com.pierudzki.aipowereddemoapp.ai.answer.ShowWelcomeScreen
import com.pierudzki.aipowereddemoapp.core.AppDestination
import com.pierudzki.aipowereddemoapp.core.ParamsSettingScreenTexts
import com.pierudzki.aipowereddemoapp.core.ResultScreenTexts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

class Brain {

    private var appLanguage: String = DEFAULT_APP_LANGUAGE
    private var n: Int = DEFAULT_N

    private val _answer = MutableStateFlow<Answer>(ShowWelcomeScreen)
    val answer: StateFlow<Answer> = _answer.asStateFlow()

    private val _paramsTexts = MutableStateFlow(LOADING_PARAMS_TEXTS)
    val paramsTexts: StateFlow<ParamsSettingScreenTexts> = _paramsTexts.asStateFlow()

    private val _calculationHint = MutableStateFlow(LOADING_TEXT)
    val calculationHint: StateFlow<String> = _calculationHint.asStateFlow()

    private val _successTexts = MutableStateFlow(LOADING_RESULT_TEXTS)
    val successTexts: StateFlow<ResultScreenTexts> = _successTexts.asStateFlow()

    private val _failureTexts = MutableStateFlow(LOADING_RESULT_TEXTS)
    val failureTexts: StateFlow<ResultScreenTexts> = _failureTexts.asStateFlow()

    private var lastParamsTextsLanguage: String? = null
    private var lastCalculationHintLanguage: String? = null
    private var lastSuccessTextsLanguage: String? = null
    private var lastFailureTextsLanguage: String? = null

    // Navigation decisions stay predictable; screen texts are intentionally creative.
    private val navigationConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.2)
    private val creativeConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0)

    suspend fun onNewInputAction(action: Action) = withContext(Dispatchers.IO) {
        val activeEngine = EngineWrapper.engine ?: return@withContext
        try {
            activeEngine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(systemPrompt()),
                    automaticToolCalling = false,
                    samplerConfig = navigationConfig,
                ),
            ).use { conversation ->
                val response = conversation.sendMessage(action.prompt)
                val text = response.contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString("") { it.text }
                    .trim()
                _answer.value = parse(text)
            }
        } catch (e: Exception) {
            android.util.Log.d("Brain", "onNewInputAction error: ${e.message}")
        }
    }

    suspend fun generateParamsTexts(language: String) = withContext(Dispatchers.IO) {
        if (language == lastParamsTextsLanguage) return@withContext
        lastParamsTextsLanguage = language
        _paramsTexts.value = _paramsTexts.value.copy(loading = true)

        val activeEngine = EngineWrapper.engine ?: run {
            _paramsTexts.value = PARAMS_FALLBACK
            return@withContext
        }

        try {
            activeEngine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(paramsTextsPrompt(language)),
                    automaticToolCalling = false,
                    samplerConfig = creativeConfig,
                ),
            ).use { conversation ->
                val raw = conversation.sendMessage("Propose texts.").contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString("") { it.text }
                    .trim()
                _paramsTexts.value = parseParamsTexts(raw)
            }
        } catch (e: Exception) {
            _paramsTexts.value = PARAMS_FALLBACK
        }
    }

    suspend fun generateCalculationHint(language: String) = withContext(Dispatchers.IO) {
        if (language == lastCalculationHintLanguage) return@withContext
        lastCalculationHintLanguage = language

        val activeEngine = EngineWrapper.engine ?: run {
            _calculationHint.value = FALLBACK_TEXT
            return@withContext
        }

        try {
            activeEngine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(calculationHintPrompt(language)),
                    automaticToolCalling = false,
                    samplerConfig = creativeConfig,
                ),
            ).use { conversation ->
                val raw = conversation.sendMessage("Propose text.").contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString("") { it.text }
                    .trim()
                _calculationHint.value = if (raw.isEmpty()) FALLBACK_TEXT else raw
            }
        } catch (e: Exception) {
            _calculationHint.value = FALLBACK_TEXT
        }
    }

    suspend fun generateSuccessTexts(language: String) = withContext(Dispatchers.IO) {
        if (language == lastSuccessTextsLanguage) return@withContext
        lastSuccessTextsLanguage = language
        _successTexts.value = resultTextsFor(successTextsPrompt(language))
    }

    suspend fun generateFailureTexts(language: String) = withContext(Dispatchers.IO) {
        if (language == lastFailureTextsLanguage) return@withContext
        lastFailureTextsLanguage = language
        _failureTexts.value = resultTextsFor(failureTextsPrompt(language))
    }

    private fun resultTextsFor(prompt: String): ResultScreenTexts {
        val activeEngine = EngineWrapper.engine ?: return RESULT_FALLBACK
        return try {
            activeEngine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(prompt),
                    automaticToolCalling = false,
                    samplerConfig = creativeConfig,
                ),
            ).use { conversation ->
                val raw = conversation.sendMessage("Propose texts.").contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString("") { it.text }
                    .trim()
                parseResultTexts(raw)
            }
        } catch (e: Exception) {
            RESULT_FALLBACK
        }
    }

    private fun currentScreenId(): String = when (_answer.value) {
        is ShowWelcomeScreen -> AppDestination.WELCOME.id
        is ShowParamsSettingScreenAndRefreshTexts -> AppDestination.PARAMS.id
        is ShowCalculationScreen -> AppDestination.CALCULATION.id
        is ShowSuccessScreen -> AppDestination.SUCCESS.id
        is ShowFailureScreen -> AppDestination.FAILURE.id
    }

    private fun systemPrompt(): String =
        """
        You are the navigation brain of an Android app. Based on the user's action and the current
        state, decide which screen the app should show next.

        Available screens:
        - "welcome": the welcome screen with a button that starts the app.
        - "params": the screen where the user sets the app language and the N value for the
          Fibonacci sequence.
        - "calculation": the screen that runs the Fibonacci calculation for N and shows the
          produced values.
        - "success": the screen shown after the Fibonacci calculation finishes within the allowed
          time limit.
        - "failure": the screen shown when the Fibonacci calculation runs longer than the allowed
          time limit and must be interrupted.

        Current state:
        - currentScreen: "${currentScreenId()}"
        - appLanguage: "$appLanguage"
        - n: $n
        - calculationTimeLimitSeconds: $CALCULATION_TIME_LIMIT_SECONDS

        Rules:
        - When the user is ready to start from the welcome screen, go to "params".
        - When the user changes the app language, stay on "params" and update appLanguage.
        - When the user finishes setting the parameters, go to "calculation".
        - The Fibonacci calculation must finish within $CALCULATION_TIME_LIMIT_SECONDS seconds.
        - When you are told the calculation has been running for more than
          $CALCULATION_TIME_LIMIT_SECONDS seconds, go to "failure".
        - When you are told the calculation finished, go to "success".
        - When the user presses the back button while on "calculation", go to "params".
        - When the user presses the back button while on "params", go to "welcome".
        - When the user presses the back button while on "success" or "failure", go to "params".

        Respond with ONLY a single minified JSON object, without markdown code fences and without
        any extra text or explanation. Use exactly these keys:
        - "screen": one of "welcome", "params", "calculation", "success", "failure".
        - "n": integer, the current or updated N value.
        - "appLanguage": string, the current or updated app language.

        Example of the exact required format:
        {"screen":"params","n":10,"appLanguage":"English"}
        """.trimIndent()

    private fun parse(raw: String): Answer = try {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        val json = JSONObject(raw.substring(start, end + 1))
        appLanguage = json.optString("appLanguage", appLanguage)
        n = json.optInt("n", n)
        when (AppDestination.fromId(json.optString("screen"))) {
            AppDestination.PARAMS -> ShowParamsSettingScreenAndRefreshTexts(n = n, appLanguage = appLanguage)
            AppDestination.WELCOME -> ShowWelcomeScreen
            AppDestination.CALCULATION -> ShowCalculationScreen(n = n, appLanguage = appLanguage)
            AppDestination.SUCCESS -> ShowSuccessScreen(appLanguage = appLanguage)
            AppDestination.FAILURE -> ShowFailureScreen(appLanguage = appLanguage)
            null -> _answer.value
        }
    } catch (e: Exception) {
        _answer.value
    }

    private fun paramsTextsPrompt(appLanguage: String): String =
        """
        You manage an Android mobile app. Suggest text for elements on the app' screen.

        These texts are:
        - The prompt text for the text field where the user enters the language in which
        the app's on-screen text should appear. This text can be up to 20 words long.
        - The button text that triggers on-screen text updates. This text can be up to 4 words long.
        - The prompt text for the text field where the user enters the selected value N for
        the Fibonacci sequence. The prompt text can be up to 20 words long.
        - The button text that updates the value N selected by the user in the app's memory.
        This text can be up to 4 words long. This button triggers the calculation of the Fibonacci.

        The user wants the text to be in the following language: "${appLanguage}."

        Respond with ONLY a single minified JSON object, without markdown code fences and
        without any extra text or explanation. Use exactly these keys and meanings:
        - "languageHint": the text field hint for the app language (item 1 above).
        - "changeLanguageButton": the button text that triggers text updates (item 2 above).
        - "nHint": the text field hint for the N value (item 3 above).
        - "changeNButton": the button text that tells the app to update the N value (item 4 above).

        Example of the exact required format:
        {"languageHint":"...","changeLanguageButton":"...","nHint":"...","changeNButton":"..."}
        """.trimIndent()

    private fun calculationHintPrompt(appLanguage: String): String =
        """
        You manage an Android mobile app. Suggest a short status text shown on the screen
        while the app is calculating the Fibonacci sequence. The text tells the user that a
        calculation is in progress and to please wait. It can be up to 10 words long.

        The user wants the text to be in the following language: "${appLanguage}."

        Respond with ONLY the status text itself, as plain text. Do not use JSON, markdown,
        quotes, labels, or any extra explanation.
        """.trimIndent()

    private fun parseParamsTexts(raw: String): ParamsSettingScreenTexts = try {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        val json = JSONObject(raw.substring(start, end + 1))
        ParamsSettingScreenTexts(
            languageHint = json.optString("languageHint", PARAMS_FALLBACK.languageHint),
            changeLanguageButton = json.optString("changeLanguageButton", PARAMS_FALLBACK.changeLanguageButton),
            nHint = json.optString("nHint", PARAMS_FALLBACK.nHint),
            saveNButton = json.optString("changeNButton", PARAMS_FALLBACK.saveNButton),
            loading = false,
        )
    } catch (e: Exception) {
        PARAMS_FALLBACK
    }

    private fun successTextsPrompt(appLanguage: String): String =
        """
        You manage an Android mobile app. Suggest text for the success screen shown after the
        Fibonacci calculation finished within the allowed time limit.

        These texts are:
        - A short screen title shown in the top app bar. Up to 3 words.
        - A short congratulatory message telling the user the calculation finished successfully.
        Up to 15 words.

        The user wants the text to be in the following language: "${appLanguage}."

        Respond with ONLY a single minified JSON object, without markdown code fences and
        without any extra text or explanation. Use exactly these keys and meanings:
        - "title": the screen title.
        - "message": the success message.

        Example of the exact required format:
        {"title":"...","message":"..."}
        """.trimIndent()

    private fun failureTextsPrompt(appLanguage: String): String =
        """
        You manage an Android mobile app. Suggest text for the failure screen shown when the
        Fibonacci calculation took too long and had to be interrupted.

        These texts are:
        - A short screen title shown in the top app bar. Up to 3 words.
        - A short message telling the user the calculation took too long and was stopped.
        Up to 15 words.

        The user wants the text to be in the following language: "${appLanguage}."

        Respond with ONLY a single minified JSON object, without markdown code fences and
        without any extra text or explanation. Use exactly these keys and meanings:
        - "title": the screen title.
        - "message": the failure message.

        Example of the exact required format:
        {"title":"...","message":"..."}
        """.trimIndent()

    private fun parseResultTexts(raw: String): ResultScreenTexts = try {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        val json = JSONObject(raw.substring(start, end + 1))
        ResultScreenTexts(
            title = json.optString("title", RESULT_FALLBACK.title),
            message = json.optString("message", RESULT_FALLBACK.message),
        )
    } catch (e: Exception) {
        RESULT_FALLBACK
    }

    private companion object {
        const val DEFAULT_APP_LANGUAGE = "English"
        const val DEFAULT_N = 10
        const val CALCULATION_TIME_LIMIT_SECONDS = 10
        const val LOADING_TEXT = "Loading..."
        const val FALLBACK_TEXT = "error"

        val LOADING_PARAMS_TEXTS = ParamsSettingScreenTexts(
            languageHint = LOADING_TEXT,
            changeLanguageButton = LOADING_TEXT,
            nHint = LOADING_TEXT,
            saveNButton = LOADING_TEXT,
            loading = true,
        )

        val PARAMS_FALLBACK = ParamsSettingScreenTexts(
            languageHint = FALLBACK_TEXT,
            changeLanguageButton = FALLBACK_TEXT,
            nHint = FALLBACK_TEXT,
            saveNButton = FALLBACK_TEXT,
            loading = false,
        )

        val LOADING_RESULT_TEXTS = ResultScreenTexts(
            title = LOADING_TEXT,
            message = LOADING_TEXT,
        )

        val RESULT_FALLBACK = ResultScreenTexts(
            title = FALLBACK_TEXT,
            message = FALLBACK_TEXT,
        )
    }
}
