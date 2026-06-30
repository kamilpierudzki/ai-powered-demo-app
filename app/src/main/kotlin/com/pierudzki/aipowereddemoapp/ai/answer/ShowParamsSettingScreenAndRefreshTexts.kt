package com.pierudzki.aipowereddemoapp.ai.answer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pierudzki.aipowereddemoapp.ai.BrainViewModel
import com.pierudzki.aipowereddemoapp.ai.action.UserChangedAppLanguage
import com.pierudzki.aipowereddemoapp.ai.action.UserFinishedSettingUpParams
import com.pierudzki.aipowereddemoapp.ai.action.UserPressedBackButton
import com.pierudzki.aipowereddemoapp.core.AppDestination
import com.pierudzki.aipowereddemoapp.core.ParamsSettingScreen

data class ShowParamsSettingScreenAndRefreshTexts(
    val n: Int,
    val appLanguage: String,
) : Answer {
    override val destination: AppDestination get() = AppDestination.PARAMS

    @Composable
    override fun Content(brainViewModel: BrainViewModel) {
        val screenTexts by brainViewModel.paramsTexts.collectAsStateWithLifecycle()

        LaunchedEffect(appLanguage) {
            brainViewModel.refreshParamsTexts(appLanguage)
        }

        ParamsSettingScreen(
            texts = screenTexts,
            appLanguage = appLanguage,
            n = n,
            onAppLanguageChanged = {
                brainViewModel.onNewInputAction(UserChangedAppLanguage(it))
            },
            onNextStepClicked = {
                brainViewModel.onNewInputAction(UserFinishedSettingUpParams(it))
            },
            onBackClicked = {
                brainViewModel.onNewInputAction(UserPressedBackButton())
            }
        )
    }
}
