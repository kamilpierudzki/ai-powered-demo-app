package com.pierudzki.aipowereddemoapp.ai.action

class UserPressedBackButton : Action {
    override val prompt: String
        get() = "The user pressed the system back button and wants to go to the previous screen."
}