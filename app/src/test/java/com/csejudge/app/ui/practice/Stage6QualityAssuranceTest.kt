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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * Stage 6 Quality Assurance and Regression Test Suite.
 * Validates question filtering, uniform selection, skip/answer/next lifecycle,
 * persistence invariants, and rapid interaction safety.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Stage6QualityAssuranceTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var fakeQuestionBankRepo: QAQuestionBankRepository
    private lateinit var fakeHistoryRepo: QAPracticeHistoryRepository

    private val allSevenSubjects = listOf(
        "AMAC", "Economy", "Environment", "Geography", "Modern History", "Polity", "Sci and Tech"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeQuestionBankRepo = QAQuestionBankRepository()
        fakeHistoryRepo = QAPracticeHistoryRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(seed: Int = 42): PracticeViewModel {
        return PracticeViewModel(
            questionBankRepository = fakeQuestionBankRepo,
            practiceHistoryRepository = fakeHistoryRepo,
            ioDispatcher = testDispatcher,
            random = Random(seed)
        )
    }

    // =========================================================================
    // 1. Practice Filter Testing (All 7 subjects, microthemes, invalid states)
    // =========================================================================

    @Test
    fun testSelectingEachOfTheSevenSubjects() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        for (subject in allSevenSubjects) {
            vm.selectSubject(subject)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isRandomMode)
            assertEquals(PracticeMode.SUBJECT, state.mode)
            assertEquals(subject, state.selectedSubject)
            assertNull("Microtheme must default to null when selecting subject", state.selectedMicrotheme)
            assertEquals(subject, state.currentQuestion?.subject)
            assertTrue(state.availableMicrothemes.isNotEmpty())
        }
    }

    @Test
    fun testSelectAllMicrothemesOptionAllowsAnyMicrothemeInSubject() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.selectSubject("Polity")
        advanceUntilIdle()

        // Select a specific microtheme first
        vm.selectMicrotheme("Preamble")
        advanceUntilIdle()
        assertEquals("Preamble", vm.uiState.value.selectedMicrotheme)
        assertEquals("Preamble", vm.uiState.value.currentQuestion?.microtheme)

        // Select null ("All microthemes")
        vm.selectMicrotheme(null)
        advanceUntilIdle()
        assertNull(vm.uiState.value.selectedMicrotheme)
        assertEquals("Polity", vm.uiState.value.selectedSubject)
        assertEquals("Polity", vm.uiState.value.currentQuestion?.subject)
    }

    @Test
    fun testChangingSubjectResetsMicrothemeToNull() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.selectSubject("Economy")
        vm.selectMicrotheme("Banking")
        advanceUntilIdle()
        assertEquals("Banking", vm.uiState.value.selectedMicrotheme)

        // Change to Geography
        vm.selectSubject("Geography")
        advanceUntilIdle()
        assertEquals("Geography", vm.uiState.value.selectedSubject)
        assertNull("Selected microtheme should reset when changing subject", vm.uiState.value.selectedMicrotheme)
        assertTrue(vm.uiState.value.availableMicrothemes.contains("Geomorphology"))
        assertFalse(vm.uiState.value.availableMicrothemes.contains("Banking"))
    }

    @Test
    fun testSwitchingBetweenRandomAndSubjectModesPreservesValidState() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        // Initial: Random mode enabled
        assertTrue(vm.uiState.value.isRandomMode)
        assertNull(vm.uiState.value.selectedSubject)

        // Select subject
        vm.selectSubject("Polity")
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isRandomMode)
        assertEquals("Polity", vm.uiState.value.selectedSubject)

        // Switch back to Random mode
        vm.setRandomMode(true)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isRandomMode)
        assertNull(vm.uiState.value.selectedSubject)
        assertNull(vm.uiState.value.selectedMicrotheme)

        // Switch to Subject mode without previously selected subject -> session should clear gracefully
        vm.setRandomMode(false)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isRandomMode)
        assertNull(vm.uiState.value.selectedSubject)
        assertNull(vm.uiState.value.currentQuestion)
    }

    // =========================================================================
    // 2. Uniform Random and Subject-wise Selection Testing
    // =========================================================================

    @Test
    fun testRandomModeDrawsFromAllEligibleUnsolvedQuestionsWithoutSubjectBias() = runTest(testDispatcher) {
        // Run multiple distinct random sessions to verify all 7 subjects are represented
        val observedSubjects = mutableSetOf<String>()

        for (seed in 1..50) {
            val vm = createViewModel(seed = seed)
            advanceUntilIdle()
            val q = vm.uiState.value.currentQuestion
            assertNotNull(q)
            observedSubjects.add(q!!.subject)
        }

        // With 50 random samples across 7 subjects, all 7 subjects should be observed
        assertEquals(allSevenSubjects.toSet(), observedSubjects)
    }

    @Test
    fun testSubjectModeNeverSelectsQuestionsFromOtherSubjects() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.selectSubject("Geography")
        advanceUntilIdle()

        // Cycle through 10 skips
        for (i in 1..10) {
            val q = vm.uiState.value.currentQuestion
            assertNotNull(q)
            assertEquals("Geography", q!!.subject)
            vm.skipQuestion()
            advanceUntilIdle()
        }
    }

    @Test
    fun testSubjectAndMicrothemeModeStrictlyRestrictsQuestions() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.selectSubject("Modern History")
        vm.selectMicrotheme("From 1857 to 1884 CE")
        advanceUntilIdle()

        for (i in 1..5) {
            val q = vm.uiState.value.currentQuestion
            assertNotNull(q)
            assertEquals("Modern History", q!!.subject)
            assertEquals("From 1857 to 1884 CE", q!!.microtheme)
            vm.skipQuestion()
            advanceUntilIdle()
        }
    }

    // =========================================================================
    // 3. Skip, Answer, Next Complete 13-Step Lifecycle
    // =========================================================================

    @Test
    fun testCompleteSkipAnswerNextThirteenStepSequence() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        // 1. Display a question
        val q1 = vm.uiState.value.currentQuestion
        assertNotNull("Step 1: Current question must be displayed", q1)
        val q1Id = q1!!.questionId

        // 2. Skip it
        vm.skipQuestion()
        advanceUntilIdle()

        // 3. Verify that no attempt is recorded in Room
        assertFalse("Step 3: Skipped question must not be in Room", fakeHistoryRepo.attempts.containsKey(q1Id))
        assertFalse(fakeHistoryRepo.attemptedIds.contains(q1Id))

        // 4. Verify that the skipped question is deferred and a new question displayed
        val q2 = vm.uiState.value.currentQuestion
        assertNotNull("Step 4: New question displayed", q2)
        assertNotEquals("Step 4: Skipped question was deferred", q1Id, q2!!.questionId)

        // 5. Display another question (q2 is displayed)
        val q2Id = q2.questionId

        // 6. Answer correctly
        vm.submitAnswer(q2.correctAnswer)
        advanceUntilIdle()

        // 7. Verify correct answer recorded in Room
        assertTrue("Step 7: Correctness flag true in state", vm.uiState.value.isAnswerCorrect == true)
        val q2Attempt = fakeHistoryRepo.attempts[q2Id]
        assertNotNull("Step 7: Attempt recorded in Room", q2Attempt)
        assertTrue("Step 7: Attempt marked correct in Room", q2Attempt!!.isCorrect)
        assertEquals(q2.correctAnswer, q2Attempt.selectedAnswer)

        // 8. Verify solution appears
        assertTrue("Step 8: Question marked as answered", vm.uiState.value.isAnswered)
        assertNotNull("Step 8: Correct answer exposed", vm.uiState.value.correctAnswer)

        // 9. Verify Next loads another eligible question
        assertTrue(vm.uiState.value.isNextEnabled)
        vm.nextQuestion()
        advanceUntilIdle()
        val q3 = vm.uiState.value.currentQuestion
        assertNotNull("Step 9: Next loads eligible question", q3)
        assertNotEquals("Step 9: New question is different from q2", q2Id, q3!!.questionId)

        // 10. Answer incorrectly
        val q3Id = q3.questionId
        val wrongAnswer = if (q3.correctAnswer == "A") "B" else "A"
        vm.submitAnswer(wrongAnswer)
        advanceUntilIdle()

        // 11. Verify selected option is incorrect and correct option indicated
        assertEquals(wrongAnswer, vm.uiState.value.selectedAnswer)
        assertEquals(false, vm.uiState.value.isAnswerCorrect)
        assertEquals(q3.correctAnswer, vm.uiState.value.correctAnswer)

        // 12. Verify incorrect answer permanently recorded in Room
        val q3Attempt = fakeHistoryRepo.attempts[q3Id]
        assertNotNull("Step 12: Incorrect attempt recorded in Room", q3Attempt)
        assertFalse("Step 12: Incorrect flag stored in Room", q3Attempt!!.isCorrect)
        assertEquals(wrongAnswer, q3Attempt.selectedAnswer)

        // 13. Verify previously answered questions never return
        vm.nextQuestion()
        advanceUntilIdle()

        // Cycle through all remaining questions
        val answeredIds = setOf(q2Id, q3Id)
        for (i in 1..20) {
            val curr = vm.uiState.value.currentQuestion ?: break
            assertFalse("Step 13: Answered questions must never return", curr.questionId in answeredIds)
            vm.skipQuestion()
            advanceUntilIdle()
        }
    }

    // =========================================================================
    // 4. Rapid Interaction and Stress Safety
    // =========================================================================

    @Test
    fun testRapidRepeatedSkipTapsAreSafeAndResponsive() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        // Rapid 50 skip taps
        for (i in 1..50) {
            vm.skipQuestion()
        }
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.currentQuestion)
        assertEquals(50, vm.uiState.value.sessionStats.skippedCount)
        assertEquals(0, vm.uiState.value.sessionStats.answeredCount)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun testRapidRepeatedNextTapsDoNotCauseStateInconsistency() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val q = vm.uiState.value.currentQuestion!!
        vm.submitAnswer(q.correctAnswer)
        advanceUntilIdle()

        // First next advances
        vm.nextQuestion()
        // Subsequent rapid calls are ignored because isAnswered becomes false immediately
        vm.nextQuestion()
        vm.nextQuestion()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.currentQuestion)
        assertFalse(vm.uiState.value.isAnswered)
        assertFalse(vm.uiState.value.isNextEnabled)
        assertTrue(vm.uiState.value.isSkipEnabled)
    }

    // =========================================================================
    // 5. Attempt History and Persistence Invariants
    // =========================================================================

    @Test
    fun testAttemptTimestampsAndFieldsAreProperlyStored() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val beforeTime = System.currentTimeMillis()
        val q = vm.uiState.value.currentQuestion!!
        vm.submitAnswer(q.correctAnswer)
        advanceUntilIdle()
        val afterTime = System.currentTimeMillis()

        val record = fakeHistoryRepo.attempts[q.questionId]
        assertNotNull(record)
        assertEquals(q.questionId, record!!.questionId)
        assertEquals(q.correctAnswer, record.selectedAnswer)
        assertTrue(record.isCorrect)
        assertTrue(record.attemptedAt in beforeTime..afterTime)
    }

    @Test
    fun testAttemptedQuestionsRemainExcludedAfterSimulatedAppRestart() = runTest(testDispatcher) {
        // Session 1: User attempts POL00001 and ECO00001
        fakeHistoryRepo.recordAttempt("POL00001", "A", true)
        fakeHistoryRepo.recordAttempt("ECO00001", "D", false)

        // Session 2: Fresh ViewModel instance created (simulating app restart)
        val vm = createViewModel()
        advanceUntilIdle()

        // Switch to Polity -> POL00001 must not appear
        vm.selectSubject("Polity")
        vm.selectMicrotheme("Preamble")
        advanceUntilIdle()

        val q = vm.uiState.value.currentQuestion
        assertNotNull(q)
        assertEquals("POL00002", q!!.questionId)
        assertNotEquals("POL00001", q.questionId)

        // Answer POL00002 -> Preamble is now exhausted
        vm.submitAnswer(q.correctAnswer)
        advanceUntilIdle()
        vm.nextQuestion()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isExhausted)
        assertNull(vm.uiState.value.currentQuestion)
    }

    @Test
    fun testAttemptHistoryIsNotDeletedWhenChangingFiltersOrNavigating() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        // Answer 1 question
        val q = vm.uiState.value.currentQuestion!!
        vm.submitAnswer(q.correctAnswer)
        advanceUntilIdle()
        assertEquals(1, fakeHistoryRepo.getAttemptCount())

        // Change filters multiple times
        vm.selectSubject("Economy")
        advanceUntilIdle()
        vm.selectSubject("Modern History")
        advanceUntilIdle()
        vm.setRandomMode(true)
        advanceUntilIdle()

        // Attempt history in Room is intact!
        assertEquals(1, fakeHistoryRepo.getAttemptCount())
        assertTrue(fakeHistoryRepo.isQuestionAttempted(q.questionId))
    }

    // =========================================================================
    // Test Fakes
    // =========================================================================

    private class QAQuestionBankRepository : QuestionBankRepository {
        override var isLoaded: Boolean = true

        private val testQuestions = listOf(
            createQ("POL00001", "Polity", "Preamble", "A"),
            createQ("POL00002", "Polity", "Preamble", "B"),
            createQ("POL00003", "Polity", "Judiciary", "C"),
            createQ("ECO00001", "Economy", "Banking", "D"),
            createQ("ECO00002", "Economy", "Macro economy", "A"),
            createQ("GEO00001", "Geography", "Geomorphology", "B"),
            createQ("MH00001", "Modern History", "From 1857 to 1884 CE", "C"),
            createQ("MH00002", "Modern History", "Advent of Europeans", "D"),
            createQ("ENV00001", "Environment", "Ecology", "A"),
            createQ("AMAC00001", "AMAC", "Pre-Historic Period", "B"),
            createQ("SNT00001", "Sci and Tech", "Space", "C")
        )

        private fun createQ(id: String, subj: String, mt: String, ans: String) = Question(
            questionId = id,
            subject = subj,
            microtheme = mt,
            question = listOf("Statement 1 for $id", "Statement 2 for $id"),
            options = Options("Option A", "Option B", "Option C", "Option D"),
            correctAnswer = ans,
            solution = listOf("Solution line 1 for $id", "Solution line 2 for $id")
        )

        override suspend fun loadQuestionBank() = QuestionBankStats(testQuestions.size, emptyMap(), emptyMap(), 0)
        override fun getAllQuestions(): List<Question> = testQuestions
        override fun getQuestionById(questionId: String): Question? = testQuestions.find { it.questionId == questionId }
        override fun getAvailableSubjects(): List<String> = testQuestions.map { it.subject }.distinct()
        override fun getAvailableMicrothemes(subject: String): List<String> =
            testQuestions.filter { it.subject == subject }.map { it.microtheme }.distinct()
        override fun getQuestionsForSubject(subject: String): List<Question> =
            testQuestions.filter { it.subject == subject }
        override fun getQuestionsForSubjectAndMicrotheme(subject: String, microtheme: String): List<Question> =
            testQuestions.filter { it.subject == subject && it.microtheme == microtheme }
    }

    private class QAPracticeHistoryRepository : PracticeHistoryRepository {
        val attempts = mutableMapOf<String, AttemptedQuestionEntity>()
        val attemptedIds = mutableSetOf<String>()

        override suspend fun recordAttempt(
            questionId: String,
            selectedAnswer: String,
            isCorrect: Boolean
        ): Result<Unit> {
            if (attempts.containsKey(questionId)) {
                return Result.failure(IllegalStateException("Duplicate attempt for $questionId"))
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

        override suspend fun recordOrUpdateAttempt(
            questionId: String,
            selectedAnswer: String,
            isCorrect: Boolean,
            attemptedAt: Long
        ): Result<Unit> {
            val entity = AttemptedQuestionEntity(
                questionId = questionId,
                selectedAnswer = selectedAnswer,
                isCorrect = isCorrect,
                attemptedAt = attemptedAt
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
        override suspend fun getAllAttempts(): List<AttemptedQuestionEntity> = attempts.values.toList()
        override fun observeAllAttempts(): Flow<List<AttemptedQuestionEntity>> = flowOf(attempts.values.toList())
    }
}
