package com.pierudzki.aipowereddemoapp.ai

import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.pierudzki.aipowereddemoapp.core.ParamsSettingsScreenTexts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject

class ParamsSettingsScreenTextsInference {

    sealed interface Status {
        data class Ready(val value: ParamsSettingsScreenTexts) : Status
        data object Processing : Status
    }

    private fun systemPrompt(appLanguage: String): String =
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

    private val userPrompt: String
        get() = """
        Propose texts.
    """.trimIndent()

    private val creativeConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0)

    private val fallback = ParamsSettingsScreenTexts(
        languageHint = "error",
        changeLanguageButton = "error",
        nHint = "error",
        saveNButton = "error",
        loading = false,
    )

    fun run(appLanguage: String): Flow<Status> = flow {
        emit(Status.Processing)

        val activeEngine = EngineWrapper.engine ?: run {
            emit(Status.Ready(fallback))
            return@flow
        }

        activeEngine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(systemPrompt(appLanguage)),
                automaticToolCalling = false,
                samplerConfig = creativeConfig,
            ),
        ).use { conversation ->
            val response = conversation.sendMessage(userPrompt)
            val text = response.contents.contents
                .filterIsInstance<Content.Text>()
                .joinToString("") { it.text }
                .trim()
            emit(Status.Ready(parse(text)))
        }
    }
        .catch { throwable ->
            emit(Status.Ready(fallback))
        }
        .flowOn(Dispatchers.IO)

    private fun parse(raw: String): ParamsSettingsScreenTexts = try {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        val json = JSONObject(raw.substring(start, end + 1))
        ParamsSettingsScreenTexts(
            languageHint = json.optString("languageHint", fallback.languageHint),
            changeLanguageButton = json.optString("changeLanguageButton", fallback.changeLanguageButton),
            nHint = json.optString("nHint", fallback.nHint),
            saveNButton = json.optString("changeNButton", fallback.saveNButton),
        )
    } catch (e: Exception) {
        fallback
    }
}