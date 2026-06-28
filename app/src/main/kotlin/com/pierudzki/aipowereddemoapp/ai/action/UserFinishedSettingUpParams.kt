package com.pierudzki.aipowereddemoapp.ai.action

class UserFinishedSettingUpParams(val n: Int) : Action {
    override val prompt: String
        get() = "The user confirmed the parameters with N=$n on the parameters screen."
}