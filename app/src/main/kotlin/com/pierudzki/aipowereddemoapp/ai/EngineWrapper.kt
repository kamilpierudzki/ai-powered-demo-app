package com.pierudzki.aipowereddemoapp.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

sealed interface EngineState {

    val modelName: String
    val appLanguage: String

    data class ModelUnavailable(
        override val modelName: String,
        override val appLanguage: String,
    ) : EngineState

    data class Initializing(
        override val modelName: String,
        override val appLanguage: String,
    ) : EngineState

    data class Ready(
        override val modelName: String,
        override val appLanguage: String,
    ) : EngineState

    data class Error(
        val message: String,
        override val modelName: String,
        override val appLanguage: String,
    ) : EngineState
}

object EngineWrapper {

    var engine: Engine? = null
        private set

    var appLanguage: String = "english"

    private val _state = MutableStateFlow<EngineState>(
        EngineState.Initializing(
            modelName = modelName(),
            appLanguage = appLanguage,
        )
    )
    val state: StateFlow<EngineState> = _state.asStateFlow()

    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        if (engine != null) {
            android.util.Log.d("EngineWrapper", "Already initialized")
            return@withContext
        }
        if (!isModelAvailable()) {
            android.util.Log.d("EngineWrapper", "initialize(...), ModelUnavailable")
            _state.value = EngineState.ModelUnavailable(
                modelName = modelName(),
                appLanguage = appLanguage,
            )
        } else {
            val appContext = context.applicationContext
            val config = EngineConfig(
                modelPath = ModelConfig.MODEL_PATH,
                backend = Backend.GPU(), // devices without GPU: Backend.CPU()
                cacheDir = appContext.cacheDir.path, // /data/local/tmp may not be writable by the app
            )
            try {
                engine = Engine(config).also {
                    android.util.Log.d("EngineWrapper", "initialize(...), Initializing")
                    _state.value = EngineState.Initializing(
                        modelName = modelName(),
                        appLanguage = appLanguage,
                    )
                    it.initialize()
                    android.util.Log.d("EngineWrapper", "initialize(...), Ready")
                    _state.value = EngineState.Ready(
                        modelName = modelName(),
                        appLanguage = appLanguage,
                    )
                }
            } catch (e: Exception) {
                android.util.Log.d("EngineWrapper", "initialize(...), Error: ${e.message}")
                _state.value = EngineState.Error(
                    message = "Initialization error: ${e.message}",
                    modelName = modelName(),
                    appLanguage = appLanguage,
                )
            }
        }
    }

    fun close() {
        android.util.Log.d("EngineWrapper", "close()")
        engine?.close()
        engine = null
    }

    private fun isModelAvailable(): Boolean = File(ModelConfig.MODEL_PATH).exists()

    private fun modelName(): String = ModelConfig.MODEL_PATH
}