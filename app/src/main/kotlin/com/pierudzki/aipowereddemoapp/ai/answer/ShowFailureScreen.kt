package com.pierudzki.aipowereddemoapp.ai.answer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pierudzki.aipowereddemoapp.ai.BrainViewModel
import com.pierudzki.aipowereddemoapp.ai.action.UserPressedBackButton
import com.pierudzki.aipowereddemoapp.core.FailureScreen

data class ShowFailureScreen(val appLanguage: String) : Answer {
    @Composable
    override fun Content(brainViewModel: BrainViewModel) {
        val failureTexts by brainViewModel.failureTexts.collectAsStateWithLifecycle()

        LaunchedEffect(appLanguage) {
            brainViewModel.refreshFailureTexts(appLanguage)
        }

        FailureScreen(
            texts = failureTexts,
            onBackClicked = {
                brainViewModel.onNewInputAction(UserPressedBackButton())
            },
        )
    }
}
