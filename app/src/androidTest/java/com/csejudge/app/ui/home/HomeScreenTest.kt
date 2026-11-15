package com.csejudge.app.ui.home

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
    fun testThemeToggleButtonShowsMoonIconInLightModeAndInvokesCallback() {
        var toggleClicked = false

        composeTestRule.setContent {
            HomeScreen(
                onNavigateToPractice = {},
                isDarkMode = false,
                onToggleTheme = { toggleClicked = true }
            )
        }

        val toggleButton = composeTestRule.onNodeWithTag("ThemeToggleButton")
        toggleButton.assertIsDisplayed()
        toggleButton.assertHasClickAction()
        toggleButton.assertContentDescriptionEquals("Switch to dark mode")

        composeTestRule.onNodeWithTag("MoonIconCanvas", useUnmergedTree = true).assertIsDisplayed()

        toggleButton.performClick()
        assertTrue(toggleClicked)
    }

    @Test
    fun testThemeToggleButtonShowsSunIconInDarkModeAndInvokesCallback() {
        var toggleClicked = false

        composeTestRule.setContent {
            HomeScreen(
                onNavigateToPractice = {},
                isDarkMode = true,
                onToggleTheme = { toggleClicked = true }
            )
        }

        val toggleButton = composeTestRule.onNodeWithTag("ThemeToggleButton")
        toggleButton.assertIsDisplayed()
        toggleButton.assertHasClickAction()
        toggleButton.assertContentDescriptionEquals("Switch to light mode")

        composeTestRule.onNodeWithTag("SunIconCanvas", useUnmergedTree = true).assertIsDisplayed()

        toggleButton.performClick()
        assertTrue(toggleClicked)
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
        composeTestRule.onNodeWithText("Practice prelims questions by topic, microtheme, or across all subjects at random.")
            .assertIsDisplayed()

        // Verify clicking the entire tile triggers callback
        composeTestRule.onNodeWithTag("PracticeProblemsTile").performClick()
        assertTrue(clicked)
    }

    @Test
    fun testBookmarkedQuestionsTileDisplaysEmptyStateWhenCountIsZero() {
        var bookmarkClicked = false

        composeTestRule.setContent {
            HomeScreen(
                onNavigateToPractice = {},
                onNavigateToBookmarks = { bookmarkClicked = true },
                bookmarkCount = 0
            )
        }

        // Verify tile presence
        composeTestRule.onNodeWithTag("BookmarkedQuestionsTile")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule.onNodeWithText("Bookmarked Questions").assertIsDisplayed()
        composeTestRule.onNodeWithText("No bookmarked questions yet. Tap the star button while practicing to bookmark questions for revision.")
            .assertIsDisplayed()

        // Click tile
        composeTestRule.onNodeWithTag("BookmarkedQuestionsTile").performClick()
        assertTrue(bookmarkClicked)
    }

    @Test
    fun testBookmarkedQuestionsTileDisplaysCountWhenGreaterThanZero() {
        composeTestRule.setContent {
            HomeScreen(
                onNavigateToPractice = {},
                onNavigateToBookmarks = {},
                bookmarkCount = 5
            )
        }

        composeTestRule.onNodeWithTag("BookmarkedQuestionsTile").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("BookmarkCountBadge", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("5 questions saved", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Review bookmarked questions organized by subject and microtheme.")
            .assertIsDisplayed()
    }

    @Test
    fun testAnalysisTileIsDisplayedAndClickable() {
        var analysisClicked = false

        composeTestRule.setContent {
            HomeScreen(
                onNavigateToPractice = {},
                onNavigateToBookmarks = {},
                onNavigateToAnalysis = { analysisClicked = true }
            )
        }

        val analysisTile = composeTestRule.onNodeWithTag("AnalysisTile")
        analysisTile.performScrollTo().assertIsDisplayed()
        analysisTile.assertHasClickAction()

        composeTestRule.onNodeWithText("Analysis").assertIsDisplayed()
        composeTestRule.onNodeWithText("Track your preparation and accuracy").assertIsDisplayed()

        analysisTile.performClick()
        assertTrue(analysisClicked)
    }

    @Test
    fun testTestsTileIsDisplayedAtPositionTwoAndClickable() {
        var testsClicked = false

        composeTestRule.setContent {
            HomeScreen(
                onNavigateToPractice = {},
                onNavigateToTests = { testsClicked = true },
                onNavigateToBookmarks = {},
                onNavigateToAnalysis = {}
            )
        }

        val testsTile = composeTestRule.onNodeWithTag("TestsTile")
        testsTile.assertIsDisplayed()
        testsTile.assertHasClickAction()

        composeTestRule.onNodeWithText("Tests").assertIsDisplayed()
        composeTestRule.onNodeWithText("Take timed Full Length (100 Qs) or Half Length (50 Qs) tests and review past reports.")
            .assertIsDisplayed()

        testsTile.performClick()
        assertTrue(testsClicked)
    }
}
