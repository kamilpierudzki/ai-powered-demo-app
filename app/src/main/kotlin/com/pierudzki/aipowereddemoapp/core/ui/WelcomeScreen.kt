package com.pierudzki.aipowereddemoapp.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.tooling.preview.Preview
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

    LaunchedEffect(state) {
        isEditingEnabled =
            (state as? WelcomeScreenViewModel.UiState.Ready)?.startButtonTextLoading != true &&
                    state !is WelcomeScreenViewModel.UiState.Info &&
                    state !is WelcomeScreenViewModel.UiState.ModelUnavailable &&
                    state !is WelcomeScreenViewModel.UiState.Initializing
    }

    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
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

            if (state is WelcomeScreenViewModel.UiState.Initializing) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            if (state is WelcomeScreenViewModel.UiState.Info) {
                Text(
                    text = state.text,
                    color = androidx.compose.ui.graphics.Color.Red,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text("Choose language for the app:")
                Row {
                    TextField(
                        value = selectedLanguage,
                        onValueChange = { selectedLanguage = it },
                        enabled = isEditingEnabled,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            onSubmitLanguageClicked(selectedLanguage)
                        },
                        enabled = isEditingEnabled,
                    ) {
                        Text("Submit")
                    }
                }
            }

            if (state is WelcomeScreenViewModel.UiState.Ready) {
                Button(
                    onClick = onStartClicked,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = state.startButtonText,
                            modifier = Modifier.alpha(if (state.startButtonTextLoading) 0f else 1f),
                        )
                        if (state.startButtonTextLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = LocalContentColor.current,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun WelcomeScreenPrev1() {
    WelcomeScreen(
        state = WelcomeScreenViewModel.UiState.ModelUnavailable(
            modelName = "Model name",
            appLanguage = "english",
        ),
        onSubmitLanguageClicked = {},
        onStartClicked = {},
        modifier = Modifier
    )
}

@Preview
@Composable
private fun WelcomeScreenPrev2() {
    WelcomeScreen(
        state = WelcomeScreenViewModel.UiState.Initializing(
            modelName = "Model name",
            appLanguage = "english",
        ),
        onSubmitLanguageClicked = {},
        onStartClicked = {},
        modifier = Modifier
    )
}

@Preview
@Composable
private fun WelcomeScreenPrev3() {
    WelcomeScreen(
        state = WelcomeScreenViewModel.UiState.Ready(
            startButtonTextLoading = true,
            startButtonText = "loading",
            modelName = "Model name",
            appLanguage = "english"
        ),
        onSubmitLanguageClicked = {},
        onStartClicked = {},
        modifier = Modifier
    )
}

@Preview
@Composable
private fun WelcomeScreenPrev4() {
    WelcomeScreen(
        state = WelcomeScreenViewModel.UiState.Ready(
            startButtonTextLoading = false,
            startButtonText = "go to the next page",
            modelName = "Model name",
            appLanguage = "english"
        ),
        onSubmitLanguageClicked = {},
        onStartClicked = {},
        modifier = Modifier
    )
}

@Preview
@Composable
private fun WelcomeScreenPrev5() {
    WelcomeScreen(
        state = WelcomeScreenViewModel.UiState.Info(
            text = "something went wrong!",
            modelName = "Model name",
            appLanguage = "english"
        ),
        onSubmitLanguageClicked = {},
        onStartClicked = {},
        modifier = Modifier
    )
}