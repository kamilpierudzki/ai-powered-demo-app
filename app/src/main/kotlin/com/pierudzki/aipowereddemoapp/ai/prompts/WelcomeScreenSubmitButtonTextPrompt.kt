package com.pierudzki.aipowereddemoapp.ai.prompts

import com.google.ai.edge.litertlm.SamplerConfig
import com.pierudzki.aipowereddemoapp.ai.EngineWrapper

class WelcomeScreenSubmitButtonTextPrompt: BaseTextPrompt() {

    override val systemPrompt: String
        get() =
            """
            You manage an Android mobile app. Suggest a button text on the app's welcome screen
            that will initiate the generation of all on-screen text in the user's selected language.
            The button text can contain a maximum of 4 words.
            
            User wants the button text in the following language: "${EngineWrapper.appLanguage}".
            
            Your response should contain only the proposed text.
        """.trimIndent()

    override val userPrompt: String
        get() =
            """
            Suggest a button text.
        """.trimIndent()

    override val creativeConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0)
}