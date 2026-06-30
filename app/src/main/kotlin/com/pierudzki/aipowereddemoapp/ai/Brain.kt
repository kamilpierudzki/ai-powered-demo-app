package com.pierudzki.aipowereddemoapp.ai

import android.content.Context
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.google.ai.edge.litertlm.tool
import com.pierudzki.aipowereddemoapp.ai.action.Action
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class Brain {

    private var appLanguage: String = DEFAULT_APP_LANGUAGE
    private var n: Int = DEFAULT_N

    private val _answer = MutableStateFlow<Answer>(ShowWelcomeScreen)
    val answer: StateFlow<Answer> = _answer.asStateFlow()

    private val navigationConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.2)

    private val navigationToolProvider = tool(NavigationTools())

    private val engineWrapper = EngineWrapper()
    private val screenTexts = ScreenTextsGenerator(engineWrapper)

    val engineState: StateFlow<EngineState> = engineWrapper.state

    val paramsTexts: StateFlow<ParamsSettingScreenTexts> get() = screenTexts.paramsTexts
    val calculationTexts: StateFlow<CalculationScreenTexts> get() = screenTexts.calculationTexts
    val successTexts: StateFlow<ResultScreenTexts> get() = screenTexts.successTexts
    val failureTexts: StateFlow<ResultScreenTexts> get() = screenTexts.failureTexts

    suspend fun initializeEngine(context: Context) = engineWrapper.initialize(context)

    fun closeEngine() = engineWrapper.close()

    suspend fun onNewInputAction(action: Action) = withContext(Dispatchers.IO) {
        val activeEngine = engineWrapper.engine ?: return@withContext
        try {
            activeEngine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(
                        NavigationPrompt.build(
                            currentScreenId = _answer.value.destination.id,
                            appLanguage = appLanguage,
                            n = n,
                            calculationTimeLimitSeconds = CALCULATION_TIME_LIMIT_SECONDS,
                        )
                    ),
                    tools = listOf(navigationToolProvider),
                    automaticToolCalling = true,
                    samplerConfig = navigationConfig,
                ),
            ).use { conversation ->
                conversation.sendMessage(action.prompt)
            }
        } catch (e: Exception) {
            android.util.Log.d("Brain", "onNewInputAction error: ${e.message}")
        }
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
                android.util.Log.d("Brain", "$it")
            }
            return "Showing the welcome screen."
        }

        @Tool(description = "Show the parameters screen where the user sets the app language and the N value for the Fibonacci sequence.")
        fun showParamsScreen(
            @ToolParam(description = "The current or updated N value for the Fibonacci sequence.") n: Int,
            @ToolParam(description = "The current or updated app language, for example English or Polish.") appLanguage: String,
        ): String {
            _answer.value = ShowParamsSettingScreen(n = n, appLanguage = appLanguage).also {
                android.util.Log.d("Brain", "$it")
            }
            return "Showing the parameters screen."
        }

        @Tool(description = "Show the calculation screen that runs the Fibonacci calculation for N and shows the produced values.")
        fun showCalculationScreen(
            @ToolParam(description = "The N value for the Fibonacci sequence to compute.") n: Int,
            @ToolParam(description = "The current app language, for example English or Polish.") appLanguage: String,
        ): String {
            _answer.value = ShowCalculationScreen(n = n, appLanguage = appLanguage).also {
                android.util.Log.d("Brain", "$it")
            }
            return "Showing the calculation screen."
        }

        @Tool(description = "Show the success screen, used when the Fibonacci calculation finished within the allowed time limit.")
        fun showSuccessScreen(
            @ToolParam(description = "The current app language, for example English or Polish.") appLanguage: String,
        ): String {
            _answer.value = ShowSuccessScreen(appLanguage = appLanguage).also {
                android.util.Log.d("Brain", "$it")
            }
            return "Showing the success screen."
        }

        @Tool(description = "Show the failure screen, used when the Fibonacci calculation ran longer than the allowed time limit and was interrupted.")
        fun showFailureScreen(
            @ToolParam(description = "The current app language, for example English or Polish.") appLanguage: String,
        ): String {
            _answer.value = ShowFailureScreen(appLanguage = appLanguage).also {
                android.util.Log.d("Brain", "$it")
            }
            return "Showing the failure screen."
        }
    }

    private companion object {
        const val DEFAULT_APP_LANGUAGE = "English"
        const val DEFAULT_N = 10
        const val CALCULATION_TIME_LIMIT_SECONDS = 10
    }
}
