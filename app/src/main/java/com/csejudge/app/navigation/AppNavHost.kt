package com.csejudge.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.csejudge.app.ui.home.HomeScreen
import com.csejudge.app.ui.practice.PracticeFilterScreen
import com.csejudge.app.ui.practice.PracticeQuestionScreen
import com.csejudge.app.ui.practice.PracticeViewModel

/**
 * Top-level Navigation Host for CSE Judge.
 * Manages destination routes and ensures the same PracticeViewModel instance
 * is retained while navigating between the filter and question screens.
 *
 * Flow: Home Screen -> Practice Filter Screen -> Practice Question Screen
 */
@Composable
fun AppNavHost(
    practiceViewModel: PracticeViewModel,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToPractice = {
                    navController.navigate(Screen.PracticeFilter.route)
                }
            )
        }

        composable(Screen.PracticeFilter.route) {
            PracticeFilterScreen(
                viewModel = practiceViewModel,
                onStartPractice = {
                    navController.navigate(Screen.PracticeQuestion.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.PracticeQuestion.route) {
            PracticeQuestionScreen(
                viewModel = practiceViewModel,
                onBackToFilter = {
                    navController.popBackStack()
                }
            )
        }
    }
}
