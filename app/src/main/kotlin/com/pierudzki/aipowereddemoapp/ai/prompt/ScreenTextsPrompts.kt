package com.pierudzki.aipowereddemoapp.ai.prompt

object ScreenTextsPrompts {

    fun paramsTexts(appLanguage: String): String =
        """
        You manage an Android mobile app. Suggest text for elements on the app' screen.

        These texts are:
        - The prompt text for the text field where the user enters the language in which
        the app's on-screen text should appear. This text can be up to 20 words long.
        - The button text that triggers on-screen text updates. This text can be up to 4 words long.
        - The prompt text for the text field where the user enters the selected value N for
        the Fibonacci sequence. The prompt text can be up to 20 words long.
        - The button text that updates the value N selected by the user in the app's memory.
        This text can be up to 4 words long. This button triggers the calculation of the Fibonacci.

        The user wants the text to be in the following language: "${appLanguage}."

        Respond with ONLY a single minified JSON object, without markdown code fences and
        without any extra text or explanation. Use exactly these keys and meanings:
        - "languageHint": the text field hint for the app language (item 1 above).
        - "changeLanguageButton": the button text that triggers text updates (item 2 above).
        - "nHint": the text field hint for the N value (item 3 above).
        - "changeNButton": the button text that tells the app to update the N value (item 4 above).

        Example of the exact required format:
        {"languageHint":"...","changeLanguageButton":"...","nHint":"...","changeNButton":"..."}
        """.trimIndent()

    fun calculationTexts(appLanguage: String): String =
        """
        You manage an Android mobile app. Suggest text for the screen shown while the app is
        calculating the Fibonacci sequence.

        These texts are:
        - A short screen title shown in the top app bar. Up to 3 words.
        - A short status message telling the user that a calculation is in progress and to
        please wait. Up to 10 words.

        The user wants the text to be in the following language: "${appLanguage}."

        Respond with ONLY a single minified JSON object, without markdown code fences and
        without any extra text or explanation. Use exactly these keys and meanings:
        - "title": the screen title.
        - "message": the in-progress status message.

        Example of the exact required format:
        {"title":"...","message":"..."}
        """.trimIndent()

    fun successTexts(appLanguage: String): String =
        """
        You manage an Android mobile app. Suggest text for the success screen shown after the
        Fibonacci calculation finished within the allowed time limit.

        These texts are:
        - A short screen title shown in the top app bar. Up to 3 words.
        - A short congratulatory message telling the user the calculation finished successfully.
        Up to 15 words.

        The user wants the text to be in the following language: "${appLanguage}."

        Respond with ONLY a single minified JSON object, without markdown code fences and
        without any extra text or explanation. Use exactly these keys and meanings:
        - "title": the screen title.
        - "message": the success message.

        Example of the exact required format:
        {"title":"...","message":"..."}
        """.trimIndent()

    fun failureTexts(appLanguage: String): String =
        """
        You manage an Android mobile app. Suggest text for the failure screen shown when the
        Fibonacci calculation took too long and had to be interrupted.

        These texts are:
        - A short screen title shown in the top app bar. Up to 3 words.
        - A short message telling the user the calculation took too long and was stopped.
        Up to 15 words.

        The user wants the text to be in the following language: "${appLanguage}."

        Respond with ONLY a single minified JSON object, without markdown code fences and
        without any extra text or explanation. Use exactly these keys and meanings:
        - "title": the screen title.
        - "message": the failure message.

        Example of the exact required format:
        {"title":"...","message":"..."}
        """.trimIndent()
}
