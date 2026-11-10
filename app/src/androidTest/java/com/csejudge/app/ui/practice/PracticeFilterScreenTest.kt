package com.csejudge.app.ui.practice

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PracticeFilterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testRandomToggleHidesSubjectAndMicrothemeDropdowns() {
        val uiState = PracticeUiState(
            isRandomMode = true,
            availableSubjects = listOf("Polity", "Economy"),
            selectedSubject = null
        )

        composeTestRule.setContent {
            PracticeFilterContent(
                uiState = uiState,
                onToggleRandom = {},
                onSelectSubject = {},
                onSelectMicrotheme = {},
                onStartPractice = {},
                onClearError = {}
            )
        }

        // Random mode switch is ON
        composeTestRule.onNodeWithTag("RandomModeSwitch").assertIsOn()

        // Dropdowns do NOT exist when Random is enabled
        composeTestRule.onNodeWithTag("SubjectDropdown").assertDoesNotExist()
        composeTestRule.onNodeWithTag("MicrothemeDropdown").assertDoesNotExist()

        // Start Practice button is enabled in Random mode
        composeTestRule.onNodeWithTag("StartPracticeButton").assertIsEnabled()
    }

    @Test
    fun testSubjectDropdownAppearsWhenRandomDisabled() {
        var toggledValue: Boolean? = null
        val uiState = PracticeUiState(
            isRandomMode = false,
            availableSubjects = listOf("Polity", "Economy", "Geography"),
            selectedSubject = null
        )

        composeTestRule.setContent {
            PracticeFilterContent(
                uiState = uiState,
                onToggleRandom = { toggledValue = it },
                onSelectSubject = {},
                onSelectMicrotheme = {},
                onStartPractice = {},
                onClearError = {}
            )
        }

        // Random switch is OFF
        composeTestRule.onNodeWithTag("RandomModeSwitch").assertIsOff()

        // Subject dropdown is visible
        composeTestRule.onNodeWithTag("SubjectDropdown").assertIsDisplayed()

        // Microtheme dropdown should NOT exist before subject selection
        composeTestRule.onNodeWithTag("MicrothemeDropdown").assertDoesNotExist()

        // Start Practice button is DISABLED because no subject is selected
        composeTestRule.onNodeWithTag("StartPracticeButton").assertIsNotEnabled()

        // Clicking toggle triggers callback
        composeTestRule.onNodeWithTag("RandomModeSwitch").performClick()
        assertEquals(true, toggledValue)
    }

    @Test
    fun testMicrothemeDropdownVisibleAfterSubjectSelection() {
        val uiState = PracticeUiState(
            isRandomMode = false,
            availableSubjects = listOf("Polity", "Economy"),
            selectedSubject = "Polity",
            availableMicrothemes = listOf("Preamble", "Fundamental Rights"),
            selectedMicrotheme = null
        )

        composeTestRule.setContent {
            PracticeFilterContent(
                uiState = uiState,
                onToggleRandom = {},
                onSelectSubject = {},
                onSelectMicrotheme = {},
                onStartPractice = {},
                onClearError = {}
            )
        }

        // Subject and Microtheme dropdowns are visible
        composeTestRule.onNodeWithTag("SubjectDropdown").assertIsDisplayed()
        composeTestRule.onNodeWithTag("MicrothemeDropdown").assertIsDisplayed()

        // With subject selected, Start Practice is enabled
        composeTestRule.onNodeWithTag("StartPracticeButton").assertIsEnabled()
    }

    @Test
    fun testSelectSubjectFromDropdown() {
        var selectedSubject: String? = null
        val uiState = PracticeUiState(
            isRandomMode = false,
            availableSubjects = listOf("Polity", "Economy", "Modern History"),
            selectedSubject = null
        )

        composeTestRule.setContent {
            PracticeFilterContent(
                uiState = uiState,
                onToggleRandom = {},
                onSelectSubject = { selectedSubject = it },
                onSelectMicrotheme = {},
                onStartPractice = {},
                onClearError = {}
            )
        }

        // Open dropdown
        composeTestRule.onNodeWithTag("SubjectDropdown").performClick()

        // Click "Economy" item
        composeTestRule.onNodeWithTag("SubjectItem_Economy").performClick()

        assertEquals("Economy", selectedSubject)
    }

    @Test
    fun testSelectAllMicrothemesOption() {
        var selectedMicrotheme: String? = "Initial"
        var wasCalled = false
        val uiState = PracticeUiState(
            isRandomMode = false,
            availableSubjects = listOf("Polity"),
            selectedSubject = "Polity",
            availableMicrothemes = listOf("Preamble", "Judiciary"),
            selectedMicrotheme = "Preamble"
        )

        composeTestRule.setContent {
            PracticeFilterContent(
                uiState = uiState,
                onToggleRandom = {},
                onSelectSubject = {},
                onSelectMicrotheme = {
                    wasCalled = true
                    selectedMicrotheme = it
                },
                onStartPractice = {},
                onClearError = {}
            )
        }

        // Open microtheme dropdown
        composeTestRule.onNodeWithTag("MicrothemeDropdown").performClick()

        // Click "All microthemes"
        composeTestRule.onNodeWithTag("MicrothemeItem_All").performClick()

        assertTrue(wasCalled)
        assertNull("All microthemes selection should pass null to selectMicrotheme", selectedMicrotheme)
    }

    @Test
    fun testStartPracticeButtonCallsCallbackWhenEnabled() {
        var started = false
        val uiState = PracticeUiState(
            isRandomMode = true,
            availableSubjects = listOf("Polity")
        )

        composeTestRule.setContent {
            PracticeFilterContent(
                uiState = uiState,
                onToggleRandom = {},
                onSelectSubject = {},
                onSelectMicrotheme = {},
                onStartPractice = { started = true },
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithTag("StartPracticeButton").performClick()
        assertTrue(started)
    }
}
