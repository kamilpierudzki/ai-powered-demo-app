package com.pierudzki.aipowereddemoapp.ai.answer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pierudzki.aipowereddemoapp.ai.AgentViewModel
import com.pierudzki.aipowereddemoapp.ai.action.UserChangedAppLanguage
import com.pierudzki.aipowereddemoapp.ai.action.UserFinishedSettingUpParams
import com.pierudzki.aipowereddemoapp.ai.action.UserPressedBackButton
import com.pierudzki.aipowereddemoapp.core.AppDestination
import com.pierudzki.aipowereddemoapp.core.ParamsSettingScreen

data class ShowParamsSettingScreen(
    val n: Int,
    val appLanguage: String,
) : Answer {
    override val destination: AppDestination get() = AppDestination.PARAMS

    @Composable
    override fun Content(agentViewModel: AgentViewModel) {
        val screenTexts by agentViewModel.paramsTexts.collectAsStateWithLifecycle()

        LaunchedEffect(appLanguage) {
            agentViewModel.refreshParamsTexts(appLanguage)
        }

        ParamsSettingScreen(
            texts = screenTexts,
            appLanguage = appLanguage,
            n = n,
            onAppLanguageChanged = {
                agentViewModel.onNewInputAction(UserChangedAppLanguage(it))
            },
            onNextStepClicked = {
                agentViewModel.onNewInputAction(
                    UserFinishedSettingUpParams(n = it, appLanguage = appLanguage)
                )
            },
            onBackClicked = {
                agentViewModel.onNewInputAction(UserPressedBackButton())
            }
        )
    }
}
