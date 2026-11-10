package com.csejudge.app.ui.home

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testHomeScreenDisplaysHeaderAndBranding() {
        composeTestRule.setContent {
            HomeScreen(onNavigateToPractice = {})
        }

        composeTestRule.onNodeWithText("CSE Judge").assertIsDisplayed()
        composeTestRule.onNodeWithText("UPSC Civil Services Prelims Preparation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Study Modules").assertIsDisplayed()
    }

    @Test
    fun testPracticeProblemsTileIsProminentAndClickable() {
        var clicked = false

        composeTestRule.setContent {
            HomeScreen(onNavigateToPractice = { clicked = true })
        }

        // Verify tile content
        composeTestRule.onNodeWithTag("PracticeProblemsTile")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule.onNodeWithText("Practice problems").assertIsDisplayed()
        composeTestRule.onNodeWithText("Practice prelims questions by topic, microtheme, or across all 7 subjects at random.")
            .assertIsDisplayed()

        // Verify clicking the entire tile triggers callback
        composeTestRule.onNodeWithTag("PracticeProblemsTile").performClick()
        assertTrue(clicked)
    }
}
