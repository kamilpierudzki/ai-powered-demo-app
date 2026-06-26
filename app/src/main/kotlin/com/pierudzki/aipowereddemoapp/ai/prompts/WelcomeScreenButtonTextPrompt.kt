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

class WelcomeScreenButtonTextPrompt : Prompt<String> {

    private val systemPrompt: String =
        """
            You're managing a mobile app for Android. Suggest a button text on the app's home screen.
            Clicking the button takes the user to a configuration screen for the calculation parameters
            the app will perform in the next steps. The text must be a maximum of 5 words.
            
            User wants the text in the following language: "${EngineWrapper.appLanguage}".
            
            Your response should contain only the proposed text.
        """.trimIndent()

    private val userPrompt: String =
        """
            Propose a button text.
        """.trimIndent()

    private val creativeConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0)

    override suspend fun execute(): Flow<Prompt.Status> = flow {
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
            android.util.Log.e("WelcomeScreenButtonTextPrompt", "execute() failed", throwable)
            emit(Prompt.Status.Ready(FALLBACK_TEXT))
        }
        .flowOn(Dispatchers.IO)

    private companion object {
        const val FALLBACK_TEXT = "error"
    }
}