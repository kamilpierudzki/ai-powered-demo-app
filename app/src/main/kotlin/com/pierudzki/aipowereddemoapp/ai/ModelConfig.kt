package com.pierudzki.aipowereddemoapp.ai

/**
 * Static configuration for the on-device generative model used by [OnDeviceAiNavigator].
 *
 * The model file is too large to ship inside the APK, so during development it is pushed to the
 * device with `adb` (see project plan). For production it should be downloaded at runtime instead.
 */
object ModelConfig {
    const val MODEL_FILE_NAME = "gemma3-1b-it-int4.task"
    const val MODEL_PATH = "/data/local/tmp/llm/$MODEL_FILE_NAME"

    const val SYSTEM_PROMPT =
        "Jestes asystentem nawigacji w aplikacji. Na podstawie wiadomosci uzytkownika " +
            "zawsze wywoluj narzedzie navigateToScreen z wlasciwym ekranem docelowym."
}
