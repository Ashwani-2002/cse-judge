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
import com.csejudge.app.data.model.TestType
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
import com.csejudge.app.ui.test.PastTestsScreen
import com.csejudge.app.ui.test.TestInstructionsScreen
import com.csejudge.app.ui.test.TestQuestionScreen
import com.csejudge.app.ui.test.TestReportScreen
import com.csejudge.app.ui.test.TestViewModel
import com.csejudge.app.ui.test.TestsLandingScreen

/**
 * Top-level Navigation Host for CSE Judge.
 * Manages destination routes and ensures ViewModel instances
 * are retained while navigating between study flows.
 *
 * Flows:
 * 1. Normal Practice: Home -> Practice Filter -> Practice Question
 * 2. Tests Module: Home -> Tests Landing -> Instructions -> Active Test -> Test Report / Past Tests
 * 3. Bookmarked Questions: Home -> Bookmark Subjects -> Bookmark Microthemes -> Bookmark Questions
 * 4. Performance Analysis: Home -> Analysis Dashboard
 */
@Composable
fun AppNavHost(
    practiceViewModel: PracticeViewModel,
    bookmarkViewModel: BookmarkViewModel? = null,
    analysisViewModel: AnalysisViewModel? = null,
    testViewModel: TestViewModel? = null,
    isDarkMode: Boolean = false,
    onToggleTheme: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val bookmarkUiState by (bookmarkViewModel?.uiState?.collectAsState()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(null) })

    val testUiState by (testViewModel?.uiState?.collectAsState()
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
                onNavigateToTests = {
                    navController.navigate(Screen.TestsLanding.route)
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

        // Tests Module Destination 1: Landing Screen
        composable(Screen.TestsLanding.route) {
            TestsLandingScreen(
                onSelectTestType = { testType ->
                    navController.navigate(Screen.TestInstructions.createRoute(testType.name))
                },
                onNavigateToPastTests = {
                    navController.navigate(Screen.PastTests.route)
                },
                onBack = {
                    navController.popBackStack()
                },
                abortedMessage = testUiState?.abortReason,
                onDismissAbortedMessage = {
                    testViewModel?.clearAbortedState()
                }
            )
        }

        // Tests Module Destination 2: Instructions Screen
        composable(
            route = Screen.TestInstructions.route,
            arguments = listOf(
                navArgument("testType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedType = backStackEntry.arguments?.getString("testType").orEmpty()
            val typeStr = RouteUtils.decode(encodedType)
            val testType = TestType.fromString(typeStr)

            if (testViewModel != null) {
                TestInstructionsScreen(
                    testType = testType,
                    viewModel = testViewModel,
                    onStartTest = {
                        navController.navigate(Screen.TestActive.route)
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // Tests Module Destination 3: Active Question Screen
        composable(Screen.TestActive.route) {
            if (testViewModel != null) {
                TestQuestionScreen(
                    viewModel = testViewModel,
                    onTestCompleted = { testId ->
                        navController.navigate(Screen.TestReport.createRoute(testId)) {
                            popUpTo(Screen.TestsLanding.route)
                        }
                    },
                    onTestAborted = {
                        navController.popBackStack(Screen.TestsLanding.route, inclusive = false)
                    }
                )
            }
        }

        // Tests Module Destination 4: Completed Test Report Screen
        composable(
            route = Screen.TestReport.route,
            arguments = listOf(
                navArgument("testId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedId = backStackEntry.arguments?.getString("testId").orEmpty()
            val testId = RouteUtils.decode(encodedId)

            if (testViewModel != null) {
                TestReportScreen(
                    testId = testId,
                    viewModel = testViewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // Tests Module Destination 5: Past Tests History Screen
        composable(Screen.PastTests.route) {
            if (testViewModel != null) {
                PastTestsScreen(
                    viewModel = testViewModel,
                    onSelectTest = { testId ->
                        navController.navigate(Screen.TestReport.createRoute(testId))
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
