package com.pierudzki.aipowereddemoapp.ai.action

class UserIsReadyToStartUsingBrain : Action {
    override val prompt: String
        get() = "The user tapped the start button on the welcome screen and wants to begin configuring the app."
}