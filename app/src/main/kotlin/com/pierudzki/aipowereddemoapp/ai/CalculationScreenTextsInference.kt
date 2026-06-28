package com.pierudzki.aipowereddemoapp.ai

import com.google.ai.edge.litertlm.SamplerConfig
import com.pierudzki.aipowereddemoapp.core.CalculationScreenTexts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CalculationScreenTextsInference {

    sealed interface Status {
        data class Ready(val value: String) : Status
        data object Processing : Status
    }

    private fun systemPrompt(appLanguage: String): String = """
        // todo
    """.trimIndent()

    private val userPrompt: String
        get() = """
        Propose text.
    """.trimIndent()

    private val creativeConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0)
    private val fallback = CalculationScreenTexts(
        screenHind = "error",
        loading = false,
    )

    fun run(appLanguage: String): Flow<ParamsSettingScreenTextsInference.Status> = flow {
        // todo
    }
}