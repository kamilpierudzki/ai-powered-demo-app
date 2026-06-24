package com.pierudzki.aipowereddemoapp.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun CalculationScreen(
    onCalculationCancelled: () -> Unit,
    modifier: Modifier = Modifier,
) {

    LaunchedEffect(Unit) {
        startCalculation()
    }

    DisposableEffect(Unit) {
        onDispose {
            onCalculationCancelled()
        }
    }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
        ) {
            CircularProgressIndicator()
            Text(text = "Calculating...")
        }
    }
}

private fun startCalculation() {
    // todo
}

@Preview
@Composable
private fun CalculationScreenPrev() {
    CalculationScreen(
        onCalculationCancelled = {},
        modifier = Modifier,
    )
}