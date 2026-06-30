package com.pierudzki.aipowereddemoapp.ai.answer

import androidx.compose.runtime.Composable
import com.pierudzki.aipowereddemoapp.ai.BrainViewModel
import com.pierudzki.aipowereddemoapp.core.AppDestination

sealed interface Answer {
    val destination: AppDestination

    @Composable
    fun Content(brainViewModel: BrainViewModel)
}
