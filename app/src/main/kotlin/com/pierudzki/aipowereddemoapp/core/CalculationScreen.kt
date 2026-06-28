package com.pierudzki.aipowereddemoapp.core

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.ai.edge.litertlm.Content

@Composable
fun CalculationScreen(
    screenHint: String,
    values: List<String>,
    calculationDurationSeconds: Int,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            CircularProgressIndicator()
            Text(calculationDurationSeconds.toString())
            Text(
                text = screenHint,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(vertical = 8.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                items(values) { value ->
                    Text(text = value)
                }
            }
        }
    }
}

@Preview
@Composable
private fun CalculationScreenPrev() {
    CalculationScreen(
        screenHint = "Calculating...",
        values = listOf("1: 111", "2: 312", "3: 842"),
        calculationDurationSeconds = 42,
        modifier = Modifier,
    )
}