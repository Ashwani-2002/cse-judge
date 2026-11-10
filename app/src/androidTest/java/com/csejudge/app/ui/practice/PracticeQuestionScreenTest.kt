package com.csejudge.app.ui.practice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PracticeQuestionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createSampleQuestion(
        id: String = "POL00001",
        correctAnswer: String = "C"
    ): Question {
        return Question(
            questionId = id,
            subject = "Polity",
            microtheme = "Historical Background",
            question = listOf(
                "Consider the following statements regarding Morley-Minto Reforms:",
                "1. Statement one about imperial council.",
                "2. Statement two about separate electorates.",
                "Which one of the following statements is correct?"
            ),
            options = Options(
                a = "Statement 1 only is correct",
                b = "Statement 2 only is correct",
                c = "Both Statement 1 and Statement 2 are correct",
                d = "Neither Statement 1 nor Statement 2 is correct"
            ),
            correctAnswer = correctAnswer,
            solution = listOf(
                "Correct Option: (c)",
                "Explanation:",
                "Both statements are factually correct under the Act of 1909.",
                "Supplementary detail line for the solution."
            )
        )
    }

    @Test
    fun testQuestionStatementsRenderedSeparatelyInOrder() {
        val question = createSampleQuestion()
        val uiState = PracticeUiState(
            currentQuestion = question,
            isAnswered = false,
            isSkipEnabled = true
        )

        composeTestRule.setContent {
            PracticeQuestionContent(
                uiState = uiState,
                onSubmitAnswer = {},
                onSkip = {},
                onNext = {},
                onBackToFilter = {},
                onClearError = {}
            )
        }

        // Each statement is rendered on a separate node in order
        composeTestRule.onNodeWithTag("QuestionStatement_0")
            .assertIsDisplayed()
            .assertTextContains("Morley-Minto Reforms", substring = true)

        composeTestRule.onNodeWithTag("QuestionStatement_1")
            .assertIsDisplayed()
            .assertTextContains("1. Statement one", substring = true)

        composeTestRule.onNodeWithTag("QuestionStatement_2")
            .assertIsDisplayed()
            .assertTextContains("2. Statement two", substring = true)

        composeTestRule.onNodeWithTag("QuestionStatement_3")
            .assertIsDisplayed()
            .assertTextContains("Which one of the following", substring = true)
    }

    @Test
    fun testFourOptionsRenderedInOrderAndSelectableBeforeAnswering() {
        var submittedAnswer: String? = null
        val question = createSampleQuestion()
        val uiState = PracticeUiState(
            currentQuestion = question,
            isAnswered = false,
            isSkipEnabled = true
        )

        composeTestRule.setContent {
            PracticeQuestionContent(
                uiState = uiState,
                onSubmitAnswer = { submittedAnswer = it },
                onSkip = {},
                onNext = {},
                onBackToFilter = {},
                onClearError = {}
            )
        }

        // Four options exist and are displayed in A, B, C, D order
        composeTestRule.onNodeWithTag("OptionCard_A").assertIsDisplayed()
        composeTestRule.onNodeWithTag("OptionCard_B").assertIsDisplayed()
        composeTestRule.onNodeWithTag("OptionCard_C").assertIsDisplayed()
        composeTestRule.onNodeWithTag("OptionCard_D").assertIsDisplayed()

        // Clicking option C submits answer
        composeTestRule.onNodeWithTag("OptionCard_C").performClick()
        assertEquals("C", submittedAnswer)
    }

    @Test
    fun testSkipVisibleBeforeAnsweringAndNextVisibleAfterAnswering() {
        var skipClicked = false
        var nextClicked = false
        val question = createSampleQuestion()

        // State 1: Before answering
        val beforeAnswerState = PracticeUiState(
            currentQuestion = question,
            isAnswered = false,
            isSkipEnabled = true,
            isNextEnabled = false
        )

        composeTestRule.setContent {
            PracticeQuestionContent(
                uiState = beforeAnswerState,
                onSubmitAnswer = {},
                onSkip = { skipClicked = true },
                onNext = { nextClicked = true },
                onBackToFilter = {},
                onClearError = {}
            )
        }

        // Skip is displayed, Next does not exist
        composeTestRule.onNodeWithTag("SkipButton").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithTag("NextButton").assertDoesNotExist()

        composeTestRule.onNodeWithTag("SkipButton").performClick()
        assertTrue(skipClicked)
    }

    @Test
    fun testNextButtonAppearsAfterAnswering() {
        var nextClicked = false
        val question = createSampleQuestion(correctAnswer = "C")

        // State 2: After answering
        val afterAnswerState = PracticeUiState(
            currentQuestion = question,
            isAnswered = true,
            selectedAnswer = "C",
            isAnswerCorrect = true,
            correctAnswer = "C",
            isSkipEnabled = false,
            isNextEnabled = true
        )

        composeTestRule.setContent {
            PracticeQuestionContent(
                uiState = afterAnswerState,
                onSubmitAnswer = {},
                onSkip = {},
                onNext = { nextClicked = true },
                onBackToFilter = {},
                onClearError = {}
            )
        }

        // Next is displayed, Skip does not exist
        composeTestRule.onNodeWithTag("NextButton").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithTag("SkipButton").assertDoesNotExist()

        composeTestRule.onNodeWithTag("NextButton").performClick()
        assertTrue(nextClicked)
    }

    @Test
    fun testSolutionHiddenBeforeAnswering() {
        val question = createSampleQuestion()

        composeTestRule.setContent {
            PracticeQuestionContent(
                uiState = PracticeUiState(
                    currentQuestion = question,
                    isAnswered = false
                ),
                onSubmitAnswer = {},
                onSkip = {},
                onNext = {},
                onBackToFilter = {},
                onClearError = {}
            )
        }
        composeTestRule.onNodeWithTag("SolutionSection").assertDoesNotExist()
    }

    @Test
    fun testSolutionVisibleAfterAnswering() {
        val question = createSampleQuestion()

        composeTestRule.setContent {
            PracticeQuestionContent(
                uiState = PracticeUiState(
                    currentQuestion = question,
                    isAnswered = true,
                    selectedAnswer = "C",
                    isAnswerCorrect = true,
                    correctAnswer = "C"
                ),
                onSubmitAnswer = {},
                onSkip = {},
                onNext = {},
                onBackToFilter = {},
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithTag("SolutionSection").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("SolutionLine_0").assertExists().assertTextContains("Correct Option", substring = true)
        composeTestRule.onNodeWithTag("SolutionLine_1").assertExists().assertTextContains("Explanation:", substring = true)
        composeTestRule.onNodeWithTag("SolutionLine_2").assertExists()
    }

    @Test
    fun testExhaustedStateRendersCorrectly() {
        var backToFilterClicked = false
        val exhaustedState = PracticeUiState(
            isExhausted = true,
            currentQuestion = null,
            sessionStats = PracticeSessionStats(answeredCount = 10, correctCount = 8, incorrectCount = 2, skippedCount = 3)
        )

        composeTestRule.setContent {
            PracticeQuestionContent(
                uiState = exhaustedState,
                onSubmitAnswer = {},
                onSkip = {},
                onNext = {},
                onBackToFilter = { backToFilterClicked = true },
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithTag("ExhaustedStateView").assertIsDisplayed()
        composeTestRule.onNodeWithText("Practice Complete").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ChooseAnotherSubjectButton").assertIsDisplayed().performClick()

        assertTrue(backToFilterClicked)
    }

    @Test
    fun testOptionsDisabledAfterAnswering() {
        var clickedAfter = false
        val question = createSampleQuestion(correctAnswer = "C")

        val uiState = PracticeUiState(
            currentQuestion = question,
            isAnswered = true,
            selectedAnswer = "B",
            isAnswerCorrect = false,
            correctAnswer = "C"
        )

        composeTestRule.setContent {
            PracticeQuestionContent(
                uiState = uiState,
                onSubmitAnswer = { clickedAfter = true },
                onSkip = {},
                onNext = {},
                onBackToFilter = {},
                onClearError = {}
            )
        }

        // Tapping option when answered does nothing
        composeTestRule.onNodeWithTag("OptionCard_A").performClick()
        assertEquals(false, clickedAfter)
    }
}
