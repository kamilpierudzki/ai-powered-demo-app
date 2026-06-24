package com.pierudzki.aipowereddemoapp.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun WelcomeScreen(
    onStandardFlowClicked: () -> Unit,
    onAiPoweredFlowClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
            ) {
            Text(
                text = "Welcome to the AI Powered Demo App!",
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Select the flow you want to use:",
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onStandardFlowClicked) {
                Text("Go with standard flow")
            }
            Button(onClick = onAiPoweredFlowClicked) {
                Text("Go with AI powered flow")
            }
        }
    }
}

@Preview
@Composable
private fun WelcomeScreenPrev() {
    WelcomeScreen(
        onStandardFlowClicked = {},
        onAiPoweredFlowClicked = {},
        modifier = Modifier
    )
}