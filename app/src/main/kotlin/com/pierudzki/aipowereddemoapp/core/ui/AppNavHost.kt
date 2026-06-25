package com.pierudzki.aipowereddemoapp.core.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pierudzki.aipowereddemoapp.ai.AppDestination

private const val ROUTE_AI = "ai"

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.WELCOME.id,
    ) {
        composable(AppDestination.WELCOME.id) {
            WelcomeScreen(
                onStandardFlowClicked = { navController.navigate(AppDestination.PARAMS.id) },
                onAiPoweredFlowClicked = { navController.navigate(ROUTE_AI) },
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
            AiNavigationScreen(
                onNavigateToDestination = { destination ->
                    navController.navigate(destination.id) { launchSingleTop = true }
                },
            )
        }
    }
}
