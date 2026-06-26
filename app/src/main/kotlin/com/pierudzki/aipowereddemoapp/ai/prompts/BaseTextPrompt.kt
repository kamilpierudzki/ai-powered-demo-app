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
import kotlin.use

abstract class BaseTextPrompt: Prompt<String> {

    protected abstract val systemPrompt: String
    protected abstract val userPrompt: String
    protected abstract val creativeConfig: SamplerConfig

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
            emit(Prompt.Status.Ready(FALLBACK_TEXT))
        }
        .flowOn(Dispatchers.IO)

    private companion object {
        const val FALLBACK_TEXT = "error"
    }
}