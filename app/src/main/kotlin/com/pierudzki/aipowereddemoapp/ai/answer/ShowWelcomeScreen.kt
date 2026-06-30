package com.pierudzki.aipowereddemoapp.ai.answer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pierudzki.aipowereddemoapp.ai.BrainViewModel
import com.pierudzki.aipowereddemoapp.ai.action.UserIsReadyToStartUsingBrain
import com.pierudzki.aipowereddemoapp.core.AppDestination
import com.pierudzki.aipowereddemoapp.core.WelcomeScreen

data object ShowWelcomeScreen : Answer {
    override val destination = AppDestination.WELCOME

    @Composable
    override fun Content(brainViewModel: BrainViewModel) {
        val uiState by brainViewModel.welcomeUiState.collectAsStateWithLifecycle()

        WelcomeScreen(
            uiState = uiState,
            onStartClicked = {
                brainViewModel.onNewInputAction(UserIsReadyToStartUsingBrain())
            },
        )
    }
}
