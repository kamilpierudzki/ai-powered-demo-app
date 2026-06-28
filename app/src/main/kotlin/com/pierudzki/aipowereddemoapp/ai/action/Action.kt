package com.pierudzki.aipowereddemoapp.ai.action

interface Action {
    val prompt: String
    val isDroppableWhenBusy: Boolean get() = false
}