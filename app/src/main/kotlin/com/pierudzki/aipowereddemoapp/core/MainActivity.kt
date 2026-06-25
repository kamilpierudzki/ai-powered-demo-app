package com.pierudzki.aipowereddemoapp.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pierudzki.aipowereddemoapp.core.ui.AppNavHost
import com.pierudzki.aipowereddemoapp.core.ui.theme.AIPPoweredDemoAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIPPoweredDemoAppTheme {
                AppNavHost()
            }
        }
    }
}
