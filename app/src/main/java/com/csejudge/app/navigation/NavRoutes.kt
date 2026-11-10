package com.csejudge.app.navigation

/**
 * Type-safe navigation routes for CSE Judge.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object PracticeFilter : Screen("practice_filter")
    data object PracticeQuestion : Screen("practice_question")
}
