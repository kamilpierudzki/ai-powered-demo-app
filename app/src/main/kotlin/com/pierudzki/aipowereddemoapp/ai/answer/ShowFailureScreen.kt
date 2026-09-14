package com.pierudzki.aipowereddemoapp.ai.answer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pierudzki.aipowereddemoapp.ai.AgentViewModel
import com.pierudzki.aipowereddemoapp.ai.action.UserPressedBackButton
import com.pierudzki.aipowereddemoapp.core.AppDestination
import com.pierudzki.aipowereddemoapp.core.FailureScreen

data class ShowFailureScreen(val appLanguage: String) : Answer {
    override val destination: AppDestination get() = AppDestination.FAILURE

    @Composable
    override fun Content(agentViewModel: AgentViewModel) {
        val failureTexts by agentViewModel.failureTexts.collectAsStateWithLifecycle()

        LaunchedEffect(appLanguage) {
            agentViewModel.refreshFailureTexts(appLanguage)
        }

        FailureScreen(
            texts = failureTexts,
            onBackClicked = {
                agentViewModel.onNewInputAction(UserPressedBackButton())
            },
        )
    }
}
