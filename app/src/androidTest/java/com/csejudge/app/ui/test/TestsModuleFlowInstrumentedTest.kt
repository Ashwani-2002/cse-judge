package com.csejudge.app.ui.test

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.csejudge.app.data.local.AppDatabase
import com.csejudge.app.data.local.AttemptedQuestionEntity
import com.csejudge.app.data.local.TestEntity
import com.csejudge.app.data.local.TestQuestionEntity
import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.model.TestReport
import com.csejudge.app.data.model.TestType
import com.csejudge.app.data.repository.BookmarkRepository
import com.csejudge.app.data.repository.PracticeHistoryRepository
import com.csejudge.app.data.repository.QuestionBankRepository
import com.csejudge.app.data.repository.QuestionBankStats
import com.csejudge.app.data.repository.TestRepository
import com.csejudge.app.navigation.AppNavHost
import com.csejudge.app.navigation.Screen
import com.csejudge.app.ui.practice.CSEJudgeTheme
import com.csejudge.app.ui.practice.PracticeViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestsModuleFlowInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController
    private lateinit var fakeQuestionBank: MockQuestionBankRepo
    private lateinit var fakeHistory: MockPracticeHistoryRepo
    private lateinit var fakeTestRepo: MockTestRepo

    private lateinit var practiceViewModel: PracticeViewModel
    private lateinit var testViewModel: TestViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        navController = TestNavHostController(context)
        navController.navigatorProvider.addNavigator(ComposeNavigator())

        fakeQuestionBank = MockQuestionBankRepo()
        fakeHistory = MockPracticeHistoryRepo()
        fakeTestRepo = MockTestRepo()

        // Generate 140 questions (20 per subject across 7 subjects)
        val subjects = listOf("Polity", "Economy", "Geography", "Modern History", "AMAC", "Sci and Tech", "Environment")
        val questions = mutableListOf<Question>()
        for (subj in subjects) {
            for (i in 1..20) {
                questions.add(
                    Question(
                        subject = subj,
                        microtheme = "Theme $i",
                        question = listOf("Statement for Q_${subj}_$i"),
                        options = Options("Option A text", "Option B text", "Option C text", "Option D text"),
                        correctAnswer = "B",
                        solution = listOf("Solution for Q_${subj}_$i"),
                        questionId = "Q_${subj}_$i"
                    )
                )
            }
        }
        fakeQuestionBank.setQuestions(questions)

        practiceViewModel = PracticeViewModel(
            questionBankRepository = fakeQuestionBank,
            practiceHistoryRepository = fakeHistory
        )

        testViewModel = TestViewModel(
            questionBankRepository = fakeQuestionBank,
            practiceHistoryRepository = fakeHistory,
            testRepository = fakeTestRepo,
            bookmarkRepository = null
        )
    }

    @Test
    fun testHomeScreenTestsTilePlacementAndNavigationToLanding() {
        composeTestRule.setContent {
            CSEJudgeTheme {
                AppNavHost(
                    practiceViewModel = practiceViewModel,
                    testViewModel = testViewModel,
                    navController = navController
                )
            }
        }

        // Verify Tests Tile is displayed on Home Screen
        val testsTile = composeTestRule.onNodeWithTag("TestsTile")
        testsTile.assertIsDisplayed().assertHasClickAction()
        composeTestRule.onNodeWithText("Tests").assertIsDisplayed()

        // Click Tests Tile
        testsTile.performClick()

        // Verify navigation to Tests landing
        assertEquals(Screen.TestsLanding.route, navController.currentBackStackEntry?.destination?.route)
        composeTestRule.onNodeWithTag("FullLengthTestCard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("HalfLengthTestCard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("PastTestsCard").assertIsDisplayed()
    }

    @Test
    fun testHalfLengthTestInstructionsAndStartFlow() {
        composeTestRule.setContent {
            CSEJudgeTheme {
                AppNavHost(
                    practiceViewModel = practiceViewModel,
                    testViewModel = testViewModel,
                    navController = navController
                )
            }
        }

        // Home -> Tests Landing
        composeTestRule.onNodeWithTag("TestsTile").performClick()

        // Tests Landing -> Half Length Test Instructions
        composeTestRule.onNodeWithTag("HalfLengthTestCard").performClick()

        // Verify Instructions Screen
        composeTestRule.onNodeWithText("Half Length Test").assertIsDisplayed()
        composeTestRule.onNodeWithTag("TestOverviewCard").assertIsDisplayed()
        composeTestRule.onNodeWithText("50").assertIsDisplayed() // Questions
        composeTestRule.onNodeWithText("60 mins").assertIsDisplayed() // Duration

        // Verify Start Test button
        val startButton = composeTestRule.onNodeWithTag("StartTestButton")
        startButton.performScrollTo().assertIsDisplayed()

        // Click Start Test -> Navigates to TestActive
        startButton.performClick()
        assertEquals(Screen.TestActive.route, navController.currentBackStackEntry?.destination?.route)

        // Verify Test Question Screen layout
        composeTestRule.onNodeWithTag("CountdownTimerText").assertIsDisplayed()
        composeTestRule.onNodeWithTag("PaletteHamburgerButton").assertIsDisplayed()
        composeTestRule.onNodeWithTag("QuestionNumberText").assertIsDisplayed()
        composeTestRule.onNodeWithTag("QuestionStatementsSection").assertIsDisplayed()
        composeTestRule.onNodeWithTag("MarkForReviewButton").assertIsDisplayed()
        composeTestRule.onNodeWithTag("TestNextButton").assertIsDisplayed()
        composeTestRule.onNodeWithTag("TestSubmitButton").assertIsDisplayed()

        // Verify Question Palette opening and closing
        composeTestRule.onNodeWithTag("PaletteHamburgerButton").performClick()
        composeTestRule.onNodeWithTag("QuestionPaletteDialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("PaletteCloseButton").performClick()

        // Verify Option Selection
        composeTestRule.onNodeWithTag("TestOptionCard_A").performClick()

        // Verify Mark for Review
        composeTestRule.onNodeWithTag("MarkForReviewButton").performClick()
        composeTestRule.onNodeWithText("Marked for Review").assertIsDisplayed()

        // Verify Submit Confirmation Dialog
        composeTestRule.onNodeWithTag("TestSubmitButton").performClick()
        composeTestRule.onNodeWithTag("SubmitConfirmationDialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("CancelSubmitButton").performClick()
    }

    @Test
    fun testPastTestsEmptyStateAndBackNavigation() {
        composeTestRule.setContent {
            CSEJudgeTheme {
                AppNavHost(
                    practiceViewModel = practiceViewModel,
                    testViewModel = testViewModel,
                    navController = navController
                )
            }
        }

        // Home -> Tests Landing
        composeTestRule.onNodeWithTag("TestsTile").performClick()

        // Tests Landing -> Past Tests
        composeTestRule.onNodeWithTag("PastTestsCard").performScrollTo().performClick()

        // Verify Past Tests Screen and Empty State
        assertEquals(Screen.PastTests.route, navController.currentBackStackEntry?.destination?.route)
        composeTestRule.onNodeWithTag("PastTestsEmptyState").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Completed Tests Yet").assertIsDisplayed()

        // Back button returns to Tests Landing
        composeTestRule.onNodeWithTag("PastTestsBackButton").performClick()
        assertEquals(Screen.TestsLanding.route, navController.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun testAutoSubmissionAtTimerExpiryNavigatesToReportAndRecordsResults() {
        composeTestRule.setContent {
            CSEJudgeTheme {
                AppNavHost(
                    practiceViewModel = practiceViewModel,
                    testViewModel = testViewModel,
                    navController = navController
                )
            }
        }

        // Home -> Tests Landing
        composeTestRule.onNodeWithTag("TestsTile").performClick()

        // Tests Landing -> Half Length Test Instructions
        composeTestRule.onNodeWithTag("HalfLengthTestCard").performClick()

        // Start Test -> Navigates to TestActive
        composeTestRule.onNodeWithTag("StartTestButton").performScrollTo().performClick()
        assertEquals(Screen.TestActive.route, navController.currentBackStackEntry?.destination?.route)

        // Select an option for Q0
        composeTestRule.onNodeWithTag("TestOptionCard_A").performClick()

        // Fast-forward remaining seconds to 1 to simulate immediate expiry
        testViewModel.setRemainingSecondsForTesting(1)

        // Wait until auto-submission completes
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            testViewModel.uiState.value.isCompleted
        }

        // Verify navigation automatically transitioned to TestReport screen
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            navController.currentBackStackEntry?.destination?.route?.startsWith("test_report") == true
        }

        // Verify report screen shows Overall summary
        composeTestRule.onNodeWithTag("ScoreHighlightCard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total Marks").assertIsDisplayed()
        composeTestRule.onNodeWithText("Accuracy").assertIsDisplayed()

        // Verify saved test in repository
        val savedTest = fakeTestRepo.savedTest
        assertNotNull(savedTest)
        assertEquals(50, savedTest!!.totalQuestions)
        assertEquals(1, savedTest.attemptedCount)
        assertEquals(49, savedTest.unansweredCount)
    }
}

// ------------------- Instrumented Fakes -------------------

class MockQuestionBankRepo : QuestionBankRepository {
    override var isLoaded: Boolean = true
    private var questions: List<Question> = emptyList()

    fun setQuestions(list: List<Question>) { questions = list }
    override suspend fun loadQuestionBank(): QuestionBankStats =
        QuestionBankStats(questions.size, emptyMap<String, Int>(), emptyMap<String, Map<String, Int>>(), 0L)
    override fun getAllQuestions(): List<Question> = questions
    override fun getQuestionById(questionId: String): Question? = questions.find { it.questionId == questionId }
    override fun getAvailableSubjects(): List<String> = questions.map { it.subject }.distinct()
    override fun getAvailableMicrothemes(subject: String): List<String> = questions.filter { it.subject == subject }.map { it.microtheme }.distinct()
    override fun getQuestionsForSubject(subject: String): List<Question> = questions.filter { it.subject == subject }
    override fun getQuestionsForSubjectAndMicrotheme(subject: String, microtheme: String): List<Question> =
        questions.filter { it.subject == subject && it.microtheme == microtheme }
}

class MockPracticeHistoryRepo : PracticeHistoryRepository {
    val attempts = mutableListOf<AttemptedQuestionEntity>()
    override suspend fun recordAttempt(questionId: String, selectedAnswer: String, isCorrect: Boolean): Result<Unit> {
        attempts.add(AttemptedQuestionEntity(questionId, selectedAnswer, isCorrect, System.currentTimeMillis()))
        return Result.success(Unit)
    }
    override suspend fun recordOrUpdateAttempt(questionId: String, selectedAnswer: String, isCorrect: Boolean, attemptedAt: Long): Result<Unit> {
        attempts.removeAll { it.questionId == questionId }
        attempts.add(AttemptedQuestionEntity(questionId, selectedAnswer, isCorrect, attemptedAt))
        return Result.success(Unit)
    }
    override suspend fun isQuestionAttempted(questionId: String): Boolean = attempts.any { it.questionId == questionId }
    override suspend fun getAllAttemptedQuestionIds(): List<String> = attempts.map { it.questionId }
    override fun observeAllAttemptedQuestionIds(): Flow<List<String>> = MutableStateFlow(attempts.map { it.questionId })
    override suspend fun getAttempt(questionId: String): AttemptedQuestionEntity? = attempts.find { it.questionId == questionId }
    override suspend fun getAttemptCount(): Int = attempts.size
    override fun observeAttemptCount(): Flow<Int> = MutableStateFlow(attempts.size)
    override suspend fun getAllAttempts(): List<AttemptedQuestionEntity> = attempts.toList()
    override fun observeAllAttempts(): Flow<List<AttemptedQuestionEntity>> = MutableStateFlow(attempts.toList())
}

class MockTestRepo : TestRepository {
    var savedTest: TestEntity? = null
    var savedQuestions: List<TestQuestionEntity> = emptyList()
    override suspend fun saveCompletedTest(test: TestEntity, testQuestions: List<TestQuestionEntity>): Result<Unit> {
        savedTest = test
        savedQuestions = testQuestions
        return Result.success(Unit)
    }
    override fun observeCompletedTests(): Flow<List<TestEntity>> = MutableStateFlow(listOfNotNull(savedTest))
    override suspend fun getAllCompletedTests(): List<TestEntity> = listOfNotNull(savedTest)
    override suspend fun getTestById(testId: String): TestEntity? = if (savedTest?.testId == testId) savedTest else null
    override suspend fun getTestReport(testId: String): TestReport? {
        val test = savedTest ?: return null
        if (test.testId != testId) return null
        return TestReport(
            testId = test.testId,
            testType = TestType.fromString(test.testType),
            startedAt = test.startedAt,
            completedAt = test.completedAt,
            durationSeconds = test.durationSeconds,
            totalQuestions = test.totalQuestions,
            attemptedCount = test.attemptedCount,
            correctCount = test.correctCount,
            incorrectCount = test.incorrectCount,
            unansweredCount = test.unansweredCount,
            score = test.score,
            maxScore = test.maxScore,
            accuracyPercentage = if (test.attemptedCount > 0) test.accuracy else null,
            subjectPerformanceList = emptyList(),
            questions = emptyList()
        )
    }
}
