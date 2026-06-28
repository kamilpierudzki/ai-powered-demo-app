package com.pierudzki.aipowereddemoapp.ai

import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class CalculationScreenTextsInference {

    sealed interface Status {
        data class Ready(val value: String) : Status
        data object Processing : Status
    }

    private fun systemPrompt(appLanguage: String): String = """
        You manage an Android mobile app. Suggest a short status text shown on the screen
        while the app is calculating the Fibonacci sequence. The text tells the user that a
        calculation is in progress and to please wait. It can be up to 10 words long.

        The user wants the text to be in the following language: "${appLanguage}."

        Respond with ONLY the status text itself, as plain text. Do not use JSON, markdown,
        quotes, labels, or any extra explanation.
    """.trimIndent()

    private val userPrompt: String
        get() = """
        Propose text.
    """.trimIndent()

    private val creativeConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0)
    private val fallback = "error"

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
            emit(Status.Ready(if (text.isEmpty()) fallback else text))
        }
    }
        .catch { throwable ->
            emit(Status.Ready(fallback))
        }
        .flowOn(Dispatchers.IO)
}
