package com.pierudzki.aipowereddemoapp.ai.answer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pierudzki.aipowereddemoapp.ai.BrainViewModel
import com.pierudzki.aipowereddemoapp.ai.action.UserPressedBackButton
import com.pierudzki.aipowereddemoapp.core.SuccessScreen

data class ShowSuccessScreen(val appLanguage: String) : Answer {
    @Composable
    override fun Content(brainViewModel: BrainViewModel) {
        val successTexts by brainViewModel.successTexts.collectAsStateWithLifecycle()

        LaunchedEffect(appLanguage) {
            brainViewModel.refreshSuccessTexts(appLanguage)
        }

        SuccessScreen(
            texts = successTexts,
            onBackClicked = {
                brainViewModel.onNewInputAction(UserPressedBackButton())
            },
        )
    }
}
