package com.pierudzki.aipowereddemoapp.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pierudzki.aipowereddemoapp.core.ResultScreenTexts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessScreen(
    texts: ResultScreenTexts,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Success",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(96.dp),
            )
            Text(
                text = texts.message,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                )
        }
    }
}

@Preview
@Composable
private fun SuccessScreenPrev() {
    SuccessScreen(
        texts = ResultScreenTexts(
            title = "Success",
            message = "Success! Your calculation has finished.",
        ),
        onBackClicked = {},
        modifier = Modifier,
    )
}
