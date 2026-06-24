package com.pierudzki.aipowereddemoapp.ai

import com.google.ai.edge.localagents.core.proto.FunctionDeclaration
import com.google.ai.edge.localagents.core.proto.Schema
import com.google.ai.edge.localagents.core.proto.Tool
import com.google.ai.edge.localagents.core.proto.Type

/**
 * Builds the single `navigateToScreen` tool exposed to the model. The model calls it with a
 * `destination` argument chosen from [AppDestination] ids; the navigator maps that back to a
 * typed [AiNavigationDecision].
 */
internal fun buildNavigationTool(): Tool {
    val allowedDestinations = AppDestination.entries.joinToString(", ") { "${it.id} (${it.description})" }

    val navigateToScreen = FunctionDeclaration.newBuilder()
        .setName("navigateToScreen")
        .setDescription("Przelacza aplikacje na wskazany ekran.")
        .setParameters(
            Schema.newBuilder()
                .setType(Type.OBJECT)
                .putProperties(
                    "destination",
                    Schema.newBuilder()
                        .setType(Type.STRING)
                        .setDescription("Identyfikator ekranu docelowego. Dozwolone wartosci: $allowedDestinations")
                        .build(),
                )
                .build(),
        )
        .build()

    return Tool.newBuilder()
        .addFunctionDeclarations(navigateToScreen)
        .build()
}
