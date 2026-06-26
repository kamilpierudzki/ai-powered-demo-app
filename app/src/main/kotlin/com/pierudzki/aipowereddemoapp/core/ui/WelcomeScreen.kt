package com.pierudzki.aipowereddemoapp.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pierudzki.aipowereddemoapp.R
import com.pierudzki.aipowereddemoapp.core.WelcomeScreenViewModel

@Composable
fun WelcomeScreen(
    state: WelcomeScreenViewModel.UiState,
    onSubmitLanguageClicked: (String) -> Unit,
    onStartClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedLanguage by remember { mutableStateOf(state.appLanguage) }
    var isEditingEnabled by remember { mutableStateOf(false) }
    var hintText by remember { mutableStateOf("") }
    var submitButtonText by remember { mutableStateOf("") }
    var startButtonText by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        isEditingEnabled =
            state !is WelcomeScreenViewModel.UiState.Error &&
                    state !is WelcomeScreenViewModel.UiState.ModelUnavailable &&
                    state !is WelcomeScreenViewModel.UiState.Initializing &&
                    !state.loadingTexts


        hintText = state.languageSelectionHintText
        submitButtonText = state.submitButtonText
        startButtonText = state.startButtonText
    }

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
                        append(state.modelName)
                    }
                },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
            )

            if (state is WelcomeScreenViewModel.UiState.Initializing || state.loadingTexts) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            if (state is WelcomeScreenViewModel.UiState.Error) {
                Text(
                    text = state.text,
                    color = androidx.compose.ui.graphics.Color.Red,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(hintText)
                TextField(
                    value = selectedLanguage,
                    onValueChange = { selectedLanguage = it },
                    enabled = isEditingEnabled,
                )
                Button(
                    onClick = {
                        onSubmitLanguageClicked(selectedLanguage)
                    },
                    enabled = isEditingEnabled,
                ) {
                    Text(submitButtonText)
                }
            }

            if (state is WelcomeScreenViewModel.UiState.EngineReady) {
                Button(
                    onClick = onStartClicked,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = startButtonText,
                        modifier = Modifier.alpha(if (state.loadingTexts) 0f else 1f),
                    )
                }
            }
        }
    }
}