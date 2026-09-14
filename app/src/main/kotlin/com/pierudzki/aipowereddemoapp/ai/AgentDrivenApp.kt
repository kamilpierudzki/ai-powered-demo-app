package com.pierudzki.aipowereddemoapp.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AgentDrivenApp() {
    val agentViewModel: AgentViewModel = viewModel()
    val answer by agentViewModel.answer.collectAsStateWithLifecycle()
    answer.Content(agentViewModel)
}
