package com.pierudzki.aipowereddemoapp.core

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pierudzki.aipowereddemoapp.ai.EngineState
import com.pierudzki.aipowereddemoapp.ai.EngineWrapper
import com.pierudzki.aipowereddemoapp.ai.prompts.WelcomeScreenButtonTextPrompt
import com.pierudzki.aipowereddemoapp.ai.prompts.WelcomeScreenLanguageSelectionHintPrompt
import com.pierudzki.aipowereddemoapp.ai.prompts.WelcomeScreenSubmitButtonTextPrompt
import com.pierudzki.aipowereddemoapp.core.WelcomeScreenViewModel.UiState.EngineReady
import com.pierudzki.aipowereddemoapp.core.WelcomeScreenViewModel.UiState.Error
import com.pierudzki.aipowereddemoapp.core.WelcomeScreenViewModel.UiState.Initializing
import com.pierudzki.aipowereddemoapp.core.WelcomeScreenViewModel.UiState.ModelUnavailable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WelcomeScreenViewModel(application: Application) : AndroidViewModel(application) {

    sealed interface UiState {

        val modelName: String
        val appLanguage: String
        val startButtonText: String
        val languageSelectionHintText: String
        val submitButtonText: String
        val loadingTexts: Boolean


        data class ModelUnavailable(
            override val modelName: String,
            override val appLanguage: String,
            override val startButtonText: String,
            override val languageSelectionHintText: String,
            override val submitButtonText: String,
            override val loadingTexts: Boolean,
        ) : UiState

        data class Initializing(
            override val modelName: String,
            override val appLanguage: String,
            override val startButtonText: String,
            override val languageSelectionHintText: String,
            override val submitButtonText: String,
            override val loadingTexts: Boolean,
        ) : UiState

        data class EngineReady(
            override val modelName: String,
            override val appLanguage: String,
            override val startButtonText: String,
            override val languageSelectionHintText: String,
            override val submitButtonText: String,
            override val loadingTexts: Boolean,
        ) : UiState

        data class Error(
            val text: String,
            override val modelName: String,
            override val appLanguage: String,
            override val startButtonText: String,
            override val languageSelectionHintText: String,
            override val submitButtonText: String,
            override val loadingTexts: Boolean,
        ) : UiState
    }

    private var lastStartButtonText: String = "Loading..."
    private var startButtonTextLoading: Boolean = false
    private var lastLanguageSelectionHintText: String = "Loading..."
    private var languageSelectionHintLoading: Boolean = false
    private var lastSubmitButtonText: String = "Loading..."
    private var submitButtonTextLoading: Boolean = false
    private val buttonTextPrompt = WelcomeScreenButtonTextPrompt()
    private val languageSelectionHintTextPrompt = WelcomeScreenLanguageSelectionHintPrompt()
    private val submitButtonTextPrompt = WelcomeScreenSubmitButtonTextPrompt()

    private val _uiState = MutableStateFlow<UiState>(
        Initializing(
            startButtonText = lastStartButtonText,
            languageSelectionHintText = lastLanguageSelectionHintText,
            submitButtonText = lastSubmitButtonText,
            loadingTexts = startButtonTextLoading || languageSelectionHintLoading || submitButtonTextLoading,
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
                            startButtonText = lastStartButtonText,
                            languageSelectionHintText = lastLanguageSelectionHintText,
                            submitButtonText = lastSubmitButtonText,
                            loadingTexts = startButtonTextLoading || languageSelectionHintLoading || submitButtonTextLoading,
                            modelName = state.modelName,
                            appLanguage = state.appLanguage,
                        )
                    }

                    is EngineState.Ready -> {
                        refreshTexts()
                    }

                    is EngineState.Error -> {
                        _uiState.value = Error(
                            startButtonText = lastStartButtonText,
                            languageSelectionHintText = lastLanguageSelectionHintText,
                            submitButtonText = lastSubmitButtonText,
                            loadingTexts = startButtonTextLoading || languageSelectionHintLoading || submitButtonTextLoading,
                            text = state.message,
                            modelName = state.modelName,
                            appLanguage = state.appLanguage,
                        )
                    }

                    is EngineState.ModelUnavailable -> {
                        _uiState.value = ModelUnavailable(
                            startButtonText = lastStartButtonText,
                            languageSelectionHintText = lastLanguageSelectionHintText,
                            submitButtonText = lastSubmitButtonText,
                            loadingTexts = startButtonTextLoading || languageSelectionHintLoading || submitButtonTextLoading,
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
            refreshLanguageSelectionHintText()
            refreshSubmitButtonText()
        }
    }

    private suspend fun refreshButtonText() {
        buttonTextPrompt.execute().collect { status ->
            when (status) {
                is Prompt.Status.Processing -> {
                    startButtonTextLoading = true
                    _uiState.value = buildEngineReadyState()
                }

                is Prompt.Status.Ready<*> -> {
                    startButtonTextLoading = false
                    lastStartButtonText = status.value as String
                    _uiState.value = buildEngineReadyState()
                }
            }
        }
    }

    private suspend fun refreshLanguageSelectionHintText() {
        languageSelectionHintTextPrompt.execute().collect { status ->
            when (status) {
                is Prompt.Status.Processing -> {
                    languageSelectionHintLoading = true
                    _uiState.value = buildEngineReadyState()
                }

                is Prompt.Status.Ready<*> -> {
                    languageSelectionHintLoading = false
                    lastLanguageSelectionHintText = status.value as String
                    _uiState.value = buildEngineReadyState()
                }
            }
        }
    }

    private suspend fun refreshSubmitButtonText() {
        submitButtonTextPrompt.execute().collect { status ->
            when (status) {
                is Prompt.Status.Processing -> {
                    submitButtonTextLoading = true
                    _uiState.value = buildEngineReadyState()
                }

                is Prompt.Status.Ready<*> -> {
                    submitButtonTextLoading = false
                    lastSubmitButtonText = status.value as String
                    _uiState.value = buildEngineReadyState()
                }
            }
        }
    }

    private fun buildEngineReadyState() = EngineReady(
        startButtonText = lastStartButtonText,
        languageSelectionHintText = lastLanguageSelectionHintText,
        submitButtonText = lastSubmitButtonText,
        loadingTexts = startButtonTextLoading || languageSelectionHintLoading || submitButtonTextLoading,
        modelName = EngineWrapper.state.value.modelName,
        appLanguage = EngineWrapper.state.value.appLanguage,
    )
}