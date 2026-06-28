package com.pierudzki.aipowereddemoapp.core

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pierudzki.aipowereddemoapp.ai.ParamsSettingScreenTextsInference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ParamsSettingScreenViewModel(application: Application) : AndroidViewModel(application) {

    private var lastGeneratedLanguage: String? = null
    private var lastParamsSettingScreenTexts = ParamsSettingScreenTexts(
        languageHint = "Loading...",
        changeLanguageButton = "Loading...",
        nHint = "Loading...",
        saveNButton = "Loading...",
        loading = true,
    )
    private val paramsSettingScreenTextsInference = ParamsSettingScreenTextsInference()

    private val _paramsSettingScreenTexts = MutableStateFlow(lastParamsSettingScreenTexts)
    val paramsSettingScreenTexts: StateFlow<ParamsSettingScreenTexts> =
        _paramsSettingScreenTexts.asStateFlow()

    fun refreshTextsOnScreenScreen(language: String) {
        if (language == lastGeneratedLanguage) return
        lastGeneratedLanguage = language
        viewModelScope.launch {
            paramsSettingScreenTextsInference.run(language).collect { status ->
                when (status) {
                    ParamsSettingScreenTextsInference.Status.Processing -> {
                        lastParamsSettingScreenTexts =
                            lastParamsSettingScreenTexts.copy(loading = true)
                        _paramsSettingScreenTexts.value = lastParamsSettingScreenTexts
                    }

                    is ParamsSettingScreenTextsInference.Status.Ready -> {
                        lastParamsSettingScreenTexts = lastParamsSettingScreenTexts.copy(
                            loading = false,
                            languageHint = status.value.languageHint,
                            changeLanguageButton = status.value.changeLanguageButton,
                            nHint = status.value.nHint,
                            saveNButton = status.value.saveNButton,
                        )
                        _paramsSettingScreenTexts.value = lastParamsSettingScreenTexts
                    }
                }
            }
        }
    }
}