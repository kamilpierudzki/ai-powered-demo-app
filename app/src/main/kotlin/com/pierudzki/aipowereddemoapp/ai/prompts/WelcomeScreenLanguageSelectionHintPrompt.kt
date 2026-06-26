package com.pierudzki.aipowereddemoapp.ai.prompts

import com.google.ai.edge.litertlm.SamplerConfig
import com.pierudzki.aipowereddemoapp.ai.EngineWrapper

class WelcomeScreenLanguageSelectionHintPrompt : BaseTextPrompt() {

    override val systemPrompt: String
        get() =
            """
            You manage an Android mobile app. Suggest a hint text for the text field where users
            enter the language they want the app to use.
            The hint text can contain a maximum of 10 words.
            
            User wants the text in the following language: "${EngineWrapper.appLanguage}".
            
            Your response should contain only the proposed text.
        """.trimIndent()

    override val userPrompt: String
        get() =
            """
            Suggest hint text.
        """.trimIndent()

    override val creativeConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0)
}