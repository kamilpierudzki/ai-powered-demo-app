package com.pierudzki.aipowereddemoapp.core

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ParamsSettingScreen(
    screenTexts: ParamsSettingScreenTexts,
    appLanguage: String,
    n: Int,
    onAppLanguageChanged: (String) -> Unit,
    onNextStepClicked: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedLanguage by remember { mutableStateOf("") }
    var selectedNText by remember { mutableStateOf("") }

    LaunchedEffect(appLanguage) {
        selectedLanguage = appLanguage
    }

    LaunchedEffect(n) {
        selectedNText = n.toString()
    }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(modifier.padding(innerPadding)) {
            Text(screenTexts.languageHint)
            TextField(
                value = selectedLanguage,
                onValueChange = { selectedLanguage = it },
                enabled = !screenTexts.loading,
            )
            Button(
                onClick = {
                    onAppLanguageChanged(selectedLanguage)
                },
                enabled = !screenTexts.loading,
            ) {
                Text(screenTexts.changeLanguageButton)
            }

            Spacer(modifier = Modifier.padding(16.dp))

            Text(screenTexts.nHint)
            TextField(
                value = selectedNText,
                onValueChange = { nText ->
                    selectedNText = nText
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = {
                val n = selectedNText.toIntOrNull()
                if (n != null) {
                    onNextStepClicked(n)
                }
            }) {
                Text(screenTexts.saveNButton)
            }
        }
    }
}