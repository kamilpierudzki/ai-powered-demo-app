package com.pierudzki.aipowereddemoapp.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * On-device navigator backed by LiteRT-LM (Engine/Conversation) running `gemma-4-E4B-it.litertlm`.
 * Given free-form user input it lets the model decide which screen to switch to via the
 * `navigateToScreen` tool, and reports the choice as an [AiNavigationDecision].
 *
 * Tool calling is manual (`automaticToolCalling = false`): this class only produces a decision;
 * performing the actual navigation is left to the caller (UI/navigation layer).
 */
@Deprecated("use EngineWrapper")
class OnDeviceAiNavigator(
    private val context: Context,
    private val modelPath: String = ModelConfig.MODEL_PATH,
) {

    /** Runs a single turn and maps the model output to a typed decision. */
    /*suspend fun decide(userInput: String): AiNavigationDecision = withContext(Dispatchers.IO) {
        val activeEngine = engine ?: error("Wywolaj initialize() przed decide()")
        activeEngine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(ModelConfig.SYSTEM_PROMPT),
                tools = listOf(tool(NavigationToolSet())),
                automaticToolCalling = false,
//                samplerConfig = SamplerConfig(...) // tutaj parametr
            ),
        ).use { conversation: Conversation ->
            val response = conversation.sendMessage(userInput)
            val toolCall = response.toolCalls.firstOrNull()
            val text = response.contents.contents
                .filterIsInstance<Content.Text>()
                .joinToString("") { it.text }
                .trim()
            when {
                toolCall != null -> {
                    val destinationId = toolCall.arguments["destination"]?.toString().orEmpty()
                    AppDestination.fromId(destinationId)
                        ?.let { AiNavigationDecision.Navigate(it) }
                        ?: AiNavigationDecision.Unknown(destinationId)
                }

                text.isNotEmpty() -> AiNavigationDecision.Message(text)
                else -> AiNavigationDecision.Unknown(response.toString())
            }
        }
    }*/
}
