package com.csejudge.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.csejudge.app.data.local.AttemptedQuestionEntity
import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.repository.PracticeHistoryRepository
import com.csejudge.app.data.repository.QuestionBankRepository
import com.csejudge.app.data.repository.QuestionBankStats
import com.csejudge.app.ui.practice.PracticeViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController
    private lateinit var fakeQuestionBankRepo: FakeQuestionBankRepository
    private lateinit var fakeHistoryRepo: FakePracticeHistoryRepository
    private lateinit var practiceViewModel: PracticeViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        navController = TestNavHostController(context)
        navController.navigatorProvider.addNavigator(ComposeNavigator())

        fakeQuestionBankRepo = FakeQuestionBankRepository()
        fakeHistoryRepo = FakePracticeHistoryRepository()

        practiceViewModel = PracticeViewModel(
            questionBankRepository = fakeQuestionBankRepo,
            practiceHistoryRepository = fakeHistoryRepo
        )
    }

    @Test
    fun testHomeScreenRendersWithPracticeProblemsTile() {
        composeTestRule.setContent {
            AppNavHost(
                practiceViewModel = practiceViewModel,
                navController = navController
            )
        }

        // Verify Home screen header
        composeTestRule.onNodeWithText("CSE Judge").assertIsDisplayed()
        composeTestRule.onNodeWithText("UPSC Civil Services Prelims Preparation").assertIsDisplayed()

        // Verify Practice problems tile
        composeTestRule.onNodeWithTag("PracticeProblemsTile").assertIsDisplayed()
        composeTestRule.onNodeWithText("Practice problems").assertIsDisplayed()
    }

    @Test
    fun testTappingTileNavigatesToPracticeFilterScreen() {
        composeTestRule.setContent {
            AppNavHost(
                practiceViewModel = practiceViewModel,
                navController = navController
            )
        }

        assertEquals(Screen.Home.route, navController.currentBackStackEntry?.destination?.route)

        // Tap Practice problems tile
        composeTestRule.onNodeWithTag("PracticeProblemsTile").performClick()

        // Verify navigation to filter screen
        assertEquals(Screen.PracticeFilter.route, navController.currentBackStackEntry?.destination?.route)
        composeTestRule.onNodeWithText("Practice Questions").assertIsDisplayed()
        composeTestRule.onNodeWithTag("RandomModeSwitch").assertIsDisplayed().assertIsOn()
    }

    @Test
    fun testCompleteNavigationFlowHomeToFilterToQuestionAndBack() {
        composeTestRule.setContent {
            AppNavHost(
                practiceViewModel = practiceViewModel,
                navController = navController
            )
        }

        // 1. Home Screen -> Tap tile
        composeTestRule.onNodeWithTag("PracticeProblemsTile").performClick()
        assertEquals(Screen.PracticeFilter.route, navController.currentBackStackEntry?.destination?.route)

        // 2. Filter Screen -> Start Practice
        composeTestRule.onNodeWithTag("StartPracticeButton").assertIsEnabled().performClick()

        // 3. Question Screen
        assertEquals(Screen.PracticeQuestion.route, navController.currentBackStackEntry?.destination?.route)
        composeTestRule.onNodeWithTag("QuestionStatementsSection").assertIsDisplayed()
        composeTestRule.onNodeWithTag("OptionCard_A").assertIsDisplayed()
        composeTestRule.onNodeWithTag("SkipButton").assertIsDisplayed()

        // 4. Back from Question Screen -> Returns to Filter Screen
        composeTestRule.onNodeWithTag("BackToFilterButton").performClick()
        assertEquals(Screen.PracticeFilter.route, navController.currentBackStackEntry?.destination?.route)
        composeTestRule.onNodeWithText("Practice Questions").assertIsDisplayed()

        // 5. Back from Filter Screen -> Returns to Home Screen
        composeTestRule.onNodeWithTag("FilterBackButton").performClick()
        assertEquals(Screen.Home.route, navController.currentBackStackEntry?.destination?.route)
        composeTestRule.onNodeWithText("CSE Judge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("PracticeProblemsTile").assertIsDisplayed()
    }

    @Test
    fun testSameViewModelAndSessionRetainedAcrossNavigation() {
        composeTestRule.setContent {
            AppNavHost(
                practiceViewModel = practiceViewModel,
                navController = navController
            )
        }

        // Navigate to filter screen
        composeTestRule.onNodeWithTag("PracticeProblemsTile").performClick()

        // Start practice
        composeTestRule.onNodeWithTag("StartPracticeButton").performClick()

        val initialQuestionId = practiceViewModel.uiState.value.currentQuestion?.questionId

        // Skip question in session
        composeTestRule.onNodeWithTag("SkipButton").performClick()
        val skippedCount = practiceViewModel.uiState.value.sessionStats.skippedCount
        assertEquals(1, skippedCount)

        // Return back to filter screen
        composeTestRule.onNodeWithTag("BackToFilterButton").performClick()
        assertEquals(Screen.PracticeFilter.route, navController.currentBackStackEntry?.destination?.route)

        // Verify session stats are retained in the same ViewModel instance
        assertEquals(1, practiceViewModel.uiState.value.sessionStats.skippedCount)

        // Re-enter question screen without resetting filter
        composeTestRule.onNodeWithTag("StartPracticeButton").performClick()
        assertEquals(Screen.PracticeQuestion.route, navController.currentBackStackEntry?.destination?.route)
        assertEquals(1, practiceViewModel.uiState.value.sessionStats.skippedCount)
    }

    @Test
    fun testSubjectModeNavigationFlow() {
        composeTestRule.setContent {
            AppNavHost(
                practiceViewModel = practiceViewModel,
                navController = navController
            )
        }

        // Navigate to filter screen
        composeTestRule.onNodeWithTag("PracticeProblemsTile").performClick()
        assertEquals(Screen.PracticeFilter.route, navController.currentBackStackEntry?.destination?.route)

        // Turn off Random mode
        composeTestRule.onNodeWithTag("RandomModeSwitch").performClick()

        // Open subject dropdown and select Polity
        composeTestRule.onNodeWithTag("SubjectDropdown").performClick()
        composeTestRule.onNodeWithTag("SubjectItem_Polity").performClick()

        // Start practice
        composeTestRule.onNodeWithTag("StartPracticeButton").performClick()
        assertEquals(Screen.PracticeQuestion.route, navController.currentBackStackEntry?.destination?.route)

        // Verify current question is Polity
        assertEquals("Polity", practiceViewModel.uiState.value.currentQuestion?.subject)

        // Back to filter
        composeTestRule.onNodeWithTag("BackToFilterButton").performClick()
        assertEquals(Screen.PracticeFilter.route, navController.currentBackStackEntry?.destination?.route)

        // Verify subject Polity remains selected
        assertEquals("Polity", practiceViewModel.uiState.value.selectedSubject)

        // Back to home
        composeTestRule.onNodeWithTag("FilterBackButton").performClick()
        assertEquals(Screen.Home.route, navController.currentBackStackEntry?.destination?.route)
    }

    // ==========================================
    // Test Fakes
    // ==========================================

    private class FakeQuestionBankRepository : QuestionBankRepository {
        override var isLoaded: Boolean = true

        private val allQuestions = listOf(
            createTestQuestion("POL00001", "Polity", "Preamble", "A"),
            createTestQuestion("POL00002", "Polity", "Preamble", "B"),
            createTestQuestion("ECO00001", "Economy", "Banking", "C"),
            createTestQuestion("MH00001", "Modern History", "From 1857 to 1884 CE", "D")
        )

        private fun createTestQuestion(id: String, subject: String, microtheme: String, correct: String): Question {
            return Question(
                questionId = id,
                subject = subject,
                microtheme = microtheme,
                question = listOf("Statement 1 for $id", "Statement 2 for $id"),
                options = Options("Option A", "Option B", "Option C", "Option D"),
                correctAnswer = correct,
                solution = listOf("Solution line 1 for $id", "Solution line 2 for $id")
            )
        }

        override suspend fun loadQuestionBank(): QuestionBankStats {
            isLoaded = true
            return QuestionBankStats(allQuestions.size, emptyMap(), emptyMap(), 0)
        }

        override fun getAllQuestions(): List<Question> = allQuestions
        override fun getQuestionById(questionId: String): Question? = allQuestions.find { it.questionId == questionId }
        override fun getAvailableSubjects(): List<String> = allQuestions.map { it.subject }.distinct()
        override fun getAvailableMicrothemes(subject: String): List<String> =
            allQuestions.filter { it.subject == subject }.map { it.microtheme }.distinct()
        override fun getQuestionsForSubject(subject: String): List<Question> =
            allQuestions.filter { it.subject == subject }
        override fun getQuestionsForSubjectAndMicrotheme(subject: String, microtheme: String): List<Question> =
            allQuestions.filter { it.subject == subject && it.microtheme == microtheme }
    }

    private class FakePracticeHistoryRepository : PracticeHistoryRepository {
        val attempts = mutableMapOf<String, AttemptedQuestionEntity>()
        val attemptedIds = mutableSetOf<String>()

        override suspend fun recordAttempt(
            questionId: String,
            selectedAnswer: String,
            isCorrect: Boolean
        ): Result<Unit> {
            val entity = AttemptedQuestionEntity(
                questionId = questionId,
                selectedAnswer = selectedAnswer,
                isCorrect = isCorrect,
                attemptedAt = System.currentTimeMillis()
            )
            attempts[questionId] = entity
            attemptedIds.add(questionId)
            return Result.success(Unit)
        }

        override suspend fun isQuestionAttempted(questionId: String): Boolean = attemptedIds.contains(questionId)
        override suspend fun getAllAttemptedQuestionIds(): List<String> = attemptedIds.toList()
        override fun observeAllAttemptedQuestionIds(): Flow<List<String>> = flowOf(attemptedIds.toList())
        override suspend fun getAttempt(questionId: String): AttemptedQuestionEntity? = attempts[questionId]
        override suspend fun getAttemptCount(): Int = attempts.size
        override fun observeAttemptCount(): Flow<Int> = flowOf(attempts.size)
    }
}
