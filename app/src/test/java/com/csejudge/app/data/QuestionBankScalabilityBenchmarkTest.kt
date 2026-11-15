package com.csejudge.app.data

import com.csejudge.app.data.local.AttemptedQuestionEntity
import com.csejudge.app.data.local.BookmarkDao
import com.csejudge.app.data.local.BookmarkedQuestionEntity
import com.csejudge.app.data.local.PracticeDao
import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.model.QuestionBankFile
import com.csejudge.app.data.repository.AnalysisRepositoryImpl
import com.csejudge.app.data.repository.BookmarkRepositoryImpl
import com.csejudge.app.data.repository.PracticeHistoryRepositoryImpl
import com.csejudge.app.data.repository.QuestionBankRepositoryImpl
import com.csejudge.app.ui.practice.PracticeSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Performance and Scalability Benchmark Test.
 *
 * Verifies that the question bank architecture scales smoothly to 10,000 questions
 * (approx 7x current size) across 15 subjects and 150 microthemes, without introducing
 * latency spikes, memory issues, or performance regressions.
 */
class QuestionBankScalabilityBenchmarkTest {

    private val json = Json { ignoreUnknownKeys = false }

    /**
     * Synthetically generates [count] realistic UPSC questions across [subjectCount] subjects
     * and [microthemeCount] microthemes.
     */
    private fun generateSyntheticQuestionBank(
        count: Int = 10_000,
        subjectCount: Int = 15,
        microthemesPerSubject: Int = 10
    ): List<Question> {
        val subjects = (1..subjectCount).map { "Subject_${it.toString().padStart(2, '0')}" }
        val optionsList = listOf("A", "B", "C", "D")

        return (1..count).map { idx ->
            val subjectIdx = (idx - 1) % subjectCount
            val subject = subjects[subjectIdx]
            val microthemeIdx = ((idx - 1) / subjectCount) % microthemesPerSubject
            val microtheme = "${subject}_Microtheme_${microthemeIdx.toString().padStart(2, '0')}"
            val questionId = "QID_${idx.toString().padStart(6, '0')}"
            val correctAnswer = optionsList[(idx - 1) % 4]

            Question(
                questionId = questionId,
                subject = subject,
                microtheme = microtheme,
                question = listOf(
                    "Consider the following statements regarding synthetic question $idx for UPSC Prelims:",
                    "1. Statement number one discusses conceptual aspects of $microtheme.",
                    "2. Statement number two analyzes practical implications under $subject.",
                    "Which of the statements given above is/are correct?"
                ),
                options = Options(
                    a = "1 only",
                    b = "2 only",
                    c = "Both 1 and 2",
                    d = "Neither 1 nor 2"
                ),
                correctAnswer = correctAnswer,
                solution = listOf(
                    "Detailed solution explanation for $questionId:",
                    "The correct answer is option ($correctAnswer). Analysis confirms statement validity based on official standard reference material."
                )
            )
        }
    }

    @Test
    fun benchmark10000QuestionBankScalability() = runBlocking {
        println("=== Scalability Benchmark: 10,000 Questions ===")

        // 1. Generate 10,000 synthetic questions
        val genTime = measureTimeMillis {
            val questions = generateSyntheticQuestionBank(10_000)
            assertEquals(10_000, questions.size)
        }
        println("Synthetic question bank generation (10k items): ${genTime} ms")

        val syntheticQuestions = generateSyntheticQuestionBank(10_000)

        // 2. Benchmark JSON Serialization & Deserialization
        val jsonBytes: ByteArray
        val serializationTime = measureTimeMillis {
            jsonBytes = json.encodeToString(
                QuestionBankFile.serializer(),
                QuestionBankFile(syntheticQuestions)
            ).toByteArray(Charsets.UTF_8)
        }
        println("JSON serialization size: ${(jsonBytes.size / (1024 * 1024.0)).format(2)} MB in ${serializationTime} ms")

        // 3. Benchmark Initial Question Bank Loading & Deserialization & Index Generation
        val repository = QuestionBankRepositoryImpl(
            openAssetStream = { ByteArrayInputStream(jsonBytes) },
            fileNames = listOf("synthetic_10k.json")
        )

        assertFalse(repository.isLoaded)
        val loadTimeMs = measureTimeMillis {
            val stats = repository.loadQuestionBank()
            assertEquals(10_000, stats.totalQuestions)
            assertEquals(15, stats.subjectCounts.size)
        }
        assertTrue(repository.isLoaded)
        println("Repository load + full validation + index generation: ${loadTimeMs} ms")
        assertTrue("10k question loading should be under 2000 ms", loadTimeMs < 2000)

        // 4. Benchmark In-Memory Question Lookup by ID (Indexed map)
        val randomIds = (1..1000).map { "QID_${Random.nextInt(1, 10_001).toString().padStart(6, '0')}" }
        val lookupTimeMs = measureTimeMillis {
            for (id in randomIds) {
                val q = repository.getQuestionById(id)
                assertNotNull("Lookup failed for $id", q)
                assertEquals(id, q?.questionId)
            }
        }
        val perLookupMicros = (lookupTimeMs * 1000.0) / randomIds.size
        println("1,000 Random ID Lookups: total ${lookupTimeMs} ms (~${perLookupMicros.format(2)} µs per lookup)")
        assertTrue("Map lookup must be sub-millisecond", perLookupMicros < 1000)

        // 5. Benchmark Subject & Microtheme Filtering Latency
        val subjects = repository.getAvailableSubjects()
        assertEquals(15, subjects.size)
        val filterSubjectTime = measureTimeMillis {
            for (subject in subjects) {
                val subjectQuestions = repository.getQuestionsForSubject(subject)
                assertTrue(subjectQuestions.isNotEmpty())
            }
        }
        println("Filtering all 15 subjects: ${filterSubjectTime} ms (< ${(filterSubjectTime / 15.0).format(2)} ms/subject)")

        val filterMicrothemeTime = measureTimeMillis {
            for (subject in subjects) {
                val microthemes = repository.getAvailableMicrothemes(subject)
                assertEquals(10, microthemes.size)
                for (mt in microthemes) {
                    val mtQuestions = repository.getQuestionsForSubjectAndMicrotheme(subject, mt)
                    assertTrue(mtQuestions.isNotEmpty())
                }
            }
        }
        println("Filtering all 150 microthemes: ${filterMicrothemeTime} ms (< ${(filterMicrothemeTime / 150.0).format(2)} ms/microtheme)")

        // 6. Benchmark Practice Session with 10,000 Questions
        // Simulate 4,000 already attempted questions
        val attemptedIds = (1..4000).map { "QID_${it.toString().padStart(6, '0')}" }.toSet()
        val unattemptedQuestions = repository.getAllQuestions().filter { it.questionId !in attemptedIds }
        assertEquals(6000, unattemptedQuestions.size)

        val sessionInitTime = measureTimeMillis {
            val session = PracticeSession(unattemptedQuestions)
            assertFalse(session.isExhausted)
            assertNotNull(session.currentQuestion)
        }
        println("PracticeSession initialization with 6,000 unattempted pool: ${sessionInitTime} ms")
        assertTrue("Session init must be instantaneous", sessionInitTime < 50)

        // Simulate advancing/skipping 100 questions in practice session
        val session = PracticeSession(unattemptedQuestions)
        val advanceTime = measureTimeMillis {
            repeat(100) { idx ->
                if (idx % 2 == 0) {
                    session.recordAnswer(isCorrect = idx % 3 == 0)
                    session.moveToNext()
                } else {
                    session.skipCurrent()
                }
            }
        }
        println("Advancing & skipping 100 questions in 6k pool: ${advanceTime} ms (~${(advanceTime / 100.0).format(3)} ms/action)")
        assertTrue("Moving between questions must be immediate (sub-millisecond)", (advanceTime / 100.0) < 1.0)

        // 7. Benchmark Bookmark Hierarchy Reconstruction with 500 Bookmarks
        val bookmarkedIds = (1..500).map { "QID_${(it * 19).coerceAtMost(10_000).toString().padStart(6, '0')}" }
        val bookmarkHierarchyTime = measureTimeMillis {
            val bookmarkedQuestions = bookmarkedIds.mapNotNull { repository.getQuestionById(it) }
            val bySubject = bookmarkedQuestions.groupBy { it.subject }
            val byMicrotheme = bySubject.mapValues { (_, qs) -> qs.groupBy { it.microtheme } }
            assertTrue(bySubject.isNotEmpty())
            assertTrue(byMicrotheme.isNotEmpty())
        }
        println("Bookmark hierarchy reconstruction (500 bookmarks): ${bookmarkHierarchyTime} ms")
        assertTrue("Bookmark hierarchy grouping must be fast", bookmarkHierarchyTime < 50)

        // 8. Benchmark Analysis Calculation with 3,000 Attempts
        val fakeAttempts = (1..3000).map { idx ->
            val qid = "QID_${idx.toString().padStart(6, '0')}"
            AttemptedQuestionEntity(
                questionId = qid,
                selectedAnswer = if (idx % 2 == 0) "A" else "B",
                isCorrect = idx % 3 == 0,
                attemptedAt = System.currentTimeMillis() - idx * 1000L
            )
        }

        val fakePracticeDao = BenchmarkPracticeDao(fakeAttempts)
        val practiceHistoryRepo = PracticeHistoryRepositoryImpl(fakePracticeDao)
        val analysisRepo = AnalysisRepositoryImpl(repository, practiceHistoryRepo)

        val analysisTime = measureTimeMillis {
            val analysis = analysisRepo.getFullAnalysis()
            assertEquals(10_000, analysis.overall.totalQuestions)
            assertEquals(3000, analysis.overall.attemptedQuestions)
            assertEquals(7000, analysis.overall.unattemptedQuestions)
            assertEquals(15, analysis.subjects.size)
            assertEquals(1000, analysis.overall.correctAnswers)
            assertEquals(2000, analysis.overall.incorrectAnswers)
        }
        println("Full analysis calculation (10k questions, 3k attempts, 150 microthemes): ${analysisTime} ms")
        assertTrue("Analysis calculation must be fast", analysisTime < 200)

        println("=== Scalability Benchmark Completed Successfully ===")
    }

    private fun Double.format(digits: Int) = String.format("%.${digits}f", this)

    private class BenchmarkPracticeDao(
        private val attempts: List<AttemptedQuestionEntity>
    ) : PracticeDao {
        override suspend fun insertAttempt(attempt: AttemptedQuestionEntity) {}
        override suspend fun upsertAttempt(attempt: AttemptedQuestionEntity) {}
        override suspend fun isQuestionAttempted(questionId: String): Boolean = attempts.any { it.questionId == questionId }
        override suspend fun getAllAttemptedQuestionIds(): List<String> = attempts.map { it.questionId }
        override fun observeAllAttemptedQuestionIds(): Flow<List<String>> = flowOf(attempts.map { it.questionId })
        override suspend fun getAttempt(questionId: String): AttemptedQuestionEntity? = attempts.find { it.questionId == questionId }
        override suspend fun getAttemptCount(): Int = attempts.size
        override fun observeAttemptCount(): Flow<Int> = flowOf(attempts.size)
        override suspend fun getAllAttempts(): List<AttemptedQuestionEntity> = attempts
        override fun observeAllAttempts(): Flow<List<AttemptedQuestionEntity>> = flowOf(attempts)
    }
}
