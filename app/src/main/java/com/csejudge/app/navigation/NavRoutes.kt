package com.csejudge.app.navigation

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Utility for safe URL encoding and decoding of route arguments.
 * Compatible with pure JVM unit tests and Android runtime.
 */
object RouteUtils {
    fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.toString())
}

/**
 * Type-safe navigation routes for CSE Judge.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object PracticeFilter : Screen("practice_filter")
    data object PracticeQuestion : Screen("practice_question")

    // Bookmarked Questions Navigation Hierarchy
    data object BookmarkSubjects : Screen("bookmark_subjects")

    data object BookmarkMicrothemes : Screen("bookmark_microthemes/{subject}") {
        fun createRoute(subject: String): String = "bookmark_microthemes/${RouteUtils.encode(subject)}"
    }

    data object BookmarkQuestions : Screen("bookmark_questions/{subject}/{microtheme}") {
        fun createRoute(subject: String, microtheme: String): String =
            "bookmark_questions/${RouteUtils.encode(subject)}/${RouteUtils.encode(microtheme)}"
    }

    // Performance Analysis Dashboard
    data object Analysis : Screen("analysis")
}
