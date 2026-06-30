package com.pierudzki.aipowereddemoapp.core

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculationScreen(
    texts: CalculationScreenTexts,
    values: List<String>,
    calculationDurationSeconds: Int,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        onBackClicked()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = texts.title) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            CircularProgressIndicator()
            Text(
                text = calculationDurationSeconds.toString(),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = texts.message,
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
        texts = CalculationScreenTexts("calculating", "Calculating..."),
        values = listOf("1: 111", "2: 312", "3: 842"),
        calculationDurationSeconds = 42,
        onBackClicked = {},
        modifier = Modifier,
    )
}