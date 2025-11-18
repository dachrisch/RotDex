package com.rotdex.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rotdex.ui.screens.DailyRewardsScreen
import com.rotdex.ui.screens.FusionScreen
import com.rotdex.ui.screens.HomeScreen
import com.rotdex.ui.viewmodel.DailyRewardsViewModel
import com.rotdex.ui.viewmodel.FusionViewModel

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object DailyRewards : Screen("daily_rewards")
    object Collection : Screen("collection")
    object CardCreate : Screen("card_create")
    object Fusion : Screen("fusion")
}

/**
 * Main navigation graph
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToDailyRewards = { navController.navigate(Screen.DailyRewards.route) },
                onNavigateToCollection = { navController.navigate(Screen.Collection.route) },
                onNavigateToCardCreate = { navController.navigate(Screen.CardCreate.route) },
                onNavigateToFusion = { navController.navigate(Screen.Fusion.route) }
            )
        }

        composable(Screen.DailyRewards.route) {
            val viewModel: DailyRewardsViewModel = hiltViewModel()
            DailyRewardsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Placeholder screens - to be implemented
        composable(Screen.Collection.route) {
            PlaceholderScreen(
                title = "Card Collection",
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CardCreate.route) {
            PlaceholderScreen(
                title = "Create Card",
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Fusion.route) {
            val viewModel: FusionViewModel = hiltViewModel()
            FusionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
