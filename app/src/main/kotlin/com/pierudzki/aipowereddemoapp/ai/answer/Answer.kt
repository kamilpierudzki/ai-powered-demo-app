package com.pierudzki.aipowereddemoapp.ai.answer

import androidx.compose.runtime.Composable
import com.pierudzki.aipowereddemoapp.ai.BrainViewModel

sealed interface Answer {
    @Composable
    fun Content(brainViewModel: BrainViewModel)
}
