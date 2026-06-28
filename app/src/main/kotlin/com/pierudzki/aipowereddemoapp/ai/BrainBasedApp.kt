package com.pierudzki.aipowereddemoapp.ai

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pierudzki.aipowereddemoapp.ai.action.UserChangedAppLanguage
import com.pierudzki.aipowereddemoapp.ai.action.UserFinishedSettingUpParams
import com.pierudzki.aipowereddemoapp.ai.action.UserIsReadyToStartUsingBrain
import com.pierudzki.aipowereddemoapp.ai.action.UserPressedBackButton
import com.pierudzki.aipowereddemoapp.ai.answer.ShowParamsSettingScreen
import com.pierudzki.aipowereddemoapp.ai.answer.ShowWelcomeScreen
import com.pierudzki.aipowereddemoapp.core.ParamsSettingScreen
import com.pierudzki.aipowereddemoapp.core.WelcomeScreen
import com.pierudzki.aipowereddemoapp.core.WelcomeScreenViewModel

@Composable
fun BrainBasedApp() {
    val brainViewModel: BrainViewModel = viewModel()
    val answer by brainViewModel.answer.collectAsStateWithLifecycle()

    when (val current = answer) {
        is ShowWelcomeScreen -> {
            val welcomeScreenViewModel: WelcomeScreenViewModel = viewModel()
            val uiState by welcomeScreenViewModel.uiState.collectAsStateWithLifecycle()

            WelcomeScreen(
                uiState = uiState,
                onStartClicked = {
                    brainViewModel.onNewInputAction(UserIsReadyToStartUsingBrain())
                },
            )
        }

        is ShowParamsSettingScreen -> {
            val screenTexts by brainViewModel.paramsSettingsScreenTexts.collectAsStateWithLifecycle()

            BackHandler {
                brainViewModel.onNewInputAction(UserPressedBackButton())
            }

            ParamsSettingScreen(
                screenTexts = screenTexts,
                appLanguage = current.appLanguage,
                n = current.n,
                onAppLanguageChanged = {
                    brainViewModel.onNewInputAction(UserChangedAppLanguage(it))
                },
                onNextStepClicked = {
                    brainViewModel.onNewInputAction(UserFinishedSettingUpParams(it))
                },
            )
        }
    }
}
