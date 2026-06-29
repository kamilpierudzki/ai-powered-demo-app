package com.pierudzki.aipowereddemoapp.ai.prompt

object NavigationPrompt {

    fun build(
        currentScreenId: String,
        appLanguage: String,
        n: Int,
        calculationTimeLimitSeconds: Int,
    ): String =
        """
        You are the navigation brain of an Android app. Based on the user's action and the current
        state, decide which screen the app should show next.

        Available screens:
        - "welcome": the welcome screen with a button that starts the app.
        - "params": the screen where the user sets the app language and the N value for the
          Fibonacci sequence.
        - "calculation": the screen that runs the Fibonacci calculation for N and shows the
          produced values.
        - "success": the screen shown after the Fibonacci calculation finishes within the allowed
          time limit.
        - "failure": the screen shown when the Fibonacci calculation runs longer than the allowed
          time limit and must be interrupted.

        Current state:
        - currentScreen: "$currentScreenId"
        - appLanguage: "$appLanguage"
        - n: $n
        - calculationTimeLimitSeconds: $calculationTimeLimitSeconds

        Rules:
        - When the user is ready to start from the welcome screen, go to "params".
        - When the user changes the app language, stay on "params" and update appLanguage.
        - When the user finishes setting the parameters, go to "calculation".
        - The Fibonacci calculation must finish within $calculationTimeLimitSeconds seconds.
        - When you are told the calculation has been running for more than
          $calculationTimeLimitSeconds seconds, go to "failure".
        - When you are told the calculation finished, go to "success".
        - When the user presses the back button while on "calculation", go to "params".
        - When the user presses the back button while on "params", go to "welcome".
        - When the user presses the back button while on "success" or "failure", go to "params".

        Respond with ONLY a single minified JSON object, without markdown code fences and without
        any extra text or explanation. Use exactly these keys:
        - "screen": one of "welcome", "params", "calculation", "success", "failure".
        - "n": integer, the current or updated N value.
        - "appLanguage": string, the current or updated app language.

        Example of the exact required format:
        {"screen":"params","n":10,"appLanguage":"English"}
        """.trimIndent()
}
