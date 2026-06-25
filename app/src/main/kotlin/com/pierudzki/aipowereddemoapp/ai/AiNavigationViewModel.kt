package com.pierudzki.aipowereddemoapp.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** UI state for the AI-powered navigation screen. */
sealed interface AiUiState {
    data object ModelUnavailable : AiUiState
    data object Initializing : AiUiState
    data object Ready : AiUiState
    data object Deciding : AiUiState
    data class Info(val text: String) : AiUiState // model replied with text / unknown / error
}

/**
 * Hosts [OnDeviceAiNavigator] for the AI screen: loads the model lazily on creation, runs decisions,
 * and emits one-shot navigation events. Native resources are released in [onCleared].
 */
class AiNavigationViewModel(application: Application) : AndroidViewModel(application) {

    private val navigator = OnDeviceAiNavigator(application)

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Initializing)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _destinations = Channel<AppDestination>(Channel.BUFFERED)
    val destinations = _destinations.receiveAsFlow()

    init {
        if (!navigator.isModelAvailable()) {
            _uiState.value = AiUiState.ModelUnavailable
        } else {
            viewModelScope.launch {
                runCatching { navigator.initialize() }
                    .onSuccess { _uiState.value = AiUiState.Ready }
                    .onFailure { _uiState.value = AiUiState.Info("Blad inicjalizacji: ${it.message}") }
            }
        }
    }

    fun submit(userInput: String) {
        if (userInput.isBlank()) return
        viewModelScope.launch {
            _uiState.value = AiUiState.Deciding
            runCatching { navigator.decide(userInput) }
                .onSuccess { decision ->
                    when (decision) {
                        is AiNavigationDecision.Navigate -> {
                            _destinations.send(decision.destination)
                            _uiState.value = AiUiState.Ready
                        }

                        is AiNavigationDecision.Message ->
                            _uiState.value = AiUiState.Info(decision.text)

                        is AiNavigationDecision.Unknown ->
                            _uiState.value = AiUiState.Info("Nie rozpoznano ekranu: ${decision.raw}")
                    }
                }
                .onFailure { _uiState.value = AiUiState.Info("Blad: ${it.message}") }
        }
    }

    override fun onCleared() {
        navigator.close()
    }
}
