package com.pierudzki.aipowereddemoapp.core

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pierudzki.aipowereddemoapp.R

@Composable
fun WelcomeScreen(
    uiState: WelcomeScreenViewModel.UiState,
    onStartClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Welcome to\n")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(stringResource(id = R.string.app_name))
                    }
                    append("\n")
                    append("The app is powered by\n")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(uiState.modelName)
                    }
                },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
            )

            if (uiState is WelcomeScreenViewModel.UiState.Initializing) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            if (uiState is WelcomeScreenViewModel.UiState.Error) {
                Text(
                    text = uiState.text,
                    color = androidx.compose.ui.graphics.Color.Red,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            if (uiState is WelcomeScreenViewModel.UiState.EngineReady) {
                Button(
                    onClick = onStartClicked,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Start the Brain")
                }
            }
        }
    }
}