package com.pierudzki.aipowereddemoapp.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.pierudzki.aipowereddemoapp.ai.EngineWrapper
import com.pierudzki.aipowereddemoapp.core.ui.AppNavHost
import com.pierudzki.aipowereddemoapp.core.ui.theme.AIPPoweredDemoAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeEngine()
        enableEdgeToEdge()
        setContent {
            AIPPoweredDemoAppTheme {
                AppNavHost()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && !isChangingConfigurations) {
            EngineWrapper.close()
        }
    }

    private fun initializeEngine() {
        lifecycleScope.launch {
            EngineWrapper.initialize(applicationContext)
        }
    }
}
