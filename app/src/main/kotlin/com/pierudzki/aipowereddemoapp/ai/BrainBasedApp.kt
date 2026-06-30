package com.pierudzki.aipowereddemoapp.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun BrainBasedApp() {
    val brainViewModel: BrainViewModel = viewModel()
    val answer by brainViewModel.answer.collectAsStateWithLifecycle()
    answer.Content(brainViewModel)
}
