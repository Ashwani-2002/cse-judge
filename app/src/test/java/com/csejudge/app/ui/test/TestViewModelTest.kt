package com.csejudge.app.ui.test

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TestViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeQuestionBank: FakeQuestionBankRepository
    private lateinit var fakePracticeHistory: FakePracticeHistoryRepository
    private lateinit var fakeTestRepository: FakeTestRepository
    private lateinit var viewModel: TestViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeQuestionBank = FakeQuestionBankRepository()
        fakePracticeHistory = FakePracticeHistoryRepository()
        fakeTestRepository = FakeTestRepository(fakePracticeHistory)

        // Populate question bank with 140 questions across 7 subjects (20 per subject)
        val subjects = listOf("Polity", "Economy", "Geography", "Modern History", "AMAC", "Sci and Tech", "Environment")
        val questions = mutableListOf<Question>()
        for (subj in subjects) {
            for (i in 1..20) {
                questions.add(
                    Question(
                        subject = subj,
                        microtheme = "Theme $i",
                        question = listOf("Statement for Q_${subj}_$i"),
                        options = Options("Option A", "Option B", "Option C", "Option D"),
                        correctAnswer = if (i % 2 == 0) "A" else "B",
                        solution = listOf("Solution for Q_${subj}_$i"),
                        questionId = "Q_${subj}_$i"
                    )
                )
            }
        }
        fakeQuestionBank.setQuestions(questions)

        viewModel = TestViewModel(
            questionBankRepository = fakeQuestionBank,
            practiceHistoryRepository = fakePracticeHistory,
            testRepository = fakeTestRepository,
            bookmarkRepository = null,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testStartFltInitializesCorrectStateAndTimer() = runTest {
        viewModel.startTest(TestType.FULL_LENGTH)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(TestType.FULL_LENGTH, state.testType)
        assertEquals(100, state.totalQuestions)
        assertEquals(7200, state.remainingSeconds)
        assertEquals("120:00", state.formattedTimer)
        assertTrue(state.isTimerActive)
        assertEquals(0, state.currentQuestionIndex)
        assertNotNull(state.currentQuestion)
    }

    @Test
    fun testStartHltInitializesCorrectStateAndTimer() = runTest {
        viewModel.startTest(TestType.HALF_LENGTH)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(TestType.HALF_LENGTH, state.testType)
        assertEquals(50, state.totalQuestions)
        assertEquals(3600, state.remainingSeconds)
        assertEquals("60:00", state.formattedTimer)
        assertTrue(state.isTimerActive)
        assertEquals(0, state.currentQuestionIndex)
        assertNotNull(state.currentQuestion)
    }

    @Test
    fun testOptionSelectionPreservesChoiceWithoutRevealingCorrectness() = runTest {
        viewModel.startTest(TestType.HALF_LENGTH)
        testDispatcher.scheduler.runCurrent()

        // Select Option A
        viewModel.selectOption("A")
        var state = viewModel.uiState.value
        assertEquals("A", state.currentSelectedAnswer)
        assertEquals("A", state.selectedAnswers[0])

        // Change selection to Option C
        viewModel.selectOption("C")
        state = viewModel.uiState.value
        assertEquals("C", state.currentSelectedAnswer)
        assertEquals("C", state.selectedAnswers[0])
    }

    @Test
    fun testMarkForReviewToggling() = runTest {
        viewModel.startTest(TestType.HALF_LENGTH)
        testDispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isCurrentMarkedForReview)

        // Toggle on
        viewModel.toggleMarkForReview()
        assertTrue(viewModel.uiState.value.isCurrentMarkedForReview)
        assertTrue(0 in viewModel.uiState.value.markedForReviewIndices)

        // Toggle off
        viewModel.toggleMarkForReview()
        assertFalse(viewModel.uiState.value.isCurrentMarkedForReview)
        assertFalse(0 in viewModel.uiState.value.markedForReviewIndices)
    }

    @Test
    fun testPaletteColorSemantics() = runTest {
        viewModel.startTest(TestType.HALF_LENGTH)
        testDispatcher.scheduler.runCurrent()

        // Question 0: Unanswered & not review -> Red
        assertEquals(PaletteQuestionState.UNANSWERED_RED, viewModel.uiState.value.getPaletteState(0))

        // Question 0: Answered & not review -> Green
        viewModel.selectOption("A")
        assertEquals(PaletteQuestionState.ANSWERED_GREEN, viewModel.uiState.value.getPaletteState(0))

        // Question 0: Answered & marked for review -> Purple
        viewModel.toggleMarkForReview()
        assertEquals(PaletteQuestionState.MARKED_FOR_REVIEW_PURPLE, viewModel.uiState.value.getPaletteState(0))

        // Question 1: Unanswered & marked for review -> Purple
        viewModel.navigateToQuestion(1)
        viewModel.toggleMarkForReview()
        assertEquals(PaletteQuestionState.MARKED_FOR_REVIEW_PURPLE, viewModel.uiState.value.getPaletteState(1))
    }

    @Test
    fun testQuestionNavigationPreservesAnswers() = runTest {
        viewModel.startTest(TestType.HALF_LENGTH)
        testDispatcher.scheduler.runCurrent()

        // Answer Q0 with "B"
        viewModel.selectOption("B")

        // Advance to Q1
        viewModel.nextQuestion()
        assertEquals(1, viewModel.uiState.value.currentQuestionIndex)
        assertNull(viewModel.uiState.value.currentSelectedAnswer)

        // Answer Q1 with "D"
        viewModel.selectOption("D")

        // Return to Q0 via direct navigation
        viewModel.navigateToQuestion(0)
        assertEquals(0, viewModel.uiState.value.currentQuestionIndex)
        assertEquals("B", viewModel.uiState.value.currentSelectedAnswer)

        // Return to Q1
        viewModel.navigateToQuestion(1)
        assertEquals("D", viewModel.uiState.value.currentSelectedAnswer)
    }

    @Test
    fun testCountdownTimerDecrementsAutorun() = runTest {
        viewModel.startTest(TestType.HALF_LENGTH)
        testDispatcher.scheduler.runCurrent()

        assertEquals(3600, viewModel.uiState.value.remainingSeconds)

        // Advance by 3 seconds
        advanceTimeBy(3000L)
        testDispatcher.scheduler.runCurrent()

        assertEquals(3597, viewModel.uiState.value.remainingSeconds)
        assertEquals("59:57", viewModel.uiState.value.formattedTimer)
    }

    @Test
    fun testTimerExpiryAutoSubmitsTest() = runTest {
        viewModel.startTest(TestType.HALF_LENGTH)
        testDispatcher.scheduler.runCurrent()

        // Answer question 0
        viewModel.selectOption("A")

        // Fast forward 3601 seconds until timer expires
        advanceTimeBy(3601_000L)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue("Test should be completed after timer expiry", state.isCompleted)
        assertNotNull(state.completedTestId)
        assertFalse(state.isTimerActive)
    }

    @Test
    fun testScoreCalculationAndUnifiedPracticePersistenceOnSubmit() = runTest {
        viewModel.startTest(TestType.HALF_LENGTH)
        testDispatcher.scheduler.runCurrent()

        val questions = viewModel.uiState.value.questions
        // Suppose:
        // Question 0: Correct (+2)
        // Question 1: Incorrect (-2/3)
        // Questions 2..49: Unanswered (0)
        val q0Correct = questions[0].correctAnswer
        val q1Wrong = if (questions[1].correctAnswer == "A") "B" else "A"

        viewModel.navigateToQuestion(0)
        viewModel.selectOption(q0Correct)

        viewModel.navigateToQuestion(1)
        viewModel.selectOption(q1Wrong)

        // Submit test
        viewModel.submitTest()
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isCompleted)
        assertNotNull(state.completedTestId)

        // Check saved report
        val savedTest = fakeTestRepository.savedTest
        assertNotNull(savedTest)
        assertEquals(1, savedTest!!.correctCount)
        assertEquals(1, savedTest.incorrectCount)
        assertEquals(48, savedTest.unansweredCount)
        assertEquals(2, savedTest.attemptedCount)

        // Score = 2 - (2/3) = 1.3333333333333333
        val expectedScore = 2.0 - (2.0 / 3.0)
        assertEquals(expectedScore, savedTest.score, 0.001)

        // Check unified Practice persistence:
        // Q0 and Q1 should be persisted into PracticeDao!
        // Unanswered questions (Q2..Q49) must NOT be persisted.
        val practiceAttempts = fakePracticeHistory.allAttempts
        assertEquals(2, practiceAttempts.size)
        assertTrue(practiceAttempts.any { it.questionId == questions[0].questionId && it.isCorrect })
        assertTrue(practiceAttempts.any { it.questionId == questions[1].questionId && !it.isCorrect })
        assertFalse(practiceAttempts.any { it.questionId == questions[2].questionId })
    }

    @Test
    fun testAbortDiscardsActiveSessionWithoutPersistingData() = runTest {
        viewModel.startTest(TestType.HALF_LENGTH)
        testDispatcher.scheduler.runCurrent()

        viewModel.selectOption("A")
        viewModel.toggleMarkForReview()

        // Abort test (e.g. app backgrounded or user exited)
        viewModel.abortTest(reason = "App sent to background")
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isAborted)
        assertEquals("App sent to background", state.abortReason)
        assertTrue(state.questions.isEmpty())
        assertTrue(state.selectedAnswers.isEmpty())
        assertFalse(state.isTimerActive)

        // Ensure NO test report saved
        assertNull(fakeTestRepository.savedTest)

        // Ensure NO practice attempts saved
        assertTrue(fakePracticeHistory.allAttempts.isEmpty())
    }

    @Test
    fun testFltAutoSubmitsWhenTimerReachesZero() = runTest {
        viewModel.startTest(TestType.FULL_LENGTH)
        testDispatcher.scheduler.runCurrent()

        assertEquals(7200, viewModel.uiState.value.remainingSeconds)
        assertEquals("120:00", viewModel.uiState.value.formattedTimer)

        // Select an answer for question 0
        viewModel.selectOption("A")

        // Fast forward 7201 seconds until FLT timer expires
        advanceTimeBy(7201_000L)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue("FLT must be completed after timer reaches zero", state.isCompleted)
        assertEquals(0, state.remainingSeconds)
        assertFalse(state.isTimerActive)
        assertNotNull(state.completedTestId)

        val savedTest = fakeTestRepository.savedTest
        assertNotNull(savedTest)
        assertEquals(TestType.FULL_LENGTH.name, savedTest!!.testType)
        assertEquals(100, savedTest.totalQuestions)
        assertEquals(1, savedTest.attemptedCount)
        assertEquals(99, savedTest.unansweredCount)
    }

    @Test
    fun testHltAutoSubmitsWhenTimerReachesZero() = runTest {
        viewModel.startTest(TestType.HALF_LENGTH)
        testDispatcher.scheduler.runCurrent()

        assertEquals(3600, viewModel.uiState.value.remainingSeconds)
        assertEquals("60:00", viewModel.uiState.value.formattedTimer)

        // Fast forward 3601 seconds until HLT timer expires
        advanceTimeBy(3601_000L)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue("HLT must be completed after timer reaches zero", state.isCompleted)
        assertEquals(0, state.remainingSeconds)
        assertFalse(state.isTimerActive)
        assertNotNull(state.completedTestId)

        val savedTest = fakeTestRepository.savedTest
        assertNotNull(savedTest)
        assertEquals(TestType.HALF_LENGTH.name, savedTest!!.testType)
        assertEquals(50, savedTest.totalQuestions)
        assertEquals(0, savedTest.attemptedCount)
        assertEquals(50, savedTest.unansweredCount)
    }

    @Test
    fun testAutoSubmitEvaluatesCorrectAndIncorrectScoring() = runTest {
        viewModel.startTest(TestType.HALF_LENGTH)
        testDispatcher.scheduler.runCurrent()

        val questions = viewModel.uiState.value.questions
        // Q0: correct (+2)
        val q0Correct = questions[0].correctAnswer
        viewModel.navigateToQuestion(0)
        viewModel.selectOption(q0Correct)

        // Q1: correct (+2)
        val q1Correct = questions[1].correctAnswer
        viewModel.navigateToQuestion(1)
        viewModel.selectOption(q1Correct)

        // Q2: incorrect (-2/3)
        val q2Wrong = if (questions[2].correctAnswer == "A") "B" else "A"
        viewModel.navigateToQuestion(2)
        viewModel.selectOption(q2Wrong)

        // Fast forward timer to 0:00
        advanceTimeBy(3601_000L)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isCompleted)

        val savedTest = fakeTestRepository.savedTest
        assertNotNull(savedTest)
        assertEquals(2, savedTest!!.correctCount)
        assertEquals(1, savedTest.incorrectCount)
        assertEquals(47, savedTest.unansweredCount)
        assertEquals(3, savedTest.attemptedCount)

        // Expected score: (2 * 2.0) - (1 * 2/3) = 4.0 - 0.6666667 = 3.3333333
        val expectedScore = (2 * 2.0) - (1 * (2.0 / 3.0))
        assertEquals(expectedScore, savedTest.score, 0.001)

        // Expected accuracy: 2 / 3 * 100 = 66.6666667%
        val expectedAccuracy = (2.0 / 3.0) * 100.0
        assertEquals(expectedAccuracy, savedTest.accuracy, 0.001)
    }

    @Test
    fun testUnansweredQuestionsReceiveZeroAndDoNotEnterPracticeHistory() = runTest {
        viewModel.startTest(TestType.HALF_LENGTH)
        testDispatcher.scheduler.runCurrent()

        val questions = viewModel.uiState.value.questions
        // Answer only Q0 (correct) and Q1 (incorrect)
        viewModel.navigateToQuestion(0)
        viewModel.selectOption(questions[0].correctAnswer)

        viewModel.navigateToQuestion(1)
        val q1Wrong = if (questions[1].correctAnswer == "A") "B" else "A"
        viewModel.selectOption(q1Wrong)

        // Leave Q2..Q49 unanswered and allow timer to expire
        advanceTimeBy(3601_000L)
        testDispatcher.scheduler.runCurrent()

        // Verify only 2 answered questions entered Practice history
        val attempts = fakePracticeHistory.allAttempts
        assertEquals(2, attempts.size)
        assertTrue(attempts.any { it.questionId == questions[0].questionId && it.isCorrect })
        assertTrue(attempts.any { it.questionId == questions[1].questionId && !it.isCorrect })

        // Verify none of the unanswered questions (Q2..Q49) exist in practice history
        for (i in 2 until 50) {
            assertFalse(
                "Unanswered question ${questions[i].questionId} must not enter practice history",
                attempts.any { it.questionId == questions[i].questionId }
            )
        }
    }

    @Test
    fun testLastSelectedAnswerImmediatelyBeforeExpiryIsIncluded() = runTest {
        viewModel.startTest(TestType.HALF_LENGTH)
        testDispatcher.scheduler.runCurrent()

        val questions = viewModel.uiState.value.questions

        // Fast forward to 1 second before expiry
        advanceTimeBy(3599_000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.uiState.value.remainingSeconds)

        // User selects answer right before timer expiry at T = 1s
        viewModel.navigateToQuestion(5)
        viewModel.selectOption("C")

        // 2 seconds later -> timer hits 0 and auto-submits
        advanceTimeBy(2_000L)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isCompleted)

        val savedTest = fakeTestRepository.savedTest
        assertNotNull(savedTest)
        assertEquals(1, savedTest!!.attemptedCount)

        val q5Snapshot = fakeTestRepository.savedQuestions.find { it.questionId == questions[5].questionId }
        assertNotNull(q5Snapshot)
        assertEquals("C", q5Snapshot!!.selectedAnswer)

        val practiceRecord = fakePracticeHistory.allAttempts.find { it.questionId == questions[5].questionId }
        assertNotNull(practiceRecord)
        assertEquals("C", practiceRecord!!.selectedAnswer)
    }

    @Test
    fun testNoAdditionalSelectionsAcceptedAfterExpiryOrDuringSubmitting() = runTest {
        viewModel.startTest(TestType.HALF_LENGTH)
        testDispatcher.scheduler.runCurrent()

        // Expire the timer
        advanceTimeBy(3601_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isCompleted)

        val answersBeforeAttempt = viewModel.uiState.value.selectedAnswers
        val reviewMarksBeforeAttempt = viewModel.uiState.value.markedForReviewIndices

        // Attempt to select option and toggle mark for review after expiry
        viewModel.selectOption("D")
        viewModel.toggleMarkForReview()
        testDispatcher.scheduler.runCurrent()

        assertEquals(answersBeforeAttempt, viewModel.uiState.value.selectedAnswers)
        assertEquals(reviewMarksBeforeAttempt, viewModel.uiState.value.markedForReviewIndices)
    }

    @Test
    fun testTimerExpiryWhilePaletteOrAbortDialogIsOpenDismissesDialogAndSubmits() = runTest {
        viewModel.startTest(TestType.HALF_LENGTH)
        testDispatcher.scheduler.runCurrent()

        // Open palette and abort dialog
        viewModel.openPalette()
        viewModel.showAbortDialog()
        assertTrue(viewModel.uiState.value.isPaletteOpen)
        assertTrue(viewModel.uiState.value.showAbortConfirmation)

        // Timer reaches zero
        advanceTimeBy(3601_000L)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isCompleted)
        assertFalse("Palette must be dismissed on auto-submission", state.isPaletteOpen)
        assertFalse("Abort dialog must be dismissed on auto-submission", state.showAbortConfirmation)
        assertFalse("Submit dialog must be dismissed on auto-submission", state.showSubmitConfirmation)
    }

    @Test
    fun testAppBackgroundingDuringSubmissionDoesNotAbortTest() = runTest {
        viewModel.startTest(TestType.HALF_LENGTH)
        testDispatcher.scheduler.runCurrent()

        viewModel.selectOption("A")

        // Timer reaches zero
        advanceTimeBy(3601_000L)
        testDispatcher.scheduler.runCurrent()

        // Simulate Lifecycle ON_STOP calling abortTest
        viewModel.abortTest(reason = "App backgrounded")
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertFalse("Abort must be rejected once test is completed/submitting", state.isAborted)
        assertTrue("Test must remain completed", state.isCompleted)
        assertNotNull(fakeTestRepository.savedTest)
    }

    @Test
    fun testSubmissionIdempotencePreventsDuplicateReports() = runTest {
        viewModel.startTest(TestType.HALF_LENGTH)
        testDispatcher.scheduler.runCurrent()

        viewModel.submitTest()
        viewModel.submitTest() // Second simultaneous call
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, fakeTestRepository.saveCallCount)
    }
}

// ------------------- Test Fakes -------------------

class FakeQuestionBankRepository : QuestionBankRepository {
    override var isLoaded: Boolean = true
    private var questions: List<Question> = emptyList()

    fun setQuestions(list: List<Question>) {
        questions = list
    }

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

class FakePracticeHistoryRepository : PracticeHistoryRepository {
    val allAttempts = mutableListOf<AttemptedQuestionEntity>()
    private val attemptsFlow = MutableStateFlow<List<AttemptedQuestionEntity>>(emptyList())

    override suspend fun recordAttempt(questionId: String, selectedAnswer: String, isCorrect: Boolean): Result<Unit> {
        val entity = AttemptedQuestionEntity(questionId, selectedAnswer, isCorrect, System.currentTimeMillis())
        allAttempts.add(entity)
        attemptsFlow.value = allAttempts.toList()
        return Result.success(Unit)
    }

    override suspend fun recordOrUpdateAttempt(questionId: String, selectedAnswer: String, isCorrect: Boolean, attemptedAt: Long): Result<Unit> {
        allAttempts.removeAll { it.questionId == questionId }
        val entity = AttemptedQuestionEntity(questionId, selectedAnswer, isCorrect, attemptedAt)
        allAttempts.add(entity)
        attemptsFlow.value = allAttempts.toList()
        return Result.success(Unit)
    }

    override suspend fun isQuestionAttempted(questionId: String): Boolean = allAttempts.any { it.questionId == questionId }
    override suspend fun getAllAttemptedQuestionIds(): List<String> = allAttempts.map { it.questionId }
    override fun observeAllAttemptedQuestionIds(): Flow<List<String>> = MutableStateFlow(allAttempts.map { it.questionId })
    override suspend fun getAttempt(questionId: String): AttemptedQuestionEntity? = allAttempts.find { it.questionId == questionId }
    override suspend fun getAttemptCount(): Int = allAttempts.size
    override fun observeAttemptCount(): Flow<Int> = MutableStateFlow(allAttempts.size)
    override suspend fun getAllAttempts(): List<AttemptedQuestionEntity> = allAttempts.toList()
    override fun observeAllAttempts(): Flow<List<AttemptedQuestionEntity>> = attemptsFlow.asStateFlow()
}

class FakeTestRepository(
    private val practiceHistory: FakePracticeHistoryRepository? = null
) : TestRepository {
    var savedTest: TestEntity? = null
    var savedQuestions: List<TestQuestionEntity> = emptyList()
    var saveCallCount = 0

    override suspend fun saveCompletedTest(test: TestEntity, testQuestions: List<TestQuestionEntity>): Result<Unit> {
        saveCallCount++
        savedTest = test
        savedQuestions = testQuestions
        testQuestions.forEach { q ->
            if (!q.selectedAnswer.isNullOrBlank()) {
                practiceHistory?.recordOrUpdateAttempt(
                    questionId = q.questionId,
                    selectedAnswer = q.selectedAnswer,
                    isCorrect = q.status == "CORRECT",
                    attemptedAt = test.completedAt
                )
            }
        }
        return Result.success(Unit)
    }

    override fun observeCompletedTests(): Flow<List<TestEntity>> = MutableStateFlow(listOfNotNull(savedTest))
    override suspend fun getAllCompletedTests(): List<TestEntity> = listOfNotNull(savedTest)
    override suspend fun getTestById(testId: String): TestEntity? = if (savedTest?.testId == testId) savedTest else null
    override suspend fun getTestReport(testId: String): TestReport? = null
}
