package com.pierudzki.aipowereddemoapp.ai.action

class CalculationDurationUpdated(val durationSeconds: Int) : Action {
    override val prompt: String
        get() = "The Fibonacci calculation has been running for $durationSeconds seconds so far."

    override val isDroppableWhenBusy: Boolean
        get() = true
}
