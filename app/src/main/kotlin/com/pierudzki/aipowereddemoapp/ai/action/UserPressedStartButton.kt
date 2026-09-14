package com.pierudzki.aipowereddemoapp.ai.action

class UserPressedStartButton : Action {
    override val prompt: String
        get() = "The user tapped the start button on the welcome screen and wants to configure parameters."
}