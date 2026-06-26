package com.pierudzki.aipowereddemoapp.ai

/**
 * Static configuration for the on-device generative model used by [OnDeviceAiNavigator].
 *
 * The model file is too large to ship inside the APK, so during development it is pushed to the
 * device with `adb` (see project plan). For production it should be downloaded at runtime instead.
 */
object ModelConfig {
    const val MODEL_FILE_NAME = "gemma-4-E4B-it.litertlm"
    const val MODEL_PATH = "/data/local/tmp/llm/$MODEL_FILE_NAME"
}
