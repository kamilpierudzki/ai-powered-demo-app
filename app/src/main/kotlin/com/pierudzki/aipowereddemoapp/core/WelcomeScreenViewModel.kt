package com.pierudzki.aipowereddemoapp.core

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pierudzki.aipowereddemoapp.ai.EngineState
import com.pierudzki.aipowereddemoapp.ai.EngineWrapper
import com.pierudzki.aipowereddemoapp.ai.prompts.WelcomeScreenButtonTextPrompt
import com.pierudzki.aipowereddemoapp.ai.prompts.WelcomeScreenLanguageSelectionHintPrompt
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
            val startButtonText: String,
            val languageSelectionHint: String,
            val loadingTexts: Boolean,
            override val modelName: String,
            override val appLanguage: String,
        ) : UiState

        data class Info(
            val text: String,
            override val modelName: String,
            override val appLanguage: String,
        ) : UiState
    }

    private var startButtonText: String = "Loading..."
    private var startButtonTextLoading: Boolean = false
    private var languageSelectionHint: String = "Loading..."
    private var languageSelectionHintLoading: Boolean = false
    private val buttonTextPrompt = WelcomeScreenButtonTextPrompt()
    private val languageSelectionHintPrompt = WelcomeScreenLanguageSelectionHintPrompt()

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
                        refreshTexts()
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
        refreshTexts()
    }

    private fun refreshTexts() {
        viewModelScope.launch {
            refreshButtonText()
            refreshLanguageSelectionHint()
        }
    }

    private suspend fun refreshButtonText() {
        buttonTextPrompt.execute().collect { status ->
            when (status) {
                is Prompt.Status.Processing -> {
                    startButtonTextLoading = true
                    _uiState.value = buildReadyState()
                }

                is Prompt.Status.Ready<*> -> {
                    startButtonTextLoading = false
                    startButtonText = status.value as String
                    _uiState.value = buildReadyState()
                }
            }
        }
    }

    private suspend fun refreshLanguageSelectionHint() {
        languageSelectionHintPrompt.execute().collect { status ->
            when (status) {
                is Prompt.Status.Processing -> {
                    languageSelectionHintLoading = true
                    _uiState.value = buildReadyState()
                }

                is Prompt.Status.Ready<*> -> {
                    languageSelectionHintLoading = false
                    languageSelectionHint = status.value as String
                    _uiState.value = buildReadyState()
                }
            }
        }
    }

    private fun buildReadyState() = Ready(
        startButtonText = startButtonText,
        languageSelectionHint = languageSelectionHint,
        loadingTexts = startButtonTextLoading || languageSelectionHintLoading,
        modelName = EngineWrapper.state.value.modelName,
        appLanguage = EngineWrapper.state.value.appLanguage,
    )
}