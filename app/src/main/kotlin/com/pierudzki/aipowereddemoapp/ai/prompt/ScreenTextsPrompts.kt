package com.pierudzki.aipowereddemoapp.ai.prompt

object ScreenTextsPrompts {

    fun paramsTexts(appLanguage: String): String =
        """
        You manage an Android mobile app. Suggest text for elements on the app screen.
        The screen contains two text fields. The first text field accepts the name of 
        the language in which the app's text should be generated. The second text field accepts
        the N value for the Fibonacci sequence.
        
        These text fields are:
        - A short title displayed in the app's top bar. Up to 4 words long.
        - A prompt text for the text field, where the user enters the language in which
        the app's text should be displayed on the screen. This text can be up to 20 words long.
        - A button text that triggers an on-screen text update. This text can be up to 4 words long.
        - A prompt text for the text field, where the user enters the selected N value for
        the Fibonacci sequence. This prompt text can be up to 20 words long.
        - A button text that updates the user-selected N value in the app's memory. 
        This text can be up to 4 words long. This button takes the user to another screen and starts
        the Fibonacci sequence calculation.

        The user wants the text to be in the following language: "${appLanguage}."
        
        Response with ONLY a single, minified JSON object, without Markdown constraints and without any additional text or explanation. Use these keys and meanings exactly:
        - "title": the screen title (point 1 above).
        - "languageHint": the text box hint for the app language (point 2 above).
        - "changeLanguageButton": the text box text that triggers the text update (point 3 above).
        - "nHint": the text box hint for the N value (point 4 above).
        - "changeNButton": the text box text that tells the app to update the N value (point 5 above).

        Example of the exact required format:
        {"title":"...","languageHint":"...","changeLanguageButton":"...","nHint":"...","changeNButton":"..."}
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
