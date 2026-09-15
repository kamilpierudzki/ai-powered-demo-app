package com.pierudzki.aipowereddemoapp.ai.action

class UserChangedAppLanguage(val newLanguage: String) : Action {
    override val message: String
        get() = "The user changed the app language to \"$newLanguage\". Keep them on the parameters screen using this language."
}