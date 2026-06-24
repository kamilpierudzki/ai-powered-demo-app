package com.pierudzki.aipowereddemoapp.ai

import android.content.Context
import com.google.ai.edge.localagents.core.proto.Content
import com.google.ai.edge.localagents.core.proto.Part
import com.google.ai.edge.localagents.fc.GemmaFormatter
import com.google.ai.edge.localagents.fc.GenerativeModel
import com.google.ai.edge.localagents.fc.LlmInferenceBackend
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Initial on-device configuration of a generative model with function calling (AI Edge FC SDK +
 * MediaPipe LLM Inference). Given free-form user input it lets the model decide which screen to
 * switch to via the `navigateToScreen` tool, and reports the choice as an [AiNavigationDecision].
 *
 * This class only produces a decision; performing the actual navigation is left to the caller.
 */
class OnDeviceAiNavigator(
    private val context: Context,
    private val modelPath: String = ModelConfig.MODEL_PATH,
) {

    private var llmInference: LlmInference? = null
    private var generativeModel: GenerativeModel? = null

    /** Whether the model file has been provisioned on the device (see ModelConfig for the path). */
    fun isModelAvailable(): Boolean = File(modelPath).exists()

    /** Loads the model and wires up the system prompt and the navigation tool. */
    fun initialize() {
        val options = LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .build()
        val inference = LlmInference.createFromOptions(context, options)
        val backend = LlmInferenceBackend(inference, GemmaFormatter())

        val systemInstruction = Content.newBuilder()
            .setRole("system")
            .addParts(Part.newBuilder().setText(ModelConfig.SYSTEM_PROMPT))
            .build()

        llmInference = inference
        generativeModel = GenerativeModel(
            backend,
            systemInstruction,
            listOf(buildNavigationTool()),
        )
    }

    /** Runs a single turn and maps the model output to a typed decision. */
    suspend fun decide(userInput: String): AiNavigationDecision = withContext(Dispatchers.IO) {
        val model = generativeModel ?: error("Wywolaj initialize() przed decide()")
        val chat = model.startChat()
        // Tylko Gemma: tutaj mozna wlaczyc constrained decoding, aby wymusic wylacznie wywolania narzedzi.
        val response = chat.sendMessage(userInput)

        if (response.candidatesCount == 0 || response.getCandidates(0).content.partsList.isEmpty()) {
            return@withContext AiNavigationDecision.Unknown(response.toString())
        }

        val message = response.getCandidates(0).content.getParts(0)
        when {
            message.hasFunctionCall() -> {
                val functionCall = message.functionCall
                val destinationId = functionCall.args.fieldsMap["destination"]?.stringValue.orEmpty()
                AppDestination.fromId(destinationId)
                    ?.let { AiNavigationDecision.Navigate(it) }
                    ?: AiNavigationDecision.Unknown(destinationId)
            }

            message.hasText() -> AiNavigationDecision.Message(message.text)
            else -> AiNavigationDecision.Unknown(response.toString())
        }
    }

    /** Releases native resources held by the underlying inference engine. */
    fun close() {
        generativeModel = null
        llmInference?.close()
        llmInference = null
    }
}
