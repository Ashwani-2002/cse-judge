package com.csejudge.app.ui.practice

import com.csejudge.app.data.local.AttemptedQuestionEntity
import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.repository.PracticeHistoryRepository
import com.csejudge.app.data.repository.QuestionBankRepository
import com.csejudge.app.data.repository.QuestionBankStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
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
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var fakeQuestionBankRepo: FakeQuestionBankRepository
    private lateinit var fakeHistoryRepo: FakePracticeHistoryRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeQuestionBankRepo = FakeQuestionBankRepository()
        fakeHistoryRepo = FakePracticeHistoryRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PracticeViewModel {
        return PracticeViewModel(
            questionBankRepository = fakeQuestionBankRepo,
            practiceHistoryRepository = fakeHistoryRepo,
            ioDispatcher = testDispatcher
        )
    }

    // ==========================================
    // 1. Mode and Filtering Tests
    // ==========================================

    @Test
    fun testRandomModeIncludesQuestionsFromAllSubjects() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isRandomMode)
        assertEquals(PracticeMode.RANDOM, state.mode)
        assertNull(state.selectedSubject)
        assertNull(state.selectedMicrotheme)
        assertNotNull(state.currentQuestion)
    }

    @Test
    fun testSubjectModeRestrictsQuestionsToSelectedSubject() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.selectSubject("Economy")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isRandomMode)
        assertEquals(PracticeMode.SUBJECT, state.mode)
        assertEquals("Economy", state.selectedSubject)
        assertEquals("Economy", state.currentQuestion?.subject)
    }

    @Test
    fun testSubjectOnlyModeIncludesAllMicrothemesInSubject() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.selectSubject("Polity")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Polity", state.selectedSubject)
        assertNull(state.selectedMicrotheme)
        assertTrue(state.availableMicrothemes.contains("Preamble"))
        assertTrue(state.availableMicrothemes.contains("Judiciary"))
    }

    @Test
    fun testSubjectPlusMicrothemeModeRestrictsQuestionsCorrectly() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.selectSubject("Polity")
        advanceUntilIdle()

        vm.selectMicrotheme("Preamble")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Polity", state.selectedSubject)
        assertEquals("Preamble", state.selectedMicrotheme)
        assertEquals("Polity", state.currentQuestion?.subject)
        assertEquals("Preamble", state.currentQuestion?.microtheme)
    }

    @Test
    fun testInvalidFilterCombinationsAreRejected() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        // Invalid subject
        vm.selectSubject("NonExistentSubject")
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.errorMessage!!.contains("Invalid subject"))

        // Clear error and verify selection was not applied
        vm.clearError()
        assertNull(vm.uiState.value.errorMessage)

        // Invalid microtheme
        vm.selectSubject("Economy")
        advanceUntilIdle()
        vm.selectMicrotheme("InvalidMicrotheme")
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.errorMessage!!.contains("Invalid microtheme"))
    }

    @Test
    fun testActualModernHistoryMicrothemeUsed() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.selectSubject("Modern History")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.availableMicrothemes.contains("From 1857 to 1884 CE"))
        assertFalse(state.availableMicrothemes.contains("From 1757 to 1884 CE"))

        vm.selectMicrotheme("From 1857 to 1884 CE")
        advanceUntilIdle()
        assertEquals("From 1857 to 1884 CE", vm.uiState.value.selectedMicrotheme)
        assertEquals("From 1857 to 1884 CE", vm.uiState.value.currentQuestion?.microtheme)
    }

    // ==========================================
    // 2. Attempt History Tests
    // ==========================================

    @Test
    fun testPreviouslyAttemptedQuestionsAreExcluded() = runTest(testDispatcher) {
        // Mark POL00001 as already attempted in Room
        fakeHistoryRepo.attemptedIds.add("POL00001")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.selectSubject("Polity")
        vm.selectMicrotheme("Preamble")
        advanceUntilIdle()

        // Preamble only has POL00001 and POL00002. Since POL00001 was attempted, POL00002 must be selected!
        assertEquals("POL00002", vm.uiState.value.currentQuestion?.questionId)
    }

    @Test
    fun testCorrectAnswerIsPersistedInRoom() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val q = vm.uiState.value.currentQuestion!!
        vm.submitAnswer(q.correctAnswer)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isAnswered)
        assertEquals(true, state.isAnswerCorrect)
        assertEquals(q.correctAnswer, state.selectedAnswer)

        // Verify persisted in Room
        val persisted = fakeHistoryRepo.attempts[q.questionId]
        assertNotNull(persisted)
        assertEquals(q.questionId, persisted?.questionId)
        assertEquals(q.correctAnswer, persisted?.selectedAnswer)
        assertTrue(persisted?.isCorrect == true)
    }

    @Test
    fun testIncorrectAnswerIsPersistedInRoom() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val q = vm.uiState.value.currentQuestion!!
        val wrongAnswer = if (q.correctAnswer == "A") "B" else "A"
        vm.submitAnswer(wrongAnswer)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isAnswered)
        assertEquals(false, state.isAnswerCorrect)
        assertEquals(wrongAnswer, state.selectedAnswer)

        val persisted = fakeHistoryRepo.attempts[q.questionId]
        assertNotNull(persisted)
        assertEquals(wrongAnswer, persisted?.selectedAnswer)
        assertFalse(persisted?.isCorrect == true)
    }

    @Test
    fun testSkippingDoesNotCreateRoomAttempt() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val initialQ = vm.uiState.value.currentQuestion!!
        vm.skipQuestion()
        advanceUntilIdle()

        assertFalse(fakeHistoryRepo.attempts.containsKey(initialQ.questionId))
        assertFalse(fakeHistoryRepo.attemptedIds.contains(initialQ.questionId))
    }

    @Test
    fun testDuplicateSubmissionIsPrevented() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val q = vm.uiState.value.currentQuestion!!
        vm.submitAnswer(q.correctAnswer)
        // Immediately trigger second submission
        vm.submitAnswer("D")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(q.correctAnswer, state.selectedAnswer)
        assertEquals(1, fakeHistoryRepo.recordAttemptCallCount)
    }

    @Test
    fun testFailedPersistenceDoesNotMarkQuestionAttempted() = runTest(testDispatcher) {
        fakeHistoryRepo.failNextAttempt = true

        val vm = createViewModel()
        advanceUntilIdle()

        val q = vm.uiState.value.currentQuestion!!
        vm.submitAnswer(q.correctAnswer)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse("Question should not be marked answered if persistence failed", state.isAnswered)
        assertFalse(state.isSubmitting)
        assertNotNull(state.errorMessage)
        assertFalse(fakeHistoryRepo.attempts.containsKey(q.questionId))
    }

    // ==========================================
    // 3. Skip and Next Tests
    // ==========================================

    @Test
    fun testNextIsUnavailableBeforeAnswering() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isNextEnabled)
        assertTrue(state.isSkipEnabled)

        // Attempting to invoke nextQuestion() before answering does nothing
        val current = state.currentQuestion
        vm.nextQuestion()
        advanceUntilIdle()
        assertEquals(current?.questionId, vm.uiState.value.currentQuestion?.questionId)
    }

    @Test
    fun testNextSelectsNewQuestionAfterSuccessfulAnswer() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val firstQ = vm.uiState.value.currentQuestion!!
        vm.submitAnswer(firstQ.correctAnswer)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isNextEnabled)
        assertFalse(vm.uiState.value.isSkipEnabled)

        vm.nextQuestion()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isAnswered)
        assertNull(state.selectedAnswer)
        assertNull(state.isAnswerCorrect)
        assertNotNull(state.currentQuestion)
        assertTrue(state.currentQuestion?.questionId != firstQ.questionId)
        assertTrue(state.isSkipEnabled)
        assertFalse(state.isNextEnabled)
    }

    @Test
    fun testExhaustionDetectedWhenNoUnattemptedQuestionsRemain() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        // Preamble has exactly 2 questions: POL00001 and POL00002
        vm.selectSubject("Polity")
        vm.selectMicrotheme("Preamble")
        advanceUntilIdle()

        // Answer question 1
        val q1 = vm.uiState.value.currentQuestion!!
        vm.submitAnswer(q1.correctAnswer)
        advanceUntilIdle()
        vm.nextQuestion()
        advanceUntilIdle()

        // Answer question 2
        val q2 = vm.uiState.value.currentQuestion!!
        assertTrue(q2.questionId != q1.questionId)
        vm.submitAnswer(q2.correctAnswer)
        advanceUntilIdle()
        vm.nextQuestion()
        advanceUntilIdle()

        // Pool is now completely exhausted
        val state = vm.uiState.value
        assertTrue(state.isExhausted)
        assertNull(state.currentQuestion)
        assertFalse(state.isSkipEnabled)
        assertFalse(state.isNextEnabled)
    }

    // ==========================================
    // 4. Filter Reset Tests
    // ==========================================

    @Test
    fun testChangingSubjectResetsSession() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.selectSubject("Economy")
        advanceUntilIdle()
        assertEquals("Economy", vm.uiState.value.currentQuestion?.subject)

        vm.selectSubject("Geography")
        advanceUntilIdle()
        assertEquals("Geography", vm.uiState.value.currentQuestion?.subject)
    }

    @Test
    fun testChangingMicrothemeResetsSession() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.selectSubject("Polity")
        advanceUntilIdle()

        vm.selectMicrotheme("Preamble")
        advanceUntilIdle()
        assertEquals("Preamble", vm.uiState.value.currentQuestion?.microtheme)

        vm.selectMicrotheme("Judiciary")
        advanceUntilIdle()
        assertEquals("Judiciary", vm.uiState.value.currentQuestion?.microtheme)
    }

    @Test
    fun testChangingRandomModeResetsSession() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.selectSubject("Economy")
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isRandomMode)

        vm.setRandomMode(true)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isRandomMode)
        assertNull(vm.uiState.value.selectedSubject)
        assertNull(vm.uiState.value.selectedMicrotheme)
    }

    @Test
    fun testSkippedQuestionsBecomeEligibleAgainInNewSession() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.selectSubject("Polity")
        vm.selectMicrotheme("Preamble")
        advanceUntilIdle()

        val q1 = vm.uiState.value.currentQuestion!!
        vm.skipQuestion()
        advanceUntilIdle()

        // Restart session: skipped question should be eligible again
        vm.restartSession()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.currentQuestion)
        assertFalse(fakeHistoryRepo.attemptedIds.contains(q1.questionId))
    }

    // ==========================================
    // 5. Statistics Tests
    // ==========================================

    @Test
    fun testSessionStatisticsAreAccurate() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        // Skip 1 question
        vm.skipQuestion()
        advanceUntilIdle()

        // Answer 1 correctly
        val q1 = vm.uiState.value.currentQuestion!!
        vm.submitAnswer(q1.correctAnswer)
        advanceUntilIdle()
        vm.nextQuestion()
        advanceUntilIdle()

        // Answer 1 incorrectly
        val q2 = vm.uiState.value.currentQuestion!!
        val wrongAns = if (q2.correctAnswer == "A") "D" else "A"
        vm.submitAnswer(wrongAns)
        advanceUntilIdle()

        val stats = vm.uiState.value.sessionStats
        assertEquals(1, stats.skippedCount)
        assertEquals(2, stats.answeredCount)
        assertEquals(1, stats.correctCount)
        assertEquals(1, stats.incorrectCount)
    }

    // ==========================================
    // Test Fakes
    // ==========================================

    private class FakeQuestionBankRepository : QuestionBankRepository {
        override var isLoaded: Boolean = true

        private val allQuestions = listOf(
            createTestQuestion("POL00001", "Polity", "Preamble", "A"),
            createTestQuestion("POL00002", "Polity", "Preamble", "B"),
            createTestQuestion("POL00003", "Polity", "Judiciary", "C"),
            createTestQuestion("ECO00001", "Economy", "Banking", "D"),
            createTestQuestion("ECO00002", "Economy", "Macro economy", "A"),
            createTestQuestion("GEO00001", "Geography", "Geomorphology", "B"),
            createTestQuestion("MH00001", "Modern History", "From 1857 to 1884 CE", "C"),
            createTestQuestion("MH00002", "Modern History", "Advent of Europeans", "D"),
            createTestQuestion("ENV00001", "Environment", "Ecology", "A"),
            createTestQuestion("AMAC00001", "AMAC", "Pre-Historic Period", "B"),
            createTestQuestion("SNT00001", "Sci and Tech", "Space", "C")
        )

        private fun createTestQuestion(id: String, subject: String, microtheme: String, correct: String): Question {
            return Question(
                questionId = id,
                subject = subject,
                microtheme = microtheme,
                question = listOf("Statement for $id"),
                options = Options("1", "2", "3", "4"),
                correctAnswer = correct,
                solution = listOf("Solution for $id")
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
        var recordAttemptCallCount = 0
        var failNextAttempt = false

        override suspend fun recordAttempt(
            questionId: String,
            selectedAnswer: String,
            isCorrect: Boolean
        ): Result<Unit> {
            recordAttemptCallCount++
            if (failNextAttempt) {
                failNextAttempt = false
                return Result.failure(IOException("Disk I/O error"))
            }
            if (attempts.containsKey(questionId)) {
                return Result.failure(IllegalStateException("Already attempted"))
            }
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
