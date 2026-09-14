package com.pierudzki.aipowereddemoapp.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pierudzki.aipowereddemoapp.ai.action.Action
import com.pierudzki.aipowereddemoapp.ai.answer.Answer
import com.pierudzki.aipowereddemoapp.core.CalculationScreenTexts
import com.pierudzki.aipowereddemoapp.core.ParamsSettingScreenTexts
import com.pierudzki.aipowereddemoapp.core.ResultScreenTexts
import com.pierudzki.aipowereddemoapp.core.WelcomeScreenUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BrainViewModel(application: Application) : AndroidViewModel(application) {

    private val brain = Brain()
    val answer: StateFlow<Answer> = brain.answer
    val paramsTexts: StateFlow<ParamsSettingScreenTexts> = brain.paramsTexts
    val calculationTexts: StateFlow<CalculationScreenTexts> = brain.calculationTexts
    val successTexts: StateFlow<ResultScreenTexts> = brain.successTexts
    val failureTexts: StateFlow<ResultScreenTexts> = brain.failureTexts

    val welcomeUiState: StateFlow<WelcomeScreenUiState> = brain.engineState
        .map { it.toWelcomeUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, brain.engineState.value.toWelcomeUiState())

    init {
        viewModelScope.launch { brain.initializeEngine(getApplication()) }
    }

    override fun onCleared() {
        super.onCleared()
        brain.close()
    }

    // Serialization and drop-when-busy live in Brain; this is only the bridge to viewModelScope.
    fun onNewInputAction(action: Action) {
        viewModelScope.launch {
            if (!brain.onNewInputAction(action)) {
                android.util.Log.d("BrainViewModel", "Dropped: ${action::class.simpleName}")
            }
        }
    }

    // Text generation is not serialized with navigation (see Brain), so it never blocks
    // navigation decisions.
    fun refreshParamsTexts(language: String) {
        viewModelScope.launch { brain.generateParamsTexts(language) }
    }

    fun refreshCalculationTexts(language: String) {
        viewModelScope.launch { brain.generateCalculationTexts(language) }
    }

    fun refreshSuccessTexts(language: String) {
        viewModelScope.launch { brain.generateSuccessTexts(language) }
    }

    fun refreshFailureTexts(language: String) {
        viewModelScope.launch { brain.generateFailureTexts(language) }
    }

    private fun EngineState.toWelcomeUiState(): WelcomeScreenUiState = when (this) {
        is EngineState.Initializing -> WelcomeScreenUiState.Initializing(modelName)
        is EngineState.Error -> WelcomeScreenUiState.Error(modelName, message)
        is EngineState.Ready -> WelcomeScreenUiState.EngineReady(modelName)
    }
}
