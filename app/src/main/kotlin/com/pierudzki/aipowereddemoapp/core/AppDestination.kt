package com.pierudzki.aipowereddemoapp.core

enum class AppDestination(val id: String, val description: String) {
    WELCOME("welcome", "Application welcome screen"),
    PARAMS("params", "The screen where the user sets an app language and the N value for the Fibonacci sequence."),
    CALCULATION("calculation", "The screen that runs the Fibonacci calculation for N and shows the produced values."),
    SUCCESS("success", "The screen shown when the Fibonacci calculation finished within the allowed time limit."),
    FAILURE("failure", "The screen shown when the Fibonacci calculation exceeded the allowed time limit and was interrupted.");

    companion object {
        fun fromId(id: String): AppDestination? = entries.firstOrNull { it.id == id }
    }
}