package com.pierudzki.aipowereddemoapp.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pierudzki.aipowereddemoapp.ai.AiNavigationViewModel
import com.pierudzki.aipowereddemoapp.ai.AiUiState
import com.pierudzki.aipowereddemoapp.ai.AppDestination
import com.pierudzki.aipowereddemoapp.ai.ModelConfig

@Composable
fun AiNavigationScreen(
    onNavigateToDestination: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AiNavigationViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.destinations.collect(onNavigateToDestination)
    }

    var input by rememberSaveable { mutableStateOf("") }
    val canSubmit = uiState is AiUiState.Ready || uiState is AiUiState.Info

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Napisz, dokad chcesz przejsc, a model zdecyduje o ekranie.")

            when (val state = uiState) {
                AiUiState.ModelUnavailable ->
                    Text("Model niedostepny. Wgraj plik modelu do: ${ModelConfig.MODEL_PATH}")

                AiUiState.Initializing -> {
                    CircularProgressIndicator()
                    Text("Ladowanie modelu...")
                }

                AiUiState.Deciding -> {
                    CircularProgressIndicator()
                    Text("Analizuje...")
                }

                is AiUiState.Info -> Text(state.text)
                AiUiState.Ready -> {}
            }

            TextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Dokad chcesz przejsc?") },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { viewModel.submit(input) },
                enabled = canSubmit,
            ) {
                Text("Wyslij")
            }
        }
    }
}
