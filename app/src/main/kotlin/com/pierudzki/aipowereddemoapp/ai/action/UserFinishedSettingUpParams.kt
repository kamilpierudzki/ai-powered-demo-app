package com.pierudzki.aipowereddemoapp.ai.action

class UserFinishedSettingUpParams(
    val n: Int,
    val appLanguage: String,
) : Action {
    override val prompt: String
        get() = "The user confirmed the parameters on the parameters screen with N=$n and the app " +
            "language \"$appLanguage\". A brand-new Fibonacci calculation is starting from scratch with " +
            "its elapsed-time counter at 0 seconds. Use N=$n and this app language for the calculation, " +
            "success, and failure screens, and evaluate the time limit fresh for this run."

    override val startsFreshNavigationConversation: Boolean
        get() = true
}