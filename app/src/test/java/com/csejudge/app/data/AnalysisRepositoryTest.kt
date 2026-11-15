package com.csejudge.app.data

import com.csejudge.app.data.local.AttemptedQuestionEntity
import com.csejudge.app.data.repository.AnalysisRepository
import com.csejudge.app.data.repository.AnalysisRepositoryImpl
import com.csejudge.app.data.repository.PracticeHistoryRepository
import com.csejudge.app.data.repository.QuestionBankRepository
import com.csejudge.app.data.repository.QuestionBankRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class AnalysisRepositoryTest {

    private lateinit var questionBankRepository: QuestionBankRepository
    private lateinit var fakeHistoryRepository: FakeTestPracticeHistoryRepository
    private lateinit var analysisRepository: AnalysisRepository

    @Before
    fun setUp() {
        val questionBankDir = File("../question-bank").let {
            if (it.exists()) it else File("question-bank")
        }
        questionBankRepository = QuestionBankRepositoryImpl(
            openAssetStream = { fileName ->
                val file = File(questionBankDir, fileName)
                require(file.exists()) { "Question bank file not found: ${file.absolutePath}" }
                file.inputStream()
            }
        )
        fakeHistoryRepository = FakeTestPracticeHistoryRepository()
        runBlocking {
            questionBankRepository.loadQuestionBank()
        }
        analysisRepository = AnalysisRepositoryImpl(
            questionBankRepository = questionBankRepository,
            practiceHistoryRepository = fakeHistoryRepository
        )
    }

    // =========================================================================
    // 1. Overall Analysis Tests
    // =========================================================================

    @Test
    fun testOverallAnalysisWithNoAttempts() = runBlocking {
        val overall = analysisRepository.getOverallAnalysis()

        assertEquals(1432, overall.totalQuestions)
        assertEquals(0, overall.attemptedQuestions)
        assertEquals(1432, overall.unattemptedQuestions)
        assertEquals(0.0, overall.coveragePercentage, 0.001)
        assertEquals(0, overall.correctAnswers)
        assertEquals(0, overall.incorrectAnswers)
        // Zero-attempt must return null (N/A) accuracy, never 0.0%
        assertNull(overall.accuracyPercentage)
    }

    @Test
    fun testOverallAnalysisWithOnlyCorrectAttempts() = runBlocking {
        // Record 10 correct attempts
        val questions = questionBankRepository.getAllQuestions().take(10)
        questions.forEach { q ->
            fakeHistoryRepository.recordAttempt(q.questionId, q.correctAnswer, isCorrect = true)
        }

        val overall = analysisRepository.getOverallAnalysis()
        assertEquals(1432, overall.totalQuestions)
        assertEquals(10, overall.attemptedQuestions)
        assertEquals(1422, overall.unattemptedQuestions)
        assertEquals((10.0 / 1432.0) * 100.0, overall.coveragePercentage, 0.001)
        assertEquals(10, overall.correctAnswers)
        assertEquals(0, overall.incorrectAnswers)
        assertEquals(100.0, overall.accuracyPercentage!!, 0.001)
    }

    @Test
    fun testOverallAnalysisWithOnlyIncorrectAttempts() = runBlocking {
        // Record 5 incorrect attempts
        val questions = questionBankRepository.getAllQuestions().take(5)
        questions.forEach { q ->
            fakeHistoryRepository.recordAttempt(q.questionId, "X", isCorrect = false)
        }

        val overall = analysisRepository.getOverallAnalysis()
        assertEquals(1432, overall.totalQuestions)
        assertEquals(5, overall.attemptedQuestions)
        assertEquals(1427, overall.unattemptedQuestions)
        assertEquals((5.0 / 1432.0) * 100.0, overall.coveragePercentage, 0.001)
        assertEquals(0, overall.correctAnswers)
        assertEquals(5, overall.incorrectAnswers)
        // Attempted > 0 and correct == 0, so 0.0% accuracy is expected
        assertEquals(0.0, overall.accuracyPercentage!!, 0.001)
    }

    @Test
    fun testOverallAnalysisWithMixedAttempts() = runBlocking {
        val questions = questionBankRepository.getAllQuestions().take(10)
        // 7 correct, 3 incorrect
        questions.take(7).forEach { q ->
            fakeHistoryRepository.recordAttempt(q.questionId, q.correctAnswer, isCorrect = true)
        }
        questions.drop(7).forEach { q ->
            fakeHistoryRepository.recordAttempt(q.questionId, "X", isCorrect = false)
        }

        val overall = analysisRepository.getOverallAnalysis()
        assertEquals(1432, overall.totalQuestions)
        assertEquals(10, overall.attemptedQuestions)
        assertEquals(1422, overall.unattemptedQuestions)
        assertEquals(7, overall.correctAnswers)
        assertEquals(3, overall.incorrectAnswers)
        assertEquals(70.0, overall.accuracyPercentage!!, 0.001)
    }

    // =========================================================================
    // 2. Subject-wise Analysis Tests
    // =========================================================================

    @Test
    fun testSubjectWiseAnalysisContainsAllSevenSubjectsWithExactTotals() = runBlocking {
        val subjects = analysisRepository.getSubjectAnalysisList()

        assertEquals(7, subjects.size)
        val expectedTotals = mapOf(
            "AMAC" to 149,
            "Economy" to 243,
            "Environment" to 248,
            "Geography" to 233,
            "Modern History" to 127,
            "Polity" to 252,
            "Sci and Tech" to 180
        )

        for (subjectAnalysis in subjects) {
            val expected = expectedTotals[subjectAnalysis.subject]
            assertNotNull("Unexpected subject '${subjectAnalysis.subject}'", expected)
            assertEquals("Total questions mismatch for '${subjectAnalysis.subject}'", expected, subjectAnalysis.totalQuestions)
        }
    }

    @Test
    fun testSubjectWiseAttemptsAndAccuracyCalculations() = runBlocking {
        // Attempt 4 Polity questions: 3 correct, 1 incorrect
        val polityQuestions = questionBankRepository.getQuestionsForSubject("Polity").take(4)
        fakeHistoryRepository.recordAttempt(polityQuestions[0].questionId, polityQuestions[0].correctAnswer, isCorrect = true)
        fakeHistoryRepository.recordAttempt(polityQuestions[1].questionId, polityQuestions[1].correctAnswer, isCorrect = true)
        fakeHistoryRepository.recordAttempt(polityQuestions[2].questionId, polityQuestions[2].correctAnswer, isCorrect = true)
        fakeHistoryRepository.recordAttempt(polityQuestions[3].questionId, "X", isCorrect = false)

        val subjects = analysisRepository.getSubjectAnalysisList()
        val polity = subjects.first { it.subject == "Polity" }

        assertEquals(252, polity.totalQuestions)
        assertEquals(4, polity.attemptedQuestions)
        assertEquals(248, polity.unattemptedQuestions)
        assertEquals(3, polity.correctAnswers)
        assertEquals(1, polity.incorrectAnswers)
        assertEquals((4.0 / 252.0) * 100.0, polity.coveragePercentage, 0.001)
        assertEquals(75.0, polity.accuracyPercentage!!, 0.001)

        // Other subjects have 0 attempts -> accuracy must be null
        val economy = subjects.first { it.subject == "Economy" }
        assertEquals(243, economy.totalQuestions)
        assertEquals(0, economy.attemptedQuestions)
        assertEquals(243, economy.unattemptedQuestions)
        assertEquals(0.0, economy.coveragePercentage, 0.001)
        assertNull(economy.accuracyPercentage)
    }

    // =========================================================================
    // 3. Microtheme Analysis Tests
    // =========================================================================

    @Test
    fun testMicrothemeDynamicDiscoveryAndTotals() = runBlocking {
        val modernHistoryMthemes = analysisRepository.getMicrothemeAnalysisList("Modern History")
        assertEquals(16, modernHistoryMthemes.size)

        // 1. "From 1857 to 1884 CE" must exist with exactly 5 questions
        val mtheme1857 = modernHistoryMthemes.firstOrNull { it.microtheme == "From 1857 to 1884 CE" }
        assertNotNull("Expected 'From 1857 to 1884 CE' in Modern History", mtheme1857)
        assertEquals(5, mtheme1857?.totalQuestions)

        // 2. "From 1757 to 1884 CE" must NOT exist
        val mtheme1757 = modernHistoryMthemes.firstOrNull { it.microtheme.contains("1757") }
        assertNull("Forbidden 'From 1757 to 1884 CE' must not exist", mtheme1757)

        // 3. Economy's "Macro economy" must exist with exactly 5 questions
        val economyMthemes = analysisRepository.getMicrothemeAnalysisList("Economy")
        val macroEconomy = economyMthemes.firstOrNull { it.microtheme == "Macro economy" }
        assertNotNull("Expected 'Macro economy' in Economy", macroEconomy)
        assertEquals(5, macroEconomy?.totalQuestions)
    }

    @Test
    fun testMicrothemeAttemptAggregationAndAccuracy() = runBlocking {
        // Attempt questions in Modern History -> "From 1857 to 1884 CE"
        val questionsIn1857 = questionBankRepository.getQuestionsForSubjectAndMicrotheme(
            "Modern History",
            "From 1857 to 1884 CE"
        )
        assertEquals(5, questionsIn1857.size)

        // Attempt 2 questions: 1 correct, 1 incorrect
        fakeHistoryRepository.recordAttempt(questionsIn1857[0].questionId, questionsIn1857[0].correctAnswer, true)
        fakeHistoryRepository.recordAttempt(questionsIn1857[1].questionId, "X", false)

        val mthemes = analysisRepository.getMicrothemeAnalysisList("Modern History")
        val targetMtheme = mthemes.first { it.microtheme == "From 1857 to 1884 CE" }

        assertEquals(5, targetMtheme.totalQuestions)
        assertEquals(2, targetMtheme.attemptedQuestions)
        assertEquals(3, targetMtheme.unattemptedQuestions)
        assertEquals(1, targetMtheme.correctAnswers)
        assertEquals(1, targetMtheme.incorrectAnswers)
        assertEquals(40.0, targetMtheme.coveragePercentage, 0.001)
        assertEquals(50.0, targetMtheme.accuracyPercentage!!, 0.001)

        // Other microthemes in Modern History must have null accuracy
        val otherMtheme = mthemes.first { it.microtheme != "From 1857 to 1884 CE" }
        assertEquals(0, otherMtheme.attemptedQuestions)
        assertNull(otherMtheme.accuracyPercentage)
    }

    // =========================================================================
    // 4. Reactive Updates & Re-answering Tests
    // =========================================================================

    @Test
    fun testReactiveFlowEmitsUpdatesWhenAttemptsChange() = runBlocking {
        val flow = analysisRepository.observeFullAnalysis()
        val initialSnapshot = flow.first()
        assertEquals(0, initialSnapshot.overall.attemptedQuestions)

        val question = questionBankRepository.getAllQuestions().first()
        fakeHistoryRepository.recordAttempt(question.questionId, question.correctAnswer, true)

        val updatedSnapshot = flow.first()
        assertEquals(1, updatedSnapshot.overall.attemptedQuestions)
        assertEquals(1, updatedSnapshot.overall.correctAnswers)
        assertEquals(100.0, updatedSnapshot.overall.accuracyPercentage!!, 0.001)
    }

    @Test
    fun testReansweringQuestionUpdatesAccuracyWithoutInflatingAttemptCount() = runBlocking {
        val question = questionBankRepository.getAllQuestions().first()

        // 1. Initial attempt: INCORRECT
        fakeHistoryRepository.recordAttempt(question.questionId, "WRONG", isCorrect = false)
        var overall = analysisRepository.getOverallAnalysis()
        assertEquals(1, overall.attemptedQuestions)
        assertEquals(0, overall.correctAnswers)
        assertEquals(1, overall.incorrectAnswers)
        assertEquals(0.0, overall.accuracyPercentage!!, 0.001)

        // 2. Re-answering the question (e.g. from Bookmarks screen) to CORRECT
        fakeHistoryRepository.recordOrUpdateAttempt(question.questionId, question.correctAnswer, isCorrect = true)
        overall = analysisRepository.getOverallAnalysis()
        // Attempted count must NOT inflate!
        assertEquals(1, overall.attemptedQuestions)
        assertEquals(1, overall.correctAnswers)
        assertEquals(0, overall.incorrectAnswers)
        assertEquals(100.0, overall.accuracyPercentage!!, 0.001)
    }

    @Test
    fun testSkippingDoesNotCountAsAttempted() = runBlocking {
        // Skipping does not call recordAttempt
        val overall = analysisRepository.getOverallAnalysis()
        assertEquals(0, overall.attemptedQuestions)
        assertEquals(1432, overall.unattemptedQuestions)
        assertNull(overall.accuracyPercentage)
    }

    // =========================================================================
    // 5. Consistency Invariant Tests
    // =========================================================================

    @Test
    fun testHierarchicalConsistencyInvariantsAcrossAllLevels() = runBlocking {
        // Attempt a variety of questions across different subjects
        val q1 = questionBankRepository.getQuestionsForSubject("AMAC")[0]
        val q2 = questionBankRepository.getQuestionsForSubject("Economy")[0]
        val q3 = questionBankRepository.getQuestionsForSubject("Economy")[1]
        val q4 = questionBankRepository.getQuestionsForSubject("Polity")[0]

        fakeHistoryRepository.recordAttempt(q1.questionId, q1.correctAnswer, true)
        fakeHistoryRepository.recordAttempt(q2.questionId, q2.correctAnswer, true)
        fakeHistoryRepository.recordAttempt(q3.questionId, "WRONG", false)
        fakeHistoryRepository.recordAttempt(q4.questionId, q4.correctAnswer, true)

        val fullData = analysisRepository.getFullAnalysis()

        // 1. Subject sums equal overall totals
        assertEquals(fullData.overall.totalQuestions, fullData.subjects.sumOf { it.totalQuestions })
        assertEquals(fullData.overall.attemptedQuestions, fullData.subjects.sumOf { it.attemptedQuestions })
        assertEquals(fullData.overall.correctAnswers, fullData.subjects.sumOf { it.correctAnswers })
        assertEquals(fullData.overall.incorrectAnswers, fullData.subjects.sumOf { it.incorrectAnswers })

        // 2. For every subject: microtheme sums equal subject totals
        for (subject in fullData.subjects) {
            val microthemes = fullData.microthemesBySubject[subject.subject].orEmpty()
            assertEquals(subject.totalQuestions, microthemes.sumOf { it.totalQuestions })
            assertEquals(subject.attemptedQuestions, microthemes.sumOf { it.attemptedQuestions })
            assertEquals(subject.correctAnswers, microthemes.sumOf { it.correctAnswers })
            assertEquals(subject.incorrectAnswers, microthemes.sumOf { it.incorrectAnswers })
        }

        // 3. For every record: Attempted = Correct + Incorrect
        assertEquals(fullData.overall.attemptedQuestions, fullData.overall.correctAnswers + fullData.overall.incorrectAnswers)
        for (subject in fullData.subjects) {
            assertEquals(subject.attemptedQuestions, subject.correctAnswers + subject.incorrectAnswers)
        }
        for ((_, microthemes) in fullData.microthemesBySubject) {
            for (m in microthemes) {
                assertEquals(m.attemptedQuestions, m.correctAnswers + m.incorrectAnswers)
            }
        }

        // 4. For every record: Total = Attempted + Unattempted
        assertEquals(fullData.overall.totalQuestions, fullData.overall.attemptedQuestions + fullData.overall.unattemptedQuestions)
        for (subject in fullData.subjects) {
            assertEquals(subject.totalQuestions, subject.attemptedQuestions + subject.unattemptedQuestions)
        }
        for ((_, microthemes) in fullData.microthemesBySubject) {
            for (m in microthemes) {
                assertEquals(m.totalQuestions, m.attemptedQuestions + m.unattemptedQuestions)
            }
        }
    }

    // =========================================================================
    // 6. Unmatched Attempt Handling Test
    // =========================================================================

    @Test
    fun testUnmatchedAttemptIdIsSafelyTrackedAndDoesNotCorruptHierarchy() = runBlocking {
        var warningLogged: String? = null
        val repoWithLogger = AnalysisRepositoryImpl(
            questionBankRepository = questionBankRepository,
            practiceHistoryRepository = fakeHistoryRepository,
            onUnmatchedAttempt = { unmatchedId -> warningLogged = unmatchedId }
        )

        val validQ = questionBankRepository.getAllQuestions().first()
        fakeHistoryRepository.recordAttempt(validQ.questionId, validQ.correctAnswer, true)
        // Record an attempt with an unknown ID
        fakeHistoryRepository.recordAttempt("UNKNOWN_99999", "A", true)

        val fullData = repoWithLogger.getFullAnalysis()

        // Unmatched attempt is recorded
        assertEquals(listOf("UNKNOWN_99999"), fullData.unmatchedAttemptIds)
        assertEquals("UNKNOWN_99999", warningLogged)

        // Overall and subject hierarchy remain strictly consistent based on question bank
        assertEquals(1432, fullData.overall.totalQuestions)
        assertEquals(1, fullData.overall.attemptedQuestions) // Only valid question counted
        assertEquals(1, fullData.overall.correctAnswers)
        assertEquals(fullData.subjects.sumOf { it.attemptedQuestions }, fullData.overall.attemptedQuestions)
    }

    // =========================================================================
    // Fake Repository for Testing
    // =========================================================================

    private class FakeTestPracticeHistoryRepository : PracticeHistoryRepository {
        val attempts = mutableMapOf<String, AttemptedQuestionEntity>()
        private val attemptsFlow = MutableStateFlow<List<AttemptedQuestionEntity>>(emptyList())

        override suspend fun recordAttempt(
            questionId: String,
            selectedAnswer: String,
            isCorrect: Boolean
        ): Result<Unit> {
            val entity = AttemptedQuestionEntity(questionId, selectedAnswer, isCorrect)
            attempts[questionId] = entity
            attemptsFlow.value = attempts.values.toList()
            return Result.success(Unit)
        }

        override suspend fun recordOrUpdateAttempt(
            questionId: String,
            selectedAnswer: String,
            isCorrect: Boolean,
            attemptedAt: Long
        ): Result<Unit> {
            val entity = AttemptedQuestionEntity(questionId, selectedAnswer, isCorrect, attemptedAt)
            attempts[questionId] = entity
            attemptsFlow.value = attempts.values.toList()
            return Result.success(Unit)
        }

        override suspend fun isQuestionAttempted(questionId: String) = attempts.containsKey(questionId)
        override suspend fun getAllAttemptedQuestionIds() = attempts.keys.toList()
        override fun observeAllAttemptedQuestionIds(): Flow<List<String>> = MutableStateFlow(attempts.keys.toList())
        override suspend fun getAttempt(questionId: String) = attempts[questionId]
        override suspend fun getAttemptCount() = attempts.size
        override fun observeAttemptCount(): Flow<Int> = MutableStateFlow(attempts.size)
        override suspend fun getAllAttempts(): List<AttemptedQuestionEntity> = attempts.values.toList()
        override fun observeAllAttempts(): Flow<List<AttemptedQuestionEntity>> = attemptsFlow.asStateFlow()
    }
}
