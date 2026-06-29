package com.pierudzki.aipowereddemoapp.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pierudzki.aipowereddemoapp.ai.action.Action
import com.pierudzki.aipowereddemoapp.ai.answer.Answer
import com.pierudzki.aipowereddemoapp.core.ParamsSettingScreenTexts
import com.pierudzki.aipowereddemoapp.core.ResultScreenTexts
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BrainViewModel(application: Application) : AndroidViewModel(application) {

    private val brain = Brain()
    val answer: StateFlow<Answer> = brain.answer
    val paramsTexts: StateFlow<ParamsSettingScreenTexts> = brain.paramsTexts
    val calculationHint: StateFlow<String> = brain.calculationHint
    val successTexts: StateFlow<ResultScreenTexts> = brain.successTexts
    val failureTexts: StateFlow<ResultScreenTexts> = brain.failureTexts

    private val brainMutex = Mutex()

    fun onNewInputAction(action: Action) {
        viewModelScope.launch {
            if (action.isDroppableWhenBusy) {
                if (!brainMutex.tryLock()) return@launch
                try {
                    brain.onNewInputAction(action)
                } finally {
                    brainMutex.unlock()
                }
            } else {
                brainMutex.withLock {
                    brain.onNewInputAction(action)
                }
            }
        }
    }

    // Text generation runs outside brainMutex so it does not block navigation decisions.
    fun refreshParamsTexts(language: String) {
        viewModelScope.launch { brain.generateParamsTexts(language) }
    }

    fun refreshCalculationHint(language: String) {
        viewModelScope.launch { brain.generateCalculationHint(language) }
    }

    fun refreshSuccessTexts(language: String) {
        viewModelScope.launch { brain.generateSuccessTexts(language) }
    }

    fun refreshFailureTexts(language: String) {
        viewModelScope.launch { brain.generateFailureTexts(language) }
    }
}
