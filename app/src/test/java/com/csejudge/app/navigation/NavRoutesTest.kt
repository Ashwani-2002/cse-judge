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

        assertEquals("home", homeRoute)
        assertEquals("practice_filter", filterRoute)
        assertEquals("practice_question", questionRoute)

        assertTrue(homeRoute.isNotBlank())
        assertTrue(filterRoute.isNotBlank())
        assertTrue(questionRoute.isNotBlank())

        assertNotEquals(homeRoute, filterRoute)
        assertNotEquals(filterRoute, questionRoute)
        assertNotEquals(homeRoute, questionRoute)
    }
}
