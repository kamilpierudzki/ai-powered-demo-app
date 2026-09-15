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

class AgentViewModel(application: Application) : AndroidViewModel(application) {

    private val agent = Agent()
    val answer: StateFlow<Answer> = agent.answer
    val paramsTexts: StateFlow<ParamsSettingScreenTexts> = agent.paramsTexts
    val calculationTexts: StateFlow<CalculationScreenTexts> = agent.calculationTexts
    val successTexts: StateFlow<ResultScreenTexts> = agent.successTexts
    val failureTexts: StateFlow<ResultScreenTexts> = agent.failureTexts

    val welcomeUiState: StateFlow<WelcomeScreenUiState> = agent.engineState
        .map { it.toWelcomeUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, agent.engineState.value.toWelcomeUiState())

    init {
        viewModelScope.launch { agent.initializeEngine(getApplication()) }
    }

    override fun onCleared() {
        super.onCleared()
        agent.close()
    }

    // Serialization and drop-when-busy live in Agent; this is only the bridge to viewModelScope.
    fun onNewInputAction(action: Action) {
        viewModelScope.launch {
            if (!agent.onNewInputAction(action)) {
                android.util.Log.d("AgentViewModel", "Dropped: ${action::class.simpleName}")
            }
        }
    }

    // Text generation is not serialized with navigation (the Copywriter has one small lock per
    // screen and never takes the navigation Mutex), so it never blocks navigation decisions. It is
    // launched in viewModelScope rather than the screen's LaunchedEffect on purpose: the blocking
    // native call cannot be interrupted by effect cancellation anyway, and keeping requests in one
    // scope leaves last-wins ordering to the Copywriter instead of Compose effect lifetimes.
    fun refreshParamsTexts(language: String) {
        viewModelScope.launch { agent.generateParamsTexts(language) }
    }

    fun refreshCalculationTexts(language: String) {
        viewModelScope.launch { agent.generateCalculationTexts(language) }
    }

    fun refreshSuccessTexts(language: String) {
        viewModelScope.launch { agent.generateSuccessTexts(language) }
    }

    fun refreshFailureTexts(language: String) {
        viewModelScope.launch { agent.generateFailureTexts(language) }
    }

    private fun EngineState.toWelcomeUiState(): WelcomeScreenUiState = when (this) {
        is EngineState.Initializing -> WelcomeScreenUiState.Initializing(modelName)
        is EngineState.Error -> WelcomeScreenUiState.Error(modelName, message)
        is EngineState.Ready -> WelcomeScreenUiState.EngineReady(modelName)
    }
}
