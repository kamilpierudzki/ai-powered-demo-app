package com.pierudzki.aipowereddemoapp.ai.answer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pierudzki.aipowereddemoapp.ai.AgentViewModel
import com.pierudzki.aipowereddemoapp.ai.action.UserPressedStartButton
import com.pierudzki.aipowereddemoapp.core.AppDestination
import com.pierudzki.aipowereddemoapp.core.WelcomeScreen

data object ShowWelcomeScreen : Answer {
    override val destination = AppDestination.WELCOME

    @Composable
    override fun Content(agentViewModel: AgentViewModel) {
        val uiState by agentViewModel.welcomeUiState.collectAsStateWithLifecycle()

        WelcomeScreen(
            uiState = uiState,
            onStartClicked = {
                agentViewModel.onNewInputAction(UserPressedStartButton())
            },
        )
    }
}
