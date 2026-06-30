package com.pierudzki.aipowereddemoapp.ai.answer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pierudzki.aipowereddemoapp.ai.BrainViewModel
import com.pierudzki.aipowereddemoapp.ai.action.UserIsReadyToStartUsingBrain
import com.pierudzki.aipowereddemoapp.core.WelcomeScreen

data object ShowWelcomeScreen : Answer {
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
