package com.pierudzki.aipowereddemoapp.core

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParamsSettingScreen(
    texts: ParamsSettingScreenTexts,
    appLanguage: String,
    n: Int,
    onAppLanguageChanged: (String) -> Unit,
    onNextStepClicked: (Int) -> Unit,
    onBackClicked: () -> Unit,
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

    BackHandler {
        onBackClicked()
    }

    Scaffold(
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
        modifier = modifier,
    ) { innerPadding ->
        Column(modifier.padding(innerPadding)) {
            Text(texts.languageHint)
            TextField(
                value = selectedLanguage,
                onValueChange = { selectedLanguage = it },
                enabled = !texts.loading,
            )
            Button(
                onClick = {
                    onAppLanguageChanged(selectedLanguage)
                },
                enabled = !texts.loading,
            ) {
                Text(texts.changeLanguageButton)
            }

            Spacer(modifier = Modifier.padding(16.dp))

            Text(texts.nHint)
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
                Text(texts.saveNButton)
            }
        }
    }
}