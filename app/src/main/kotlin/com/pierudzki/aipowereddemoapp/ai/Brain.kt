package com.pierudzki.aipowereddemoapp.ai

import android.content.Context
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
import com.pierudzki.aipowereddemoapp.ai.prompt.NavigationPrompt
import com.pierudzki.aipowereddemoapp.ai.prompt.ScreenTextsPrompts
import com.pierudzki.aipowereddemoapp.core.AppDestination
import com.pierudzki.aipowereddemoapp.core.CalculationScreenTexts
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

    private val _calculationTexts = MutableStateFlow(LOADING_CALCULATION_TEXTS)
    val calculationTexts: StateFlow<CalculationScreenTexts> = _calculationTexts.asStateFlow()

    private val _successTexts = MutableStateFlow(LOADING_RESULT_TEXTS)
    val successTexts: StateFlow<ResultScreenTexts> = _successTexts.asStateFlow()

    private val _failureTexts = MutableStateFlow(LOADING_RESULT_TEXTS)
    val failureTexts: StateFlow<ResultScreenTexts> = _failureTexts.asStateFlow()

    private var lastParamsTextsLanguage: String? = null
    private var lastCalculationTextsLanguage: String? = null
    private var lastSuccessTextsLanguage: String? = null
    private var lastFailureTextsLanguage: String? = null

    // Navigation decisions stay predictable; screen texts are intentionally creative.
    private val navigationConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.2)
    private val creativeConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0)

    private val engineWrapper = EngineWrapper()

    val engineState: StateFlow<EngineState> = engineWrapper.state

    suspend fun initializeEngine(context: Context) = engineWrapper.initialize(context)

    fun closeEngine() = engineWrapper.close()

    suspend fun onNewInputAction(action: Action) = withContext(Dispatchers.IO) {
        val activeEngine = engineWrapper.engine ?: return@withContext
        try {
            activeEngine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(
                        NavigationPrompt.build(
                            currentScreenId = currentScreenId(),
                            appLanguage = appLanguage,
                            n = n,
                            calculationTimeLimitSeconds = CALCULATION_TIME_LIMIT_SECONDS,
                        )
                    ),
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

        val activeEngine = engineWrapper.engine ?: run {
            _paramsTexts.value = PARAMS_FALLBACK
            return@withContext
        }

        try {
            activeEngine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(ScreenTextsPrompts.paramsTexts(language)),
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

    suspend fun generateCalculationTexts(language: String) = withContext(Dispatchers.IO) {
        if (language == lastCalculationTextsLanguage) return@withContext
        lastCalculationTextsLanguage = language
        _calculationTexts.value = calculationTextsFor(ScreenTextsPrompts.calculationTexts(language))
    }

    private fun calculationTextsFor(prompt: String): CalculationScreenTexts {
        val activeEngine = engineWrapper.engine ?: return CALCULATION_FALLBACK
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
                parseCalculationTexts(raw)
            }
        } catch (e: Exception) {
            CALCULATION_FALLBACK
        }
    }

    suspend fun generateSuccessTexts(language: String) = withContext(Dispatchers.IO) {
        if (language == lastSuccessTextsLanguage) return@withContext
        lastSuccessTextsLanguage = language
        _successTexts.value = resultTextsFor(ScreenTextsPrompts.successTexts(language))
    }

    suspend fun generateFailureTexts(language: String) = withContext(Dispatchers.IO) {
        if (language == lastFailureTextsLanguage) return@withContext
        lastFailureTextsLanguage = language
        _failureTexts.value = resultTextsFor(ScreenTextsPrompts.failureTexts(language))
    }

    private fun resultTextsFor(prompt: String): ResultScreenTexts {
        val activeEngine = engineWrapper.engine ?: return RESULT_FALLBACK
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

    private fun parse(raw: String): Answer = try {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        val json = JSONObject(raw.substring(start, end + 1))
        appLanguage = json.optString("appLanguage", appLanguage)
        n = json.optInt("n", n)
        when (AppDestination.fromId(json.optString("screen"))) {
            AppDestination.PARAMS -> ShowParamsSettingScreenAndRefreshTexts(
                n = n,
                appLanguage = appLanguage
            )

            AppDestination.WELCOME -> ShowWelcomeScreen
            AppDestination.CALCULATION -> ShowCalculationScreen(n = n, appLanguage = appLanguage)
            AppDestination.SUCCESS -> ShowSuccessScreen(appLanguage = appLanguage)
            AppDestination.FAILURE -> ShowFailureScreen(appLanguage = appLanguage)
            null -> _answer.value
        }
    } catch (e: Exception) {
        _answer.value
    }

    private fun parseParamsTexts(raw: String): ParamsSettingScreenTexts = try {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        val json = JSONObject(raw.substring(start, end + 1))
        ParamsSettingScreenTexts(
            languageHint = json.optString("languageHint", PARAMS_FALLBACK.languageHint),
            changeLanguageButton = json.optString(
                "changeLanguageButton",
                PARAMS_FALLBACK.changeLanguageButton
            ),
            nHint = json.optString("nHint", PARAMS_FALLBACK.nHint),
            saveNButton = json.optString("changeNButton", PARAMS_FALLBACK.saveNButton),
            title = json.optString("title", PARAMS_FALLBACK.title),
            loading = false,
        )
    } catch (e: Exception) {
        PARAMS_FALLBACK
    }

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

    private fun parseCalculationTexts(raw: String): CalculationScreenTexts = try {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        val json = JSONObject(raw.substring(start, end + 1))
        CalculationScreenTexts(
            title = json.optString("title", CALCULATION_FALLBACK.title),
            message = json.optString("message", CALCULATION_FALLBACK.message),
        )
    } catch (e: Exception) {
        CALCULATION_FALLBACK
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

        val LOADING_RESULT_TEXTS = ResultScreenTexts(
            title = LOADING_TEXT,
            message = LOADING_TEXT,
        )

        val RESULT_FALLBACK = ResultScreenTexts(
            title = FALLBACK_TEXT,
            message = FALLBACK_TEXT,
        )

        val LOADING_CALCULATION_TEXTS = CalculationScreenTexts(
            title = LOADING_TEXT,
            message = LOADING_TEXT,
        )

        val CALCULATION_FALLBACK = CalculationScreenTexts(
            title = FALLBACK_TEXT,
            message = FALLBACK_TEXT,
        )
    }
}
