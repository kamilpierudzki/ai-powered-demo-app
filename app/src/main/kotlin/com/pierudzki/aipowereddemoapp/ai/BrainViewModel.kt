package com.pierudzki.aipowereddemoapp.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pierudzki.aipowereddemoapp.ai.action.Action
import com.pierudzki.aipowereddemoapp.ai.answer.Answer
import com.pierudzki.aipowereddemoapp.ai.answer.ShowParamsSettingScreen
import com.pierudzki.aipowereddemoapp.core.ParamsSettingsScreenTexts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BrainViewModel(application: Application) : AndroidViewModel(application) {

    private val brain = Brain()
    val answer: StateFlow<Answer> = brain.answer

    private var lastParamsSettingsScreenTexts = ParamsSettingsScreenTexts(
        languageHint = "Loading...",
        changeLanguageButton = "Loading...",
        nHint = "Loading...",
        saveNButton = "Loading...",
        loading = true,
    )
    private val paramsSettingsScreenTextsInference = ParamsSettingsScreenTextsInference()

    private val _paramsSettingsScreenTexts = MutableStateFlow(lastParamsSettingsScreenTexts)
    val paramsSettingsScreenTexts: StateFlow<ParamsSettingsScreenTexts> =
        _paramsSettingsScreenTexts.asStateFlow()

    private var lastGeneratedLanguage: String? = null

    init {
        observeAnswers()
    }

    fun onNewInputAction(action: Action) {
        viewModelScope.launch {
            brain.onNewInputAction(action)
        }
    }

    private fun observeAnswers() {
        viewModelScope.launch {
            brain.answer.collect { answer ->
                if (answer is ShowParamsSettingScreen && answer.appLanguage != lastGeneratedLanguage) {
                    lastGeneratedLanguage = answer.appLanguage
                    refreshTextsForParamsSettingScreen(answer.appLanguage)
                }
            }
        }
    }

    private fun refreshTextsForParamsSettingScreen(language: String) {
        viewModelScope.launch {
            paramsSettingsScreenTextsInference.run(language).collect { status ->
                when (status) {
                    ParamsSettingsScreenTextsInference.Status.Processing -> {
                        lastParamsSettingsScreenTexts =
                            lastParamsSettingsScreenTexts.copy(loading = true)
                        _paramsSettingsScreenTexts.value = lastParamsSettingsScreenTexts
                    }

                    is ParamsSettingsScreenTextsInference.Status.Ready -> {
                        lastParamsSettingsScreenTexts = lastParamsSettingsScreenTexts.copy(
                            loading = false,
                            languageHint = status.value.languageHint,
                            changeLanguageButton = status.value.changeLanguageButton,
                            nHint = status.value.nHint,
                            saveNButton = status.value.saveNButton,
                        )
                        _paramsSettingsScreenTexts.value = lastParamsSettingsScreenTexts
                    }
                }
            }
        }
    }
}
