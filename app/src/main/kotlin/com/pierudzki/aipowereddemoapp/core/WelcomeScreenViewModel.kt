package com.pierudzki.aipowereddemoapp.core

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pierudzki.aipowereddemoapp.ai.EngineState
import com.pierudzki.aipowereddemoapp.ai.EngineWrapper
import com.pierudzki.aipowereddemoapp.ai.prompts.WelcomeScreenButtonTextPrompt
import com.pierudzki.aipowereddemoapp.core.WelcomeScreenViewModel.UiState.Info
import com.pierudzki.aipowereddemoapp.core.WelcomeScreenViewModel.UiState.Initializing
import com.pierudzki.aipowereddemoapp.core.WelcomeScreenViewModel.UiState.ModelUnavailable
import com.pierudzki.aipowereddemoapp.core.WelcomeScreenViewModel.UiState.Ready
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WelcomeScreenViewModel(application: Application) : AndroidViewModel(application) {

    sealed interface UiState {

        val modelName: String
        val appLanguage: String

        data class ModelUnavailable(
            override val modelName: String,
            override val appLanguage: String,
        ) : UiState

        data class Initializing(
            override val modelName: String,
            override val appLanguage: String,
        ) : UiState

        data class Ready(
            val startButtonTextLoading: Boolean,
            val startButtonText: String,
            override val modelName: String,
            override val appLanguage: String,
        ) : UiState

        data class Info(
            val text: String,
            override val modelName: String,
            override val appLanguage: String,
        ) : UiState
    }

    private var startButtonText: String = ""
    private val welcomeScreenButtonTextPrompt = WelcomeScreenButtonTextPrompt()

    private val _uiState = MutableStateFlow<UiState>(
        Initializing(
            modelName = EngineWrapper.state.value.modelName,
            appLanguage = EngineWrapper.state.value.appLanguage,
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            EngineWrapper.state.collectLatest { state ->
                when (state) {
                    is EngineState.Initializing -> {
                        _uiState.value = Initializing(
                            modelName = state.modelName,
                            appLanguage = state.appLanguage,
                        )
                    }

                    is EngineState.Ready -> {
                        refreshButtonText()
                    }

                    is EngineState.Info -> {
                        _uiState.value = Info(
                            text = state.message,
                            modelName = state.modelName,
                            appLanguage = state.appLanguage,
                        )
                    }

                    is EngineState.ModelUnavailable -> {
                        _uiState.value = ModelUnavailable(
                            modelName = state.modelName,
                            appLanguage = state.appLanguage,
                        )
                    }
                }
            }
        }
    }

    fun onAppLanguageUpdated(language: String) {
        EngineWrapper.appLanguage = language
        refreshButtonText()
    }

    private fun refreshButtonText() {
        viewModelScope.launch {
            welcomeScreenButtonTextPrompt.execute().collect { status ->
                when (status) {
                    is Prompt.Status.Processing -> {
                        startButtonText = "Loading..."
                        _uiState.value = Ready(
                            startButtonTextLoading = true,
                            startButtonText = startButtonText,
                            modelName = EngineWrapper.state.value.modelName,
                            appLanguage = EngineWrapper.state.value.appLanguage,
                        )
                    }

                    is Prompt.Status.Ready<*> -> {
                        startButtonText = status.value as String
                        _uiState.value = Ready(
                            startButtonTextLoading = false,
                            startButtonText = status.value.toString(),
                            modelName = EngineWrapper.state.value.modelName,
                            appLanguage = EngineWrapper.state.value.appLanguage,
                        )
                    }
                }
            }
        }
    }
}