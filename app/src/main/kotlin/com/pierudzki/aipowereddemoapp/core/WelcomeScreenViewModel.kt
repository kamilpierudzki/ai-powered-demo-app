package com.pierudzki.aipowereddemoapp.core

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pierudzki.aipowereddemoapp.ai.EngineState
import com.pierudzki.aipowereddemoapp.ai.EngineWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WelcomeScreenViewModel(application: Application) : AndroidViewModel(application) {

    sealed interface UiState {
        val modelName: String

        data class Initializing(
            override val modelName: String,
        ) : UiState

        data class EngineReady(
            override val modelName: String,
        ) : UiState

        data class Error(
            override val modelName: String,
            val text: String,
        ) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(
        UiState.Initializing(modelName = EngineWrapper.state.value.modelName)
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        observeEngine()
    }

    private fun observeEngine() {
        viewModelScope.launch {
            EngineWrapper.state.collect { engineState ->
                _uiState.value = when (engineState) {
                    is EngineState.Initializing -> {
                        UiState.Initializing(
                            modelName = EngineWrapper.state.value.modelName,
                        )
                    }

                    is EngineState.Error -> {
                        UiState.Error(
                            modelName = EngineWrapper.state.value.modelName,
                            text = engineState.message,
                        )
                    }

                    is EngineState.Ready -> {
                        UiState.EngineReady(
                            modelName = EngineWrapper.state.value.modelName,
                        )
                    }
                }
            }
        }
    }
}