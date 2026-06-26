package com.pierudzki.aipowereddemoapp.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pierudzki.aipowereddemoapp.ai.AppDestination
import com.pierudzki.aipowereddemoapp.core.WelcomeScreenViewModel

private const val ROUTE_AI = "ai"

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.WELCOME.id,
    ) {
        composable(AppDestination.WELCOME.id) {
            val viewModel: WelcomeScreenViewModel = viewModel()

            val state by viewModel.uiState.collectAsStateWithLifecycle()

            WelcomeScreen(
                state = state,
                onSubmitLanguageClicked = {
                    viewModel.onAppLanguageUpdated(it)
                },
                onStartClicked = {
                    // todo
                },
            )
        }
        composable(AppDestination.PARAMS.id) {
            ParamsScreen(
                onParamsSubmittedClicked = { navController.navigate(AppDestination.CALCULATION.id) },
            )
        }
        composable(AppDestination.CALCULATION.id) {
            CalculationScreen(
                onCalculationCancelled = { navController.popBackStack() },
            )
        }
        composable(AppDestination.SUCCESS.id) {
            SuccessScreen(
                onBackClicked = { navController.popBackStack() },
            )
        }
        composable(ROUTE_AI) {
            /*AiNavigationScreen(
                onNavigateToDestination = { destination ->
                    navController.navigate(destination.id) { launchSingleTop = true }
                },
            )*/
        }
    }
}
