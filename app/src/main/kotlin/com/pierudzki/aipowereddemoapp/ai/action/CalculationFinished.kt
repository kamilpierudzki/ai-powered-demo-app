package com.pierudzki.aipowereddemoapp.ai.action

class CalculationFinished(val durationSeconds: Int) : Action {
    override val prompt: String
        get() = "The Fibonacci calculation finished after $durationSeconds seconds."
}
