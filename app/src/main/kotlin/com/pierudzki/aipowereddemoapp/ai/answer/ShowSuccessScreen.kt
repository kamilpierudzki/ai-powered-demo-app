package com.pierudzki.aipowereddemoapp.ai.answer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pierudzki.aipowereddemoapp.ai.AgentViewModel
import com.pierudzki.aipowereddemoapp.ai.action.UserPressedBackButton
import com.pierudzki.aipowereddemoapp.core.AppDestination
import com.pierudzki.aipowereddemoapp.core.SuccessScreen

data class ShowSuccessScreen(val appLanguage: String) : Answer {
    override val destination: AppDestination get() = AppDestination.SUCCESS

    @Composable
    override fun Content(agentViewModel: AgentViewModel) {
        val successTexts by agentViewModel.successTexts.collectAsStateWithLifecycle()

        LaunchedEffect(appLanguage) {
            agentViewModel.refreshSuccessTexts(appLanguage)
        }

        SuccessScreen(
            texts = successTexts,
            onBackClicked = {
                agentViewModel.onNewInputAction(UserPressedBackButton())
            },
        )
    }
}
