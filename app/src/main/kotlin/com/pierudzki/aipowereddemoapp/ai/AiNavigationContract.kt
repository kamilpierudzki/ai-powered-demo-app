package com.pierudzki.aipowereddemoapp.ai

/**
 * Screens the model is allowed to choose from when calling the `navigateToScreen` tool.
 *
 * [id] is the stable identifier exposed to the model, [description] helps it pick the right one.
 * The set mirrors the existing screens in `core/ui`.
 */
enum class AppDestination(val id: String, val description: String) {
    WELCOME("welcome", "Ekran powitalny z wyborem flow"),
    PARAMS("params", "Ekran wprowadzania parametrow (czas w sekundach)"),
    CALCULATION("calculation", "Ekran trwajacych obliczen"),
    SUCCESS("success", "Ekran sukcesu po zakonczeniu obliczen");

    companion object {
        fun fromId(id: String): AppDestination? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Result of a single model turn. The navigator only reports the decision; performing the actual
 * screen change is left to the caller (UI/navigation layer).
 */
sealed interface AiNavigationDecision {
    data class Navigate(val destination: AppDestination) : AiNavigationDecision
    data class Message(val text: String) : AiNavigationDecision
    data class Unknown(val raw: String) : AiNavigationDecision
}
