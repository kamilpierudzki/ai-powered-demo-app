package com.pierudzki.aipowereddemoapp.ai

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pierudzki.aipowereddemoapp.ai.action.CalculationDurationUpdated
import com.pierudzki.aipowereddemoapp.ai.action.CalculationFinished
import com.pierudzki.aipowereddemoapp.ai.action.UserChangedAppLanguage
import com.pierudzki.aipowereddemoapp.ai.action.UserFinishedSettingUpParams
import com.pierudzki.aipowereddemoapp.ai.action.UserIsReadyToStartUsingBrain
import com.pierudzki.aipowereddemoapp.ai.action.UserPressedBackButton
import com.pierudzki.aipowereddemoapp.ai.answer.ShowCalculationScreen
import com.pierudzki.aipowereddemoapp.ai.answer.ShowFailureScreen
import com.pierudzki.aipowereddemoapp.ai.answer.ShowParamsSettingScreenAndRefreshTexts
import com.pierudzki.aipowereddemoapp.ai.answer.ShowSuccessScreen
import com.pierudzki.aipowereddemoapp.ai.answer.ShowWelcomeScreen
import com.pierudzki.aipowereddemoapp.core.CalculationScreen
import com.pierudzki.aipowereddemoapp.core.CalculationScreenViewModel
import com.pierudzki.aipowereddemoapp.core.ParamsSettingScreen
import com.pierudzki.aipowereddemoapp.core.ParamsSettingScreenViewModel
import com.pierudzki.aipowereddemoapp.core.WelcomeScreen
import com.pierudzki.aipowereddemoapp.core.WelcomeScreenViewModel
import com.pierudzki.aipowereddemoapp.core.ui.FailureScreen
import com.pierudzki.aipowereddemoapp.core.ui.SuccessScreen

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

        is ShowParamsSettingScreenAndRefreshTexts -> {
            val paramsSettingScreenViewModel: ParamsSettingScreenViewModel = viewModel()
            val screenTexts by paramsSettingScreenViewModel.paramsSettingScreenTexts.collectAsStateWithLifecycle()

            LaunchedEffect(current.appLanguage) {
                paramsSettingScreenViewModel.refreshTextsOnScreenScreen(current.appLanguage)
            }

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

        is ShowCalculationScreen -> {
            val calculationScreenViewModel: CalculationScreenViewModel = viewModel()
            val values by calculationScreenViewModel.values.collectAsStateWithLifecycle()
            val calculationDurationSeconds by calculationScreenViewModel.calculationDurationSeconds.collectAsStateWithLifecycle()
            val isFinished by calculationScreenViewModel.isFinished.collectAsStateWithLifecycle()
            val screenHint by calculationScreenViewModel.screenHint.collectAsStateWithLifecycle()

            LaunchedEffect(current.n) {
                calculationScreenViewModel.startCalculation(current.n)
            }

            LaunchedEffect(calculationDurationSeconds) {
                if (calculationDurationSeconds > 0) {
                    brainViewModel.onNewInputAction(
                        CalculationDurationUpdated(
                            calculationDurationSeconds
                        )
                    )
                }
            }

            LaunchedEffect(isFinished) {
                if (isFinished) {
                    brainViewModel.onNewInputAction(CalculationFinished(calculationDurationSeconds))
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    calculationScreenViewModel.stopCalculation()
                }
            }

            BackHandler {
                brainViewModel.onNewInputAction(UserPressedBackButton())
            }

            LaunchedEffect(current.appLanguage) {
                calculationScreenViewModel.refreshTextsOnScreenScreen(current.appLanguage)
            }

            CalculationScreen(
                screenHint = screenHint,
                values = values,
                calculationDurationSeconds = calculationDurationSeconds,
            )
        }

        is ShowSuccessScreen -> {
            SuccessScreen(
                onBackClicked = {
                    brainViewModel.onNewInputAction(UserPressedBackButton())
                },
            )
        }

        is ShowFailureScreen -> {
            FailureScreen(
                onBackClicked = {
                    brainViewModel.onNewInputAction(UserPressedBackButton())
                },
            )
        }
    }
}
