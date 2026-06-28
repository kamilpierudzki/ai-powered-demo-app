package com.pierudzki.aipowereddemoapp.ai

import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.pierudzki.aipowereddemoapp.ai.action.Action
import com.pierudzki.aipowereddemoapp.ai.answer.Answer
import com.pierudzki.aipowereddemoapp.ai.answer.ShowCalculationScreen
import com.pierudzki.aipowereddemoapp.ai.answer.ShowParamsSettingScreenAndRefreshTexts
import com.pierudzki.aipowereddemoapp.ai.answer.ShowWelcomeScreen
import com.pierudzki.aipowereddemoapp.core.AppDestination
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

    private val samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.2)

    suspend fun onNewInputAction(action: Action) = withContext(Dispatchers.IO) {
        val activeEngine = EngineWrapper.engine ?: return@withContext
        try {
            activeEngine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(systemPrompt()),
                    automaticToolCalling = false,
                    samplerConfig = samplerConfig,
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

    private fun currentScreenId(): String = when (_answer.value) {
        is ShowWelcomeScreen -> AppDestination.WELCOME.id
        is ShowParamsSettingScreenAndRefreshTexts -> AppDestination.PARAMS.id
        is ShowCalculationScreen -> AppDestination.CALCULATION.id
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

        Current state:
        - currentScreen: "${currentScreenId()}"
        - appLanguage: "$appLanguage"
        - n: $n

        Rules:
        - When the user is ready to start from the welcome screen, go to "params".
        - When the user changes the app language, stay on "params" and update appLanguage.
        - When the user finishes setting the parameters, go to "calculation".
        - When the user presses the back button while on "calculation", go to "params".
        - When the user presses the back button while on "params", go to "welcome".

        Respond with ONLY a single minified JSON object, without markdown code fences and without
        any extra text or explanation. Use exactly these keys:
        - "screen": one of "welcome", "params", "calculation".
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
            AppDestination.CALCULATION -> ShowCalculationScreen(n = n)
            null -> _answer.value
        }
    } catch (e: Exception) {
        _answer.value
    }

    private companion object {
        const val DEFAULT_APP_LANGUAGE = "English"
        const val DEFAULT_N = 10
    }
}
