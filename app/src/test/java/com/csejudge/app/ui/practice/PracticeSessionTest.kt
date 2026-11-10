package com.csejudge.app.ui.practice

import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PracticeSessionTest {

    private fun createQuestion(id: String, subject: String = "Polity", microtheme: String = "Preamble"): Question {
        return Question(
            questionId = id,
            subject = subject,
            microtheme = microtheme,
            question = listOf("Statement for $id"),
            options = Options("1", "2", "3", "4"),
            correctAnswer = "A",
            solution = listOf("Solution for $id")
        )
    }

    @Test
    fun testEmptyInitialPoolResultsInExhaustion() {
        val session = PracticeSession(emptyList())
        assertNull(session.currentQuestion)
        assertTrue(session.isExhausted)
        assertEquals(0, session.stats.answeredCount)
        assertEquals(0, session.stats.skippedCount)
    }

    @Test
    fun testInitialSelectionFromPool() {
        val questions = listOf(createQuestion("Q1"), createQuestion("Q2"), createQuestion("Q3"))
        val session = PracticeSession(questions)

        assertNotNull(session.currentQuestion)
        assertFalse(session.isExhausted)
        assertTrue(session.currentQuestion?.questionId in listOf("Q1", "Q2", "Q3"))
    }

    @Test
    fun testSingleQuestionPoolHandlesSkipAndExhaustionSafely() {
        val q1 = createQuestion("Q1")
        val session = PracticeSession(listOf(q1))

        assertEquals("Q1", session.currentQuestion?.questionId)
        assertFalse(session.isExhausted)

        // Skipping the only question repeats it safely
        val afterSkip = session.skipCurrent()
        assertEquals("Q1", afterSkip?.questionId)
        assertEquals(1, session.stats.skippedCount)
        assertEquals(0, session.stats.answeredCount)
        assertFalse(session.isExhausted)

        // Answering it then moving to next exhausts the session
        session.recordAnswer(isCorrect = true)
        assertEquals(1, session.stats.answeredCount)
        assertEquals(1, session.stats.correctCount)

        val afterNext = session.moveToNext()
        assertNull(afterNext)
        assertTrue(session.isExhausted)
    }

    @Test
    fun testSkipMovesCurrentToDeferredAndDoesNotRepeatImmediatelyWhenAlternativesExist() {
        val q1 = createQuestion("Q1")
        val q2 = createQuestion("Q2")
        // Use a deterministic seed
        val random = Random(42)
        val session = PracticeSession(listOf(q1, q2), random)

        val firstQ = session.currentQuestion!!.questionId
        val secondExpected = if (firstQ == "Q1") "Q2" else "Q1"

        // Skip first question
        val nextQ = session.skipCurrent()
        assertEquals(secondExpected, nextQ?.questionId)
        assertEquals(1, session.stats.skippedCount)

        // Now activePool is empty, deferredPool has firstQ.
        // Skip second question: deferredPool now has [firstQ, secondExpected].
        // Active pool is empty, recycled! Since pool size is 2, it must avoid immediately repeating secondExpected!
        val thirdQ = session.skipCurrent()
        assertEquals("Should not repeat the just-skipped question immediately", firstQ, thirdQ?.questionId)
        assertEquals(2, session.stats.skippedCount)
    }

    @Test
    fun testActivePoolConsumedBeforeDeferredRecycled() {
        val q1 = createQuestion("Q1")
        val q2 = createQuestion("Q2")
        val q3 = createQuestion("Q3")
        val session = PracticeSession(listOf(q1, q2, q3))

        val first = session.currentQuestion!!.questionId
        // Skip first
        val second = session.skipCurrent()!!.questionId
        assertTrue(second != first)

        // Skip second
        val third = session.skipCurrent()!!.questionId
        assertTrue(third != first && third != second)

        // All 3 initial questions were presented before deferred recycled
        val seen = setOf(first, second, third)
        assertEquals(setOf("Q1", "Q2", "Q3"), seen)
    }

    @Test
    fun testAnsweredQuestionsNeverReappear() {
        val q1 = createQuestion("Q1")
        val q2 = createQuestion("Q2")
        val session = PracticeSession(listOf(q1, q2))

        val first = session.currentQuestion!!
        session.recordAnswer(isCorrect = true)

        val second = session.moveToNext()
        assertNotNull(second)
        assertTrue(second!!.questionId != first.questionId)

        // Skip the second question repeatedly to verify the first answered question never reappears
        for (i in 1..5) {
            val q = session.skipCurrent()
            assertEquals("Only the second question should cycle", second.questionId, q?.questionId)
        }

        // Answer second question and advance
        session.recordAnswer(isCorrect = false)
        val end = session.moveToNext()
        assertNull(end)
        assertTrue(session.isExhausted)
    }

    @Test
    fun testSessionStatisticsTracking() {
        val questions = (1..5).map { createQuestion("Q$it") }
        val session = PracticeSession(questions)

        // Skip 2 questions
        session.skipCurrent()
        session.skipCurrent()

        // Answer 1 correctly
        session.recordAnswer(isCorrect = true)
        session.moveToNext()

        // Answer 1 incorrectly
        session.recordAnswer(isCorrect = false)
        session.moveToNext()

        val stats = session.stats
        assertEquals(2, stats.skippedCount)
        assertEquals(2, stats.answeredCount)
        assertEquals(1, stats.correctCount)
        assertEquals(1, stats.incorrectCount)
    }
}
