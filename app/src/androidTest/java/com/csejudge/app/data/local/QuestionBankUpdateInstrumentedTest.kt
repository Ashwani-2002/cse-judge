package com.csejudge.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.model.QuestionBankFile
import com.csejudge.app.data.repository.AnalysisRepositoryImpl
import com.csejudge.app.data.repository.BookmarkRepositoryImpl
import com.csejudge.app.data.repository.PracticeHistoryRepositoryImpl
import com.csejudge.app.data.repository.QuestionBankRepositoryImpl
import com.csejudge.app.data.repository.QuestionBankUpdateValidator
import com.csejudge.app.ui.practice.PracticeSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream

/**
 * Instrumented Version 1 -> Version 2 Question Bank Compatibility Test.
 * Runs on real physical Android device using actual Room SQLite database.
 */
@RunWith(AndroidJUnit4::class)
class QuestionBankUpdateInstrumentedTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private val json = Json { ignoreUnknownKeys = false }

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
        context = ApplicationProvider.getApplicationContext()
        // Use in-memory Room database to execute real SQLite queries and constraints
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testVersion1ToVersion2OnRealRoomDatabase() = runBlocking {
        val practiceHistoryRepo = PracticeHistoryRepositoryImpl(database.practiceDao(), ioDispatcher = Dispatchers.Main)

        // V1 Question Bank (5 questions)
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
            ioDispatcher = Dispatchers.Main
        )
        v1Repo.loadQuestionBank()

        val bookmarkRepo = BookmarkRepositoryImpl(database.bookmarkDao(), v1Repo, ioDispatcher = Dispatchers.Main)
        val v1AnalysisRepo = AnalysisRepositoryImpl(v1Repo, practiceHistoryRepo, ioDispatcher = Dispatchers.Main)

        // Simulate V1 user activity in SQLite
        practiceHistoryRepo.recordAttempt("POL00001", "A", true)
        practiceHistoryRepo.recordAttempt("POL00002", "C", false)
        practiceHistoryRepo.recordAttempt("ECO00001", "B", false)
        val reattemptTime = System.currentTimeMillis() + 500
        practiceHistoryRepo.recordOrUpdateAttempt("ECO00001", "D", true, attemptedAt = reattemptTime)
        bookmarkRepo.addBookmark("POL00001")
        bookmarkRepo.addBookmark("POL00003")

        // Verify V1 database state
        assertEquals(3, practiceHistoryRepo.getAttemptCount())
        assertEquals(2, bookmarkRepo.getBookmarkCount())

        // V2 Question Bank (9 questions with new microtheme, new subject, and reordered entries)
        val v2Questions = listOf(
            createQ("ETH00001", "Ethics", "Human Values", correct = "B"),
            createQ("POL00003", "Polity", "Fundamental Rights", correct = "C"),
            createQ("ECO00002", "Economy", "Fiscal Policy", correct = "A"),
            createQ("POL00001", "Polity", "Fundamental Rights", correct = "A"),
            createQ("ENV00001", "Environment", "Ecology", correct = "A"),
            createQ("ECO00001", "Economy", "Banking System", correct = "D"),
            createQ("ETH00002", "Ethics", "Human Values", correct = "C"),
            createQ("POL00004", "Polity", "Fundamental Rights", correct = "D"),
            createQ("POL00002", "Polity", "Fundamental Rights", correct = "B")
        )

        // Validate update using QuestionBankUpdateValidator
        val validation = QuestionBankUpdateValidator.validateUpdate(v1Questions, v2Questions)
        assertTrue(validation.isValidForRelease)

        // V2 Repository using the SAME database instance
        val v2Repo = QuestionBankRepositoryImpl(
            openAssetStream = { serializeToInputStream(v2Questions) },
            fileNames = listOf("v2.json"),
            ioDispatcher = Dispatchers.Main
        )
        v2Repo.loadQuestionBank()

        val v2BookmarkRepo = BookmarkRepositoryImpl(database.bookmarkDao(), v2Repo, ioDispatcher = Dispatchers.Main)
        val v2AnalysisRepo = AnalysisRepositoryImpl(v2Repo, practiceHistoryRepo, ioDispatcher = Dispatchers.Main)

        // ====================================================================
        // STEP 4: VERIFY ALL 17 CRITERIA ON REAL ROOM SQLITE DATABASE
        // ====================================================================

        // 1. All old attempts remain intact
        assertTrue(practiceHistoryRepo.isQuestionAttempted("POL00001"))
        assertTrue(practiceHistoryRepo.isQuestionAttempted("POL00002"))
        assertTrue(practiceHistoryRepo.isQuestionAttempted("ECO00001"))
        val pol1Attempt = practiceHistoryRepo.getAttempt("POL00001")
        assertNotNull(pol1Attempt)
        assertEquals("A", pol1Attempt?.selectedAnswer)
        assertTrue(pol1Attempt?.isCorrect == true)
        val pol2Attempt = practiceHistoryRepo.getAttempt("POL00002")
        assertNotNull(pol2Attempt)
        assertEquals("C", pol2Attempt?.selectedAnswer)
        assertFalse(pol2Attempt?.isCorrect == true)

        // 2. All old bookmarks remain intact
        assertTrue(v2BookmarkRepo.isBookmarked("POL00001"))
        assertTrue(v2BookmarkRepo.isBookmarked("POL00003"))
        assertEquals(2, v2BookmarkRepo.getBookmarkCount())

        // 3. All reattempt records remain intact
        val ecoAttempt = practiceHistoryRepo.getAttempt("ECO00001")
        assertNotNull(ecoAttempt)
        assertEquals("D", ecoAttempt?.selectedAnswer)
        assertTrue(ecoAttempt?.isCorrect == true)
        assertEquals(reattemptTime, ecoAttempt?.attemptedAt)

        // 4. No duplicate attempt records exist
        assertEquals("Attempt count must strictly remain 3", 3, practiceHistoryRepo.getAttemptCount())
        val allAttemptIds = practiceHistoryRepo.getAllAttemptedQuestionIds()
        assertEquals("No duplicate IDs in attempts", allAttemptIds.distinct().size, allAttemptIds.size)

        // 5. No duplicate bookmark records exist
        assertEquals("Bookmark count must strictly remain 2", 2, v2BookmarkRepo.getBookmarkCount())
        val allBookmarkIds = v2BookmarkRepo.getAllBookmarkedQuestionIds()
        assertEquals("No duplicate IDs in bookmarks", allBookmarkIds.distinct().size, allBookmarkIds.size)

        // 6. All new questions are loaded
        assertNotNull(v2Repo.getQuestionById("POL00004"))
        assertNotNull(v2Repo.getQuestionById("ECO00002"))
        assertNotNull(v2Repo.getQuestionById("ETH00001"))
        assertNotNull(v2Repo.getQuestionById("ETH00002"))
        assertEquals(9, v2Repo.getAllQuestions().size)

        // 7. All new questions are initially unattempted
        assertFalse(practiceHistoryRepo.isQuestionAttempted("POL00004"))
        assertFalse(practiceHistoryRepo.isQuestionAttempted("ECO00002"))
        assertFalse(practiceHistoryRepo.isQuestionAttempted("ETH00001"))
        assertFalse(practiceHistoryRepo.isQuestionAttempted("ETH00002"))

        // 8. All new questions are initially unbookmarked
        assertFalse(v2BookmarkRepo.isBookmarked("POL00004"))
        assertFalse(v2BookmarkRepo.isBookmarked("ECO00002"))
        assertFalse(v2BookmarkRepo.isBookmarked("ETH00001"))
        assertFalse(v2BookmarkRepo.isBookmarked("ETH00002"))

        // 9. New questions appear in practice
        val attempted = practiceHistoryRepo.getAllAttemptedQuestionIds().toSet()
        val unattempted = v2Repo.getAllQuestions().filter { it.questionId !in attempted }
        assertEquals(6, unattempted.size)
        val unattemptedIds = unattempted.map { it.questionId }.toSet()
        assertTrue(unattemptedIds.contains("POL00004"))
        assertTrue(unattemptedIds.contains("ECO00002"))
        assertTrue(unattemptedIds.contains("ETH00001"))
        assertTrue(unattemptedIds.contains("ETH00002"))
        assertTrue(unattemptedIds.contains("POL00003"))
        assertTrue(unattemptedIds.contains("ENV00001"))
        val session = PracticeSession(unattempted)
        assertFalse(session.isExhausted)
        assertNotNull(session.currentQuestion)
        assertTrue(session.currentQuestion?.questionId in unattemptedIds)

        // 10. Previously attempted questions remain excluded from normal practice
        assertFalse(unattemptedIds.contains("POL00001"))
        assertFalse(unattemptedIds.contains("POL00002"))
        assertFalse(unattemptedIds.contains("ECO00001"))

        // 11. New subjects appear automatically
        assertTrue(v2Repo.getAvailableSubjects().contains("Ethics"))

        // 12. New microthemes appear automatically
        assertTrue(v2Repo.getAvailableMicrothemes("Economy").contains("Fiscal Policy"))
        assertTrue(v2Repo.getAvailableMicrothemes("Ethics").contains("Human Values"))

        // 13. Analysis reflects the new question-bank totals
        val v2Analysis = v2AnalysisRepo.getFullAnalysis()
        assertEquals(9, v2Analysis.overall.totalQuestions)
        assertEquals(3, v2Analysis.overall.attemptedQuestions)
        assertEquals(6, v2Analysis.overall.unattemptedQuestions)

        val ethicsAnalysis = v2Analysis.subjects.find { it.subject == "Ethics" }
        assertNotNull("Ethics subject analysis must exist", ethicsAnalysis)
        assertEquals(2, ethicsAnalysis?.totalQuestions)
        assertEquals(0, ethicsAnalysis?.attemptedQuestions)
        assertEquals(2, ethicsAnalysis?.unattemptedQuestions)

        // 14. Existing coverage and accuracy calculations remain correct
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

        // 15. Reordering JSON entries does not affect identity or user history
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
            val q2 = v2Repo.getQuestionById(q.questionId)
            assertNotNull(q2)
            assertEquals(q.subject, q2?.subject)
            assertEquals(q.correctAnswer, q2?.correctAnswer)
        }

        // 16. Removing a question from the new JSON does not erase its old Room records
        val v3QuestionsWithoutPol1 = v2Questions.filter { it.questionId != "POL00001" }
        val v3Validation = QuestionBankUpdateValidator.validateUpdate(v2Questions, v3QuestionsWithoutPol1)
        assertFalse(v3Validation.isValidForRelease)
        assertTrue(v3Validation.missingQuestionIds.contains("POL00001"))

        val v3Repo = QuestionBankRepositoryImpl(
            openAssetStream = { serializeToInputStream(v3QuestionsWithoutPol1) },
            fileNames = listOf("v3.json"),
            ioDispatcher = Dispatchers.Main
        )
        v3Repo.loadQuestionBank()
        assertNull(v3Repo.getQuestionById("POL00001"))

        assertTrue("Attempt record in SQLite must persist", practiceHistoryRepo.isQuestionAttempted("POL00001"))
        assertNotNull(practiceHistoryRepo.getAttempt("POL00001"))
        assertEquals(3, practiceHistoryRepo.getAttemptCount())
        assertEquals(2, database.bookmarkDao().getBookmarkCount())
        assertTrue(database.bookmarkDao().isBookmarked("POL00001"))

        val v3AnalysisRepo = AnalysisRepositoryImpl(v3Repo, practiceHistoryRepo, ioDispatcher = Dispatchers.Main)
        val v3Analysis = v3AnalysisRepo.getFullAnalysis()
        assertTrue(v3Analysis.unmatchedAttemptIds.contains("POL00001"))
        assertEquals(3, practiceHistoryRepo.getAttemptCount())

        // 17. A repeated update does not create duplicate records
        val repeatedRepo = QuestionBankRepositoryImpl(
            openAssetStream = { serializeToInputStream(v2Questions) },
            fileNames = listOf("v2_repeat.json"),
            ioDispatcher = Dispatchers.Main
        )
        repeatedRepo.loadQuestionBank()

        practiceHistoryRepo.recordOrUpdateAttempt("POL00001", "A", true)
        assertEquals("Attempt count must strictly remain 3", 3, practiceHistoryRepo.getAttemptCount())
        val uniqueAttemptIds = practiceHistoryRepo.getAllAttemptedQuestionIds().distinct()
        assertEquals("Attempt IDs in SQLite must be completely unique", 3, uniqueAttemptIds.size)

        v2BookmarkRepo.addBookmark("POL00001")
        assertEquals("Bookmark count must strictly remain 2", 2, v2BookmarkRepo.getBookmarkCount())
        val uniqueBookmarkIds = v2BookmarkRepo.getAllBookmarkedQuestionIds().distinct()
        assertEquals("Bookmark IDs in SQLite must be completely unique", 2, uniqueBookmarkIds.size)
    }
}
