package com.csejudge.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavRoutesTest {

    @Test
    fun testScreenRoutesAreDefinedAndDistinct() {
        val homeRoute = Screen.Home.route
        val filterRoute = Screen.PracticeFilter.route
        val questionRoute = Screen.PracticeQuestion.route
        val bookmarkSubjectsRoute = Screen.BookmarkSubjects.route
        val bookmarkMicrothemesRoute = Screen.BookmarkMicrothemes.route
        val bookmarkQuestionsRoute = Screen.BookmarkQuestions.route
        val analysisRoute = Screen.Analysis.route

        assertEquals("home", homeRoute)
        assertEquals("practice_filter", filterRoute)
        assertEquals("practice_question", questionRoute)
        assertEquals("bookmark_subjects", bookmarkSubjectsRoute)
        assertEquals("bookmark_microthemes/{subject}", bookmarkMicrothemesRoute)
        assertEquals("bookmark_questions/{subject}/{microtheme}", bookmarkQuestionsRoute)
        assertEquals("analysis", analysisRoute)

        val routes = listOf(
            homeRoute,
            filterRoute,
            questionRoute,
            bookmarkSubjectsRoute,
            bookmarkMicrothemesRoute,
            bookmarkQuestionsRoute,
            analysisRoute
        )

        routes.forEach { assertTrue(it.isNotBlank()) }
        assertEquals(routes.size, routes.distinct().size)
    }

    @Test
    fun testBookmarkRouteCreationAndEncoding() {
        val subject = "Modern History"
        val microtheme = "From 1857 to 1884 CE"

        val microthemesRoute = Screen.BookmarkMicrothemes.createRoute(subject)
        val questionsRoute = Screen.BookmarkQuestions.createRoute(subject, microtheme)

        assertTrue(microthemesRoute.startsWith("bookmark_microthemes/"))
        assertTrue(questionsRoute.startsWith("bookmark_questions/"))

        // Verify URL encoding and decoding preserves original strings
        val encodedSubject = RouteUtils.encode(subject)
        val encodedMicrotheme = RouteUtils.encode(microtheme)

        assertEquals("bookmark_microthemes/$encodedSubject", microthemesRoute)
        assertEquals("bookmark_questions/$encodedSubject/$encodedMicrotheme", questionsRoute)

        assertEquals(subject, RouteUtils.decode(encodedSubject))
        assertEquals(microtheme, RouteUtils.decode(encodedMicrotheme))
    }
}
