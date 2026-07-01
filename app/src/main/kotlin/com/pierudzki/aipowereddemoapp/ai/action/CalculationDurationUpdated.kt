package com.pierudzki.aipowereddemoapp.ai.action

const val CALCULATION_TIME_LIMIT_SECONDS = 10

class CalculationDurationUpdated(val durationSeconds: Int) : Action {
    override val prompt: String
        get() = "The Fibonacci calculation has been running for $durationSeconds seconds so far."

    override val isDroppableWhenBusy: Boolean
        get() = true
}
