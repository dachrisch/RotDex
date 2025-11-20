package com.rotdex.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rotdex.ui.screens.CardCreateScreen
import com.rotdex.ui.screens.CollectionScreen
import com.rotdex.ui.screens.DailyRewardsScreen
import com.rotdex.ui.screens.FusionScreen
import com.rotdex.ui.screens.HomeScreen
import com.rotdex.ui.viewmodel.CardCreateViewModel
import com.rotdex.ui.viewmodel.CollectionViewModel
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

        composable(Screen.Collection.route) { backStackEntry ->
            val viewModel: CollectionViewModel = hiltViewModel()
            // Force fresh state by keying on the backStackEntry instance
            // When navigating away and back, a new backStackEntry is created
            androidx.compose.runtime.key(backStackEntry) {
                CollectionScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.CardCreate.route) {
            val viewModel: CardCreateViewModel = hiltViewModel()
            CardCreateScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCollection = {
                    navController.navigate(Screen.Collection.route) {
                        // Pop CardCreate from back stack so back from Collection goes to Home
                        popUpTo(Screen.Home.route) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
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
