package com.pierudzki.aipowereddemoapp.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ParamsScreen(
    onParamsSubmittedClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {

    var timeInSeconds by rememberSaveable { mutableStateOf("") }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(modifier.padding(innerPadding)) {
            Text(text = "How long do you want your calculation should take?")
            TextField(
                value = timeInSeconds,
                onValueChange = { timeInSeconds = it },
                label = { Text("Time in seconds") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onParamsSubmittedClicked) {
                Text("Submit")
            }
        }
    }
}

@Preview
@Composable
private fun ParamsScreenPrev() {
    ParamsScreen(
        onParamsSubmittedClicked = {},
        modifier = Modifier,
    )
}