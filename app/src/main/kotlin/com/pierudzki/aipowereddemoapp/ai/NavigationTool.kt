package com.pierudzki.aipowereddemoapp.ai

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet

/**
 * Single tool exposed to the model. LiteRT-LM turns the [Tool]/[ToolParam] annotations and the
 * function signature into an OpenAPI-style schema so the model knows when to call `navigateToScreen`.
 *
 * Note: with `automaticToolCalling = false` (see [OnDeviceAiNavigator]) the framework does NOT
 * invoke this body - we read the tool call manually and map it to an [AiNavigationDecision]. The
 * body is kept trivial and only matters if automatic tool calling is enabled later.
 */
class NavigationToolSet : ToolSet {

    @Tool(description = "Przelacza aplikacje na wskazany ekran.")
    fun navigateToScreen(
        @ToolParam(description = "Identyfikator ekranu docelowego. Dozwolone wartosci: welcome, params, calculation, success")
        destination: String,
    ): Map<String, Any> = mapOf("destination" to destination)
}
