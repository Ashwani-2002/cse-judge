package com.csejudge.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.csejudge.app.ui.analysis.AnalysisScreen
import com.csejudge.app.ui.analysis.AnalysisViewModel
import com.csejudge.app.ui.bookmark.BookmarkMicrothemesScreen
import com.csejudge.app.ui.bookmark.BookmarkQuestionScreen
import com.csejudge.app.ui.bookmark.BookmarkSubjectsScreen
import com.csejudge.app.ui.bookmark.BookmarkViewModel
import com.csejudge.app.ui.home.HomeScreen
import com.csejudge.app.ui.practice.PracticeFilterScreen
import com.csejudge.app.ui.practice.PracticeQuestionScreen
import com.csejudge.app.ui.practice.PracticeViewModel

/**
 * Top-level Navigation Host for CSE Judge.
 * Manages destination routes and ensures ViewModel instances
 * are retained while navigating between study flows.
 *
 * Flows:
 * 1. Normal Practice: Home -> Practice Filter -> Practice Question
 * 2. Bookmarked Questions: Home -> Bookmark Subjects -> Bookmark Microthemes -> Bookmark Questions
 * 3. Performance Analysis: Home -> Analysis Dashboard
 */
@Composable
fun AppNavHost(
    practiceViewModel: PracticeViewModel,
    bookmarkViewModel: BookmarkViewModel? = null,
    analysisViewModel: AnalysisViewModel? = null,
    isDarkMode: Boolean = false,
    onToggleTheme: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val bookmarkUiState by (bookmarkViewModel?.uiState?.collectAsState()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(null) })

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToPractice = {
                    navController.navigate(Screen.PracticeFilter.route)
                },
                onNavigateToBookmarks = {
                    navController.navigate(Screen.BookmarkSubjects.route)
                },
                onNavigateToAnalysis = {
                    navController.navigate(Screen.Analysis.route)
                },
                bookmarkCount = bookmarkUiState?.totalBookmarkCount ?: 0,
                isDarkMode = isDarkMode,
                onToggleTheme = onToggleTheme
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

        // Bookmark Hierarchy Level 1: Subjects
        composable(Screen.BookmarkSubjects.route) {
            if (bookmarkViewModel != null) {
                BookmarkSubjectsScreen(
                    viewModel = bookmarkViewModel,
                    onSelectSubject = { subject ->
                        navController.navigate(Screen.BookmarkMicrothemes.createRoute(subject))
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPractice = {
                        navController.navigate(Screen.PracticeFilter.route)
                    }
                )
            }
        }

        // Bookmark Hierarchy Level 2: Microthemes
        composable(
            route = Screen.BookmarkMicrothemes.route,
            arguments = listOf(
                navArgument("subject") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedSubject = backStackEntry.arguments?.getString("subject").orEmpty()
            val subject = RouteUtils.decode(encodedSubject)

            if (bookmarkViewModel != null) {
                BookmarkMicrothemesScreen(
                    subject = subject,
                    viewModel = bookmarkViewModel,
                    onSelectMicrotheme = { subj, microtheme ->
                        navController.navigate(Screen.BookmarkQuestions.createRoute(subj, microtheme))
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // Bookmark Hierarchy Level 3: Questions
        composable(
            route = Screen.BookmarkQuestions.route,
            arguments = listOf(
                navArgument("subject") { type = NavType.StringType },
                navArgument("microtheme") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedSubject = backStackEntry.arguments?.getString("subject").orEmpty()
            val encodedMicrotheme = backStackEntry.arguments?.getString("microtheme").orEmpty()
            val subject = RouteUtils.decode(encodedSubject)
            val microtheme = RouteUtils.decode(encodedMicrotheme)

            if (bookmarkViewModel != null) {
                BookmarkQuestionScreen(
                    subject = subject,
                    microtheme = microtheme,
                    viewModel = bookmarkViewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // Performance Analysis Dashboard
        composable(Screen.Analysis.route) {
            if (analysisViewModel != null) {
                AnalysisScreen(
                    viewModel = analysisViewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
