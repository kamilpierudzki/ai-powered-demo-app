package com.pierudzki.aipowereddemoapp.ai.answer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pierudzki.aipowereddemoapp.ai.BrainViewModel
import com.pierudzki.aipowereddemoapp.ai.action.CalculationDurationUpdated
import com.pierudzki.aipowereddemoapp.ai.action.CalculationFinished
import com.pierudzki.aipowereddemoapp.ai.action.UserPressedBackButton
import com.pierudzki.aipowereddemoapp.core.AppDestination
import com.pierudzki.aipowereddemoapp.core.CalculationScreen
import com.pierudzki.aipowereddemoapp.core.CalculationScreenViewModel

data class ShowCalculationScreen(
    val n: Int,
    val appLanguage: String,
) : Answer {
    override val destination: AppDestination get() = AppDestination.CALCULATION

    @Composable
    override fun Content(brainViewModel: BrainViewModel) {
        val calculationScreenViewModel: CalculationScreenViewModel = viewModel()
        val values by calculationScreenViewModel.values.collectAsStateWithLifecycle()
        val calculationDurationSeconds by calculationScreenViewModel.calculationDurationSeconds.collectAsStateWithLifecycle()
        val isFinished by calculationScreenViewModel.isFinished.collectAsStateWithLifecycle()
        val calculationTexts by brainViewModel.calculationTexts.collectAsStateWithLifecycle()

        LaunchedEffect(n) {
            calculationScreenViewModel.startCalculation(n)
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

        LaunchedEffect(appLanguage) {
            brainViewModel.refreshCalculationTexts(appLanguage)
        }

        CalculationScreen(
            texts = calculationTexts,
            values = values,
            calculationDurationSeconds = calculationDurationSeconds,
            onBackClicked = {
                brainViewModel.onNewInputAction(UserPressedBackButton())
            }
        )
    }
}
