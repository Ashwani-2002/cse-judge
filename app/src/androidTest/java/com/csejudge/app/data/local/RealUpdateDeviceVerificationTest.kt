package com.csejudge.app.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.csejudge.app.data.repository.AnalysisRepositoryImpl
import com.csejudge.app.data.repository.BookmarkRepositoryImpl
import com.csejudge.app.data.repository.PracticeHistoryRepositoryImpl
import com.csejudge.app.data.repository.QuestionBankRepository
import com.csejudge.app.data.repository.QuestionBankRepositoryImpl
import com.csejudge.app.ui.practice.PracticeSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-Device Verification Test for Real In-Place APK Updates.
 * Executes on the physical device (Realme Narzo 90 5G / RMX5111, Android 16)
 * against the persistent Room SQLite database and packaged APK assets.
 */
@RunWith(AndroidJUnit4::class)
class RealUpdateDeviceVerificationTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var qbRepository: QuestionBankRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getInstance(context)
        qbRepository = QuestionBankRepositoryImpl.create(context)
    }

    /**
     * Phase 1: Seed active user progress and verify Version 1 baseline state.
     */
    @Test
    fun seedAndVerifyVersion1() = runBlocking {
        val stats = qbRepository.loadQuestionBank()
        println("=== PHASE 1: VERSION 1 BASELINE VERIFICATION ===")
        println("V1 Questions loaded from APK assets: ${stats.totalQuestions}")
        println("V1 Available subjects: ${qbRepository.getAvailableSubjects()}")

        assertEquals("Version 1 must contain exactly 1,432 questions", 1432, stats.totalQuestions)
        assertEquals("Version 1 must contain exactly 7 subjects", 7, qbRepository.getAvailableSubjects().size)

        val practiceDao = database.practiceDao()
        val bookmarkDao = database.bookmarkDao()
        val practiceHistoryRepo = PracticeHistoryRepositoryImpl(practiceDao)
        val bookmarkRepo = BookmarkRepositoryImpl(bookmarkDao, qbRepository)
        val analysisRepo = AnalysisRepositoryImpl(qbRepository, practiceHistoryRepo)

        // Seed user activity: POL00001 correct, POL00002 incorrect, ECO00001 initial incorrect then reattempt correct
        practiceHistoryRepo.recordOrUpdateAttempt("POL00001", "C", true)
        practiceHistoryRepo.recordOrUpdateAttempt("POL00002", "D", false)
        practiceHistoryRepo.recordOrUpdateAttempt("ECO00001", "B", false)
        practiceHistoryRepo.recordOrUpdateAttempt("ECO00001", "D", true)

        if (!bookmarkRepo.isBookmarked("POL00001")) {
            bookmarkRepo.addBookmark("POL00001")
        }
        if (!bookmarkRepo.isBookmarked("POL00003")) {
            bookmarkRepo.addBookmark("POL00003")
        }

        val attemptCount = practiceHistoryRepo.getAttemptCount()
        val bookmarkCount = bookmarkRepo.getBookmarkCount()
        println("SQLite V1 Attempt Count: $attemptCount")
        println("SQLite V1 Bookmark Count: $bookmarkCount")

        assertEquals(3, attemptCount)
        assertEquals(2, bookmarkCount)
        assertTrue(practiceHistoryRepo.isQuestionAttempted("POL00001"))
        assertTrue(practiceHistoryRepo.isQuestionAttempted("POL00002"))
        assertTrue(practiceHistoryRepo.isQuestionAttempted("ECO00001"))
        assertTrue(bookmarkRepo.isBookmarked("POL00001"))
        assertTrue(bookmarkRepo.isBookmarked("POL00003"))

        val analysis = analysisRepo.getFullAnalysis()
        println("V1 Overall Analysis -> Total: ${analysis.overall.totalQuestions}, Attempted: ${analysis.overall.attemptedQuestions}, Correct: ${analysis.overall.correctAnswers}, Unattempted: ${analysis.overall.unattemptedQuestions}")

        assertEquals(1432, analysis.overall.totalQuestions)
        assertEquals(3, analysis.overall.attemptedQuestions)
        assertEquals(1429, analysis.overall.unattemptedQuestions)
        assertEquals(2, analysis.overall.correctAnswers)
        println("=== PHASE 1 BASELINE VERIFIED SUCCESSFULLY ===")
    }

    /**
     * Phase 2: Verify post-update state after Version 2 has been installed via adb install -r.
     */
    @Test
    fun verifyVersion2Update() = runBlocking {
        val stats = qbRepository.loadQuestionBank()
        println("=== PHASE 2: VERSION 2 IN-PLACE UPDATE VERIFICATION ===")
        println("V2 Questions loaded from APK assets: ${stats.totalQuestions}")
        println("V2 Available subjects: ${qbRepository.getAvailableSubjects()}")

        // 1. Verify new assets loaded
        assertEquals("Version 2 must contain exactly 1,438 questions", 1438, stats.totalQuestions)
        assertEquals("Version 2 must contain 8 subjects", 8, qbRepository.getAvailableSubjects().size)
        assertTrue("Version 2 must contain new subject 'Ethics'", qbRepository.getAvailableSubjects().contains("Ethics"))

        // 2. Verify newly appended question in existing subject (POL99999 in Polity)
        val polNew = qbRepository.getQuestionById("POL99999")
        assertNotNull("POL99999 must exist in V2", polNew)
        assertEquals("Polity", polNew?.subject)
        assertEquals("Judicial Reforms", polNew?.microtheme)

        // 3. Verify newly appended questions in new subject (ETH00001 - ETH00005)
        for (i in 1..5) {
            val qid = String.format("ETH%05d", i)
            val ethQ = qbRepository.getQuestionById(qid)
            assertNotNull("$qid must exist in V2", ethQ)
            assertEquals("Ethics", ethQ?.subject)
            assertEquals("Human Values", ethQ?.microtheme)
        }

        val practiceDao = database.practiceDao()
        val bookmarkDao = database.bookmarkDao()
        val practiceHistoryRepo = PracticeHistoryRepositoryImpl(practiceDao)
        val bookmarkRepo = BookmarkRepositoryImpl(bookmarkDao, qbRepository)
        val analysisRepo = AnalysisRepositoryImpl(qbRepository, practiceHistoryRepo)

        // 4. Verify old attempts, reattempts, and bookmarks survived intact
        val attemptCount = practiceHistoryRepo.getAttemptCount()
        val bookmarkCount = bookmarkRepo.getBookmarkCount()
        val attempts = practiceHistoryRepo.getAllAttempts()
        val bookmarkedIds = bookmarkRepo.getAllBookmarkedQuestionIds()

        println("SQLite V2 Attempt Count: $attemptCount")
        println("SQLite V2 Bookmark Count: $bookmarkCount")
        for (a in attempts) {
            println("Persistent Attempt: ${a.questionId} | answer=${a.selectedAnswer} | correct=${a.isCorrect} | time=${a.attemptedAt}")
        }
        for (b in bookmarkedIds) {
            println("Persistent Bookmark: $b")
        }

        assertEquals("Attempt count must remain strictly 3 (no data loss, no duplicates)", 3, attemptCount)
        assertEquals("Bookmark count must remain strictly 2 (no data loss, no duplicates)", 2, bookmarkCount)

        // POL00001 intact
        val a1 = attempts.firstOrNull { it.questionId == "POL00001" }
        assertNotNull(a1)
        assertEquals("C", a1?.selectedAnswer)
        assertEquals(true, a1?.isCorrect)

        // POL00002 intact
        val a2 = attempts.firstOrNull { it.questionId == "POL00002" }
        assertNotNull(a2)
        assertEquals("D", a2?.selectedAnswer)
        assertEquals(false, a2?.isCorrect)

        // ECO00001 reattempt intact
        val a3 = attempts.firstOrNull { it.questionId == "ECO00001" }
        assertNotNull(a3)
        assertEquals("D", a3?.selectedAnswer)
        assertEquals(true, a3?.isCorrect)

        // Bookmarks intact
        assertTrue("POL00001 must still be bookmarked", bookmarkedIds.contains("POL00001"))
        assertTrue("POL00003 must still be bookmarked", bookmarkedIds.contains("POL00003"))

        // 5. Verify new questions are initially unattempted and unbookmarked
        assertFalse("POL99999 must be unattempted", practiceHistoryRepo.isQuestionAttempted("POL99999"))
        assertFalse("POL99999 must be unbookmarked", bookmarkRepo.isBookmarked("POL99999"))
        for (i in 1..5) {
            val qid = String.format("ETH%05d", i)
            assertFalse("$qid must be unattempted", practiceHistoryRepo.isQuestionAttempted(qid))
            assertFalse("$qid must be unbookmarked", bookmarkRepo.isBookmarked(qid))
        }

        // 6. Verify newly appended questions appear in practice pool
        val ethicsQuestions = qbRepository.getQuestionsForSubject("Ethics")
        assertEquals(5, ethicsQuestions.size)
        val ethicsSession = PracticeSession(ethicsQuestions)
        assertFalse(ethicsSession.isExhausted)
        assertNotNull(ethicsSession.currentQuestion)
        assertTrue(ethicsSession.currentQuestion?.questionId in listOf("ETH00001", "ETH00002", "ETH00003", "ETH00004", "ETH00005"))

        // Verify Polity practice pool contains POL99999 and excludes attempted POL00001, POL00002
        val polityUnattempted = qbRepository.getQuestionsForSubject("Polity").filter {
            !practiceHistoryRepo.isQuestionAttempted(it.questionId)
        }
        assertTrue("Polity unattempted pool must include POL99999", polityUnattempted.any { it.questionId == "POL99999" })
        assertFalse("Polity unattempted pool must exclude POL00001", polityUnattempted.any { it.questionId == "POL00001" })
        assertFalse("Polity unattempted pool must exclude POL00002", polityUnattempted.any { it.questionId == "POL00002" })

        // 7. Verify Analysis dynamically reflects new totals
        val analysis = analysisRepo.getFullAnalysis()
        println("V2 Overall Analysis -> Total: ${analysis.overall.totalQuestions}, Attempted: ${analysis.overall.attemptedQuestions}, Correct: ${analysis.overall.correctAnswers}, Unattempted: ${analysis.overall.unattemptedQuestions}, Coverage: ${analysis.overall.coveragePercentage}%, Accuracy: ${analysis.overall.accuracyPercentage}%")

        assertEquals("Total questions must dynamically update to 1,438", 1438, analysis.overall.totalQuestions)
        assertEquals("Attempted questions must remain 3", 3, analysis.overall.attemptedQuestions)
        assertEquals("Unattempted questions must dynamically update to 1,435", 1435, analysis.overall.unattemptedQuestions)
        assertEquals("Correct answers must remain 2", 2, analysis.overall.correctAnswers)

        // Verify Ethics subject analysis
        val ethicsAnalysis = analysis.subjects.firstOrNull { it.subject == "Ethics" }
        assertNotNull("Ethics subject analysis must be present", ethicsAnalysis)
        assertEquals(5, ethicsAnalysis?.totalQuestions)
        assertEquals(0, ethicsAnalysis?.attemptedQuestions)
        assertEquals(5, ethicsAnalysis?.unattemptedQuestions)
        assertEquals(0.0, ethicsAnalysis?.coveragePercentage ?: 0.0, 0.001)

        // Verify Polity subject analysis (252 original + 1 appended = 253)
        val polityAnalysis = analysis.subjects.firstOrNull { it.subject == "Polity" }
        assertNotNull("Polity subject analysis must be present", polityAnalysis)
        assertEquals(253, polityAnalysis?.totalQuestions)
        assertEquals(2, polityAnalysis?.attemptedQuestions)
        assertEquals(251, polityAnalysis?.unattemptedQuestions)

        println("=== PHASE 2: ALL 7 UPDATE VERIFICATION POINTS CONFIRMED ON PHYSICAL DEVICE ===")
    }
}
