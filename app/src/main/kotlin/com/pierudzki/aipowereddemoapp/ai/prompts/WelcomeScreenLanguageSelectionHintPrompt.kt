package com.pierudzki.aipowereddemoapp.ai.prompts

import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.pierudzki.aipowereddemoapp.ai.EngineWrapper
import com.pierudzki.aipowereddemoapp.core.Prompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class WelcomeScreenLanguageSelectionHintPrompt : Prompt<String> {

    private val systemPrompt: String
        get() =
            """
            You manage an Android mobile app. Suggest a hint text for the text field where users
            enter the language they want the app to use.
            The hint text can contain a maximum of 10 words.
            
            User wants the text in the following language: "${EngineWrapper.appLanguage}".
            
            Your response should contain only the proposed text.
        """.trimIndent()

    private val userPrompt: String
        get() =
            """
            Suggest hint text.
        """.trimIndent()

    private val creativeConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0)

    override suspend fun execute(): Flow<Prompt.Status> = flow {
        android.util.Log.d("test123", "WelcomeScreenLanguageSelectionHintPrompt.execute() called")
        emit(Prompt.Status.Processing)

        val activeEngine = EngineWrapper.engine ?: run {
            emit(Prompt.Status.Ready(FALLBACK_TEXT))
            return@flow
        }

        activeEngine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(systemPrompt),
                automaticToolCalling = false,
                samplerConfig = creativeConfig,
            ),
        ).use { conversation ->
            val response = conversation.sendMessage(userPrompt)
            val text = response.contents.contents
                .filterIsInstance<Content.Text>()
                .joinToString("") { it.text }
                .trim()
            emit(Prompt.Status.Ready(text.ifBlank { FALLBACK_TEXT }))
        }
    }
        .catch { throwable ->
            emit(Prompt.Status.Ready(FALLBACK_TEXT))
        }
        .flowOn(Dispatchers.IO)

    private companion object {
        const val FALLBACK_TEXT = "error"
    }
}