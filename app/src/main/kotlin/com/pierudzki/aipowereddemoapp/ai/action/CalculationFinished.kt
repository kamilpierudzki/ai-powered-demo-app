package com.pierudzki.aipowereddemoapp.ai.action

class CalculationFinished(val durationSeconds: Int) : Action {
    override val message: String
        get() = "The Fibonacci calculation finished after $durationSeconds seconds."
}
