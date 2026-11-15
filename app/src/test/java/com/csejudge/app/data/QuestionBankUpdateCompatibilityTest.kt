package com.csejudge.app.data

import android.database.sqlite.SQLiteConstraintException
import com.csejudge.app.data.local.AttemptedQuestionEntity
import com.csejudge.app.data.local.BookmarkDao
import com.csejudge.app.data.local.BookmarkedQuestionEntity
import com.csejudge.app.data.local.PracticeDao
import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.model.QuestionBankFile
import com.csejudge.app.data.repository.AnalysisRepositoryImpl
import com.csejudge.app.data.repository.BookmarkRepository
import com.csejudge.app.data.repository.BookmarkRepositoryImpl
import com.csejudge.app.data.repository.PracticeHistoryRepository
import com.csejudge.app.data.repository.PracticeHistoryRepositoryImpl
import com.csejudge.app.data.repository.QuestionBankRepositoryImpl
import com.csejudge.app.data.repository.QuestionBankUpdateValidator
import com.csejudge.app.ui.practice.PracticeSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * Mandatory Version 1 -> Version 2 Question Bank Compatibility Simulation Test.
 *
 * Verifies that updating the question bank (adding new questions, new microthemes,
 * new subjects, and reordering questions in JSON) preserves all local user data in Room
 * without data loss, duplicates, or corrupted analysis metrics.
 */
class QuestionBankUpdateCompatibilityTest {

    private val testDispatcher = StandardTestDispatcher()
    private val json = Json { ignoreUnknownKeys = false }

    private lateinit var fakePracticeDao: TestPracticeDao
    private lateinit var fakeBookmarkDao: TestBookmarkDao
    private lateinit var practiceHistoryRepo: PracticeHistoryRepository
    private lateinit var bookmarkRepo: BookmarkRepository

    private fun createQ(
        id: String,
        subject: String,
        microtheme: String,
        text: String = "Question text for $id",
        correct: String = "A"
    ): Question {
        return Question(
            questionId = id,
            subject = subject,
            microtheme = microtheme,
            question = listOf(text),
            options = Options("Option A", "Option B", "Option C", "Option D"),
            correctAnswer = correct,
            solution = listOf("Solution for $id")
        )
    }

    private fun serializeToInputStream(questions: List<Question>) =
        ByteArrayInputStream(json.encodeToString(QuestionBankFile.serializer(), QuestionBankFile(questions)).toByteArray())

    @Before
    fun setUp() {
        fakePracticeDao = TestPracticeDao()
        fakeBookmarkDao = TestBookmarkDao()
        practiceHistoryRepo = PracticeHistoryRepositoryImpl(fakePracticeDao, ioDispatcher = testDispatcher)
    }

    @Test
    fun testVersion1ToVersion2CompatibilitySimulation() = runTest(testDispatcher) {
        // ====================================================================
        // STEP 1: VERSION 1 FIXTURE SETUP
        // ====================================================================
        // Version 1 contains:
        // - Polity: Fundamental Rights (POL00001, POL00002, POL00003)
        // - Economy: Banking System (ECO00001)
        // - Environment: Ecology (ENV00001)
        // Total: 5 questions across 3 subjects and 3 microthemes
        val v1Questions = listOf(
            createQ("POL00001", "Polity", "Fundamental Rights", correct = "A"),
            createQ("POL00002", "Polity", "Fundamental Rights", correct = "B"),
            createQ("POL00003", "Polity", "Fundamental Rights", correct = "C"),
            createQ("ECO00001", "Economy", "Banking System", correct = "D"),
            createQ("ENV00001", "Environment", "Ecology", correct = "A")
        )

        val v1Repo = QuestionBankRepositoryImpl(
            openAssetStream = { serializeToInputStream(v1Questions) },
            fileNames = listOf("v1.json"),
            ioDispatcher = testDispatcher
        )
        val v1Stats = v1Repo.loadQuestionBank()
        assertEquals(5, v1Stats.totalQuestions)

        bookmarkRepo = BookmarkRepositoryImpl(fakeBookmarkDao, v1Repo, ioDispatcher = testDispatcher)
        val v1AnalysisRepo = AnalysisRepositoryImpl(v1Repo, practiceHistoryRepo, ioDispatcher = testDispatcher)

        // ====================================================================
        // STEP 2: SIMULATE USER ACTIVITY IN VERSION 1
        // ====================================================================
        // 1. POL00001: Correct attempt
        practiceHistoryRepo.recordAttempt("POL00001", "A", true)
        // 2. POL00002: Incorrect attempt
        practiceHistoryRepo.recordAttempt("POL00002", "D", false)
        // 3. ECO00001: First answered incorrectly ('A', false), then reattempted correctly ('D', true)
        practiceHistoryRepo.recordAttempt("ECO00001", "A", false)
        val reattemptTime = System.currentTimeMillis() + 1000
        practiceHistoryRepo.recordOrUpdateAttempt("ECO00001", "D", true, attemptedAt = reattemptTime)
        // 4. POL00001 and POL00003: Bookmarked
        bookmarkRepo.addBookmark("POL00001")
        bookmarkRepo.addBookmark("POL00003")
        // POL00003 and ENV00001 remain unattempted

        // Verify V1 State before update
        assertEquals(3, practiceHistoryRepo.getAttemptCount())
        assertEquals(2, bookmarkRepo.getBookmarkCount())
        val initialV1Analysis = v1AnalysisRepo.getFullAnalysis()
        assertEquals(5, initialV1Analysis.overall.totalQuestions)
        assertEquals(3, initialV1Analysis.overall.attemptedQuestions)
        assertEquals(2, initialV1Analysis.overall.unattemptedQuestions)
        assertEquals(60.0, initialV1Analysis.overall.coveragePercentage, 0.01)
        assertEquals(2, initialV1Analysis.overall.correctAnswers) // POL00001 and ECO00001
        assertEquals(1, initialV1Analysis.overall.incorrectAnswers) // POL00002
        assertEquals(66.67, initialV1Analysis.overall.accuracyPercentage!!, 0.01)

        // Verify V1 Practice Session: unsolved pool excludes POL00001, POL00002, ECO00001
        val v1AttemptedIds = practiceHistoryRepo.getAllAttemptedQuestionIds().toSet()
        val v1Unattempted = v1Repo.getAllQuestions().filter { it.questionId !in v1AttemptedIds }
        assertEquals(2, v1Unattempted.size)
        val v1Session = PracticeSession(v1Unattempted)
        assertFalse(v1Session.isExhausted)
        assertNotNull(v1Session.currentQuestion)
        assertTrue(v1Session.currentQuestion?.questionId in listOf("POL00003", "ENV00001"))

        // ====================================================================
        // STEP 3: VERSION 2 FIXTURE SETUP
        // ====================================================================
        // Version 2 requirements:
        // - Preserves every Version 1 question ID and content
        // - Adds question to existing microtheme (POL00004 to Polity/Fundamental Rights)
        // - Adds new microtheme to existing subject (ECO00002 to Economy/Fiscal Policy)
        // - Adds new subject with new microtheme (ETH00001, ETH00002 to Ethics/Human Values)
        // - Reorders questions arbitrarily in the JSON array
        // Total: 9 questions across 4 subjects and 5 microthemes
        val v2Questions = listOf(
            createQ("ETH00001", "Ethics", "Human Values", correct = "B"),             // New subject, index 0
            createQ("POL00003", "Polity", "Fundamental Rights", correct = "C"),       // V1 question reordered to index 1
            createQ("ECO00002", "Economy", "Fiscal Policy", correct = "A"),           // New microtheme, index 2
            createQ("POL00001", "Polity", "Fundamental Rights", correct = "A"),       // V1 question reordered to index 3
            createQ("ENV00001", "Environment", "Ecology", correct = "A"),             // V1 question reordered to index 4
            createQ("ECO00001", "Economy", "Banking System", correct = "D"),           // V1 question reordered to index 5
            createQ("ETH00002", "Ethics", "Human Values", correct = "C"),             // New subject, index 6
            createQ("POL00004", "Polity", "Fundamental Rights", correct = "D"),       // New question in existing microtheme, index 7
            createQ("POL00002", "Polity", "Fundamental Rights", correct = "B")        // V1 question reordered to index 8
        )

        // Validate update using QuestionBankUpdateValidator
        val updateValidation = QuestionBankUpdateValidator.validateUpdate(v1Questions, v2Questions)
        assertTrue("Update must be valid for release", updateValidation.isValidForRelease)
        assertEquals(5, updateValidation.preservedQuestionsCount)
        assertEquals(4, updateValidation.newlyAddedQuestions.size)
        assertEquals(setOf("Ethics"), updateValidation.newlyAddedSubjects)
        assertEquals(setOf("Fiscal Policy"), updateValidation.newlyAddedMicrothemes["Economy"])

        // Load Version 2 QuestionBankRepository (simulating app start after update)
        val v2Repo = QuestionBankRepositoryImpl(
            openAssetStream = { serializeToInputStream(v2Questions) },
            fileNames = listOf("v2.json"),
            ioDispatcher = testDispatcher
        )
        val v2Stats = v2Repo.loadQuestionBank()
        assertEquals(9, v2Stats.totalQuestions)

        // IMPORTANT: Wire repositories using the SAME Room database/DAO instances
        // DO NOT reset, clear, or recreate the DAOs!
        val v2BookmarkRepo = BookmarkRepositoryImpl(fakeBookmarkDao, v2Repo, ioDispatcher = testDispatcher)
        val v2AnalysisRepo = AnalysisRepositoryImpl(v2Repo, practiceHistoryRepo, ioDispatcher = testDispatcher)

        // ====================================================================
        // STEP 4: VERIFY ALL 17 MANDATORY COMPATIBILITY CRITERIA
        // ====================================================================

        // Criterion 1: All old attempts remain intact
        assertTrue("POL00001 must still be attempted", practiceHistoryRepo.isQuestionAttempted("POL00001"))
        assertTrue("POL00002 must still be attempted", practiceHistoryRepo.isQuestionAttempted("POL00002"))
        assertTrue("ECO00001 must still be attempted", practiceHistoryRepo.isQuestionAttempted("ECO00001"))
        val pol1Attempt = practiceHistoryRepo.getAttempt("POL00001")
        assertNotNull(pol1Attempt)
        assertEquals("A", pol1Attempt?.selectedAnswer)
        assertTrue(pol1Attempt?.isCorrect == true)

        val pol2Attempt = practiceHistoryRepo.getAttempt("POL00002")
        assertNotNull(pol2Attempt)
        assertEquals("D", pol2Attempt?.selectedAnswer)
        assertFalse(pol2Attempt?.isCorrect == true)

        // Criterion 2: All old bookmarks remain intact
        assertTrue("POL00001 must still be bookmarked", v2BookmarkRepo.isBookmarked("POL00001"))
        assertTrue("POL00003 must still be bookmarked", v2BookmarkRepo.isBookmarked("POL00003"))
        assertEquals(2, v2BookmarkRepo.getBookmarkCount())

        // Criterion 3: Reattempt records remain correct
        val ecoAttempt = practiceHistoryRepo.getAttempt("ECO00001")
        assertNotNull(ecoAttempt)
        assertEquals("D", ecoAttempt?.selectedAnswer)
        assertTrue("Reattempted answer must be marked correct", ecoAttempt?.isCorrect == true)
        assertEquals(reattemptTime, ecoAttempt?.attemptedAt)

        // Criterion 4: No duplicate attempt records exist
        assertEquals("Attempt count must strictly remain 3", 3, practiceHistoryRepo.getAttemptCount())
        val allAttemptIds = practiceHistoryRepo.getAllAttemptedQuestionIds()
        assertEquals("No duplicate IDs in attempts", allAttemptIds.distinct().size, allAttemptIds.size)

        // Criterion 5: No duplicate bookmark records exist
        assertEquals("Bookmark count must strictly remain 2", 2, v2BookmarkRepo.getBookmarkCount())
        val allBookmarkIds = v2BookmarkRepo.getAllBookmarkedQuestionIds()
        assertEquals("No duplicate IDs in bookmarks", allBookmarkIds.distinct().size, allBookmarkIds.size)

        // Criterion 6: All new questions are loaded
        assertNotNull("POL00004 must be present in bank", v2Repo.getQuestionById("POL00004"))
        assertNotNull("ECO00002 must be present in bank", v2Repo.getQuestionById("ECO00002"))
        assertNotNull("ETH00001 must be present in bank", v2Repo.getQuestionById("ETH00001"))
        assertNotNull("ETH00002 must be present in bank", v2Repo.getQuestionById("ETH00002"))
        assertEquals(9, v2Repo.getAllQuestions().size)

        // Criterion 7: All new questions are initially unattempted
        assertFalse("New question POL00004 must be unattempted", practiceHistoryRepo.isQuestionAttempted("POL00004"))
        assertFalse("New question ECO00002 must be unattempted", practiceHistoryRepo.isQuestionAttempted("ECO00002"))
        assertFalse("New question ETH00001 must be unattempted", practiceHistoryRepo.isQuestionAttempted("ETH00001"))
        assertFalse("New question ETH00002 must be unattempted", practiceHistoryRepo.isQuestionAttempted("ETH00002"))

        // Criterion 8: All new questions are initially unbookmarked
        assertFalse("New question POL00004 must be unbookmarked", v2BookmarkRepo.isBookmarked("POL00004"))
        assertFalse("New question ECO00002 must be unbookmarked", v2BookmarkRepo.isBookmarked("ECO00002"))
        assertFalse("New question ETH00001 must be unbookmarked", v2BookmarkRepo.isBookmarked("ETH00001"))
        assertFalse("New question ETH00002 must be unbookmarked", v2BookmarkRepo.isBookmarked("ETH00002"))

        // Criterion 9: New questions appear in practice
        val v2AttemptedIds = practiceHistoryRepo.getAllAttemptedQuestionIds().toSet()
        val v2Unattempted = v2Repo.getAllQuestions().filter { it.questionId !in v2AttemptedIds }
        // Total unsolved: 9 total - 3 attempted = 6 unsolved
        assertEquals(6, v2Unattempted.size)
        val v2UnattemptedIds = v2Unattempted.map { it.questionId }.toSet()
        assertTrue(v2UnattemptedIds.contains("POL00004"))
        assertTrue(v2UnattemptedIds.contains("ECO00002"))
        assertTrue(v2UnattemptedIds.contains("ETH00001"))
        assertTrue(v2UnattemptedIds.contains("ETH00002"))
        assertTrue(v2UnattemptedIds.contains("POL00003")) // Old unattempted
        assertTrue(v2UnattemptedIds.contains("ENV00001")) // Old unattempted

        val v2Session = PracticeSession(v2Unattempted)
        assertFalse(v2Session.isExhausted)
        assertNotNull(v2Session.currentQuestion)
        assertTrue(v2Session.currentQuestion?.questionId in v2UnattemptedIds)

        // Criterion 10: Previously attempted questions remain excluded from normal practice
        assertFalse(v2UnattemptedIds.contains("POL00001"))
        assertFalse(v2UnattemptedIds.contains("POL00002"))
        assertFalse(v2UnattemptedIds.contains("ECO00001"))

        // Criterion 11: New subjects appear automatically
        val availableSubjects = v2Repo.getAvailableSubjects()
        assertTrue("New subject Ethics must appear in subjects", availableSubjects.contains("Ethics"))

        // Criterion 12: New microthemes appear automatically
        val economyMicrothemes = v2Repo.getAvailableMicrothemes("Economy")
        assertTrue("New microtheme Fiscal Policy must appear in Economy", economyMicrothemes.contains("Fiscal Policy"))
        val ethicsMicrothemes = v2Repo.getAvailableMicrothemes("Ethics")
        assertTrue("Human Values must appear in Ethics", ethicsMicrothemes.contains("Human Values"))

        // Criterion 13: Analysis reflects the new question-bank totals
        val v2Analysis = v2AnalysisRepo.getFullAnalysis()
        assertEquals(9, v2Analysis.overall.totalQuestions)
        assertEquals(3, v2Analysis.overall.attemptedQuestions)
        assertEquals(6, v2Analysis.overall.unattemptedQuestions)

        val ethicsAnalysis = v2Analysis.subjects.find { it.subject == "Ethics" }
        assertNotNull("Ethics subject analysis must exist", ethicsAnalysis)
        assertEquals(2, ethicsAnalysis?.totalQuestions)
        assertEquals(0, ethicsAnalysis?.attemptedQuestions)
        assertEquals(2, ethicsAnalysis?.unattemptedQuestions)
        assertEquals(0.0, ethicsAnalysis?.coveragePercentage ?: 0.0, 0.01)
        assertNull("Unattempted subject must have null / N/A accuracy", ethicsAnalysis?.accuracyPercentage)

        // Criterion 14: Existing coverage and accuracy calculations remain correct
        // In V1: 3 attempted / 5 total = 60.0% coverage; 2 correct / 3 attempted = 66.67% accuracy
        // In V2: 3 attempted / 9 total = 33.33% coverage; 2 correct / 3 attempted = 66.67% accuracy
        assertEquals(33.33, v2Analysis.overall.coveragePercentage, 0.01)
        assertEquals(2, v2Analysis.overall.correctAnswers)
        assertEquals(1, v2Analysis.overall.incorrectAnswers)
        assertEquals(66.67, v2Analysis.overall.accuracyPercentage!!, 0.01)

        val polityAnalysis = v2Analysis.subjects.find { it.subject == "Polity" }
        assertNotNull(polityAnalysis)
        assertEquals(4, polityAnalysis?.totalQuestions)
        assertEquals(2, polityAnalysis?.attemptedQuestions)
        assertEquals(50.0, polityAnalysis?.coveragePercentage ?: 0.0, 0.01)
        assertEquals(50.0, polityAnalysis?.accuracyPercentage ?: 0.0, 0.01)

        // Criterion 15: Reordering JSON entries does not affect identity or user history
        // POL00001 was at index 0 in V1, now index 3 in V2
        val pol1 = v2Repo.getQuestionById("POL00001")
        assertNotNull(pol1)
        assertEquals("Polity", pol1?.subject)
        assertEquals("Fundamental Rights", pol1?.microtheme)
        assertEquals("A", pol1?.correctAnswer)
        val pol1AttemptAfterReorder = practiceHistoryRepo.getAttempt("POL00001")
        assertEquals("A", pol1AttemptAfterReorder?.selectedAnswer)
        assertTrue(pol1AttemptAfterReorder?.isCorrect == true)
        assertTrue(v2BookmarkRepo.isBookmarked("POL00001"))

        for (q in v1Questions) {
            val loaded = v2Repo.getQuestionById(q.questionId)
            assertNotNull("Question ${q.questionId} must exist in V2", loaded)
            assertEquals("Subject must match for ${q.questionId}", q.subject, loaded?.subject)
            assertEquals("Microtheme must match for ${q.questionId}", q.microtheme, loaded?.microtheme)
            assertEquals("Correct answer must match for ${q.questionId}", q.correctAnswer, loaded?.correctAnswer)
        }

        // Criterion 16: Removing a question from the new JSON does not erase its old Room records
        val v3QuestionsWithoutPol1 = v2Questions.filter { it.questionId != "POL00001" }
        val v3Validation = QuestionBankUpdateValidator.validateUpdate(v2Questions, v3QuestionsWithoutPol1)
        assertFalse("Validator must reject removal of existing questions", v3Validation.isValidForRelease)
        assertTrue("POL00001 must be flagged as missing", v3Validation.missingQuestionIds.contains("POL00001"))

        val v3Repo = QuestionBankRepositoryImpl(
            openAssetStream = { serializeToInputStream(v3QuestionsWithoutPol1) },
            fileNames = listOf("v3.json"),
            ioDispatcher = testDispatcher
        )
        v3Repo.loadQuestionBank()
        assertNull(v3Repo.getQuestionById("POL00001"))

        // Room records for POL00001 must remain completely intact
        assertTrue("Attempt record in Room must persist", practiceHistoryRepo.isQuestionAttempted("POL00001"))
        assertNotNull(practiceHistoryRepo.getAttempt("POL00001"))
        assertEquals(3, practiceHistoryRepo.getAttemptCount())
        assertEquals(2, fakeBookmarkDao.getBookmarkCount())
        assertTrue(fakeBookmarkDao.isBookmarked("POL00001"))

        // Analysis gracefully handles unmatched attempt ID without crashing or modifying Room
        val v3AnalysisRepo = AnalysisRepositoryImpl(v3Repo, practiceHistoryRepo, ioDispatcher = testDispatcher)
        val v3Analysis = v3AnalysisRepo.getFullAnalysis()
        assertTrue("Unmatched attempt ID must be reported", v3Analysis.unmatchedAttemptIds.contains("POL00001"))
        assertEquals(3, practiceHistoryRepo.getAttemptCount())

        // Criterion 17: A repeated update does not create duplicate records
        val repeatedRepo = QuestionBankRepositoryImpl(
            openAssetStream = { serializeToInputStream(v2Questions) },
            fileNames = listOf("v2_repeat.json"),
            ioDispatcher = testDispatcher
        )
        val repeatedStats = repeatedRepo.loadQuestionBank()
        assertEquals(9, repeatedStats.totalQuestions)

        practiceHistoryRepo.recordOrUpdateAttempt("POL00001", "A", true)
        assertEquals("Attempt count must strictly remain 3", 3, practiceHistoryRepo.getAttemptCount())
        val uniqueAttemptIds = practiceHistoryRepo.getAllAttemptedQuestionIds().distinct()
        assertEquals("Attempt IDs must be completely unique", 3, uniqueAttemptIds.size)

        v2BookmarkRepo.addBookmark("POL00001")
        assertEquals("Bookmark count must strictly remain 2", 2, v2BookmarkRepo.getBookmarkCount())
        val uniqueBookmarkIds = v2BookmarkRepo.getAllBookmarkedQuestionIds().distinct()
        assertEquals("Bookmark IDs must be completely unique", 2, uniqueBookmarkIds.size)
    }

    // ====================================================================
    // Test DAO Fakes
    // ====================================================================

    private class TestPracticeDao : PracticeDao {
        private val attempts = mutableMapOf<String, AttemptedQuestionEntity>()

        override suspend fun insertAttempt(attempt: AttemptedQuestionEntity) {
            if (attempts.containsKey(attempt.questionId)) {
                throw SQLiteConstraintException("UNIQUE constraint failed: attempted_questions.question_id")
            }
            attempts[attempt.questionId] = attempt
        }

        override suspend fun upsertAttempt(attempt: AttemptedQuestionEntity) {
            attempts[attempt.questionId] = attempt
        }

        override suspend fun isQuestionAttempted(questionId: String): Boolean =
            attempts.containsKey(questionId)

        override suspend fun getAllAttemptedQuestionIds(): List<String> =
            attempts.keys.toList()

        override fun observeAllAttemptedQuestionIds(): Flow<List<String>> =
            flowOf(attempts.keys.toList())

        override suspend fun getAttempt(questionId: String): AttemptedQuestionEntity? =
            attempts[questionId]

        override suspend fun getAttemptCount(): Int =
            attempts.size

        override fun observeAttemptCount(): Flow<Int> =
            flowOf(attempts.size)

        override suspend fun getAllAttempts(): List<AttemptedQuestionEntity> =
            attempts.values.toList()

        override fun observeAllAttempts(): Flow<List<AttemptedQuestionEntity>> =
            flowOf(attempts.values.toList())
    }

    private class TestBookmarkDao : BookmarkDao {
        private val bookmarks = mutableMapOf<String, BookmarkedQuestionEntity>()

        override suspend fun insertBookmark(bookmark: BookmarkedQuestionEntity) {
            bookmarks[bookmark.questionId] = bookmark
        }

        override suspend fun deleteBookmark(questionId: String): Int {
            return if (bookmarks.remove(questionId) != null) 1 else 0
        }

        override suspend fun isBookmarked(questionId: String): Boolean =
            bookmarks.containsKey(questionId)

        override suspend fun getAllBookmarkedQuestionIds(): List<String> =
            bookmarks.values.sortedByDescending { it.bookmarkedAt }.map { it.questionId }

        override fun observeAllBookmarkedQuestionIds(): Flow<List<String>> =
            flowOf(bookmarks.values.sortedByDescending { it.bookmarkedAt }.map { it.questionId })

        override suspend fun getAllBookmarks(): List<BookmarkedQuestionEntity> =
            bookmarks.values.sortedByDescending { it.bookmarkedAt }

        override fun observeAllBookmarks(): Flow<List<BookmarkedQuestionEntity>> =
            flowOf(bookmarks.values.sortedByDescending { it.bookmarkedAt })

        override suspend fun getBookmarkCount(): Int =
            bookmarks.size

        override fun observeBookmarkCount(): Flow<Int> =
            flowOf(bookmarks.size)
    }
}
