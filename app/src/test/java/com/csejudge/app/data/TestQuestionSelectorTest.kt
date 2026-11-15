package com.csejudge.app.data

import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.model.TestType
import com.csejudge.app.data.repository.InsufficientQuestionsException
import com.csejudge.app.data.repository.TestQuestionSelector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class TestQuestionSelectorTest {

    private fun createQuestion(
        id: String,
        subject: String,
        microtheme: String = "Microtheme 1",
        correctAnswer: String = "A"
    ): Question {
        return Question(
            subject = subject,
            microtheme = microtheme,
            question = listOf("Statement 1 for $id", "Statement 2 for $id"),
            options = Options("Option A", "Option B", "Option C", "Option D"),
            correctAnswer = correctAnswer,
            solution = listOf("Solution explanation for $id"),
            questionId = id
        )
    }

    private fun generateMockQuestionBank(perSubjectCount: Int = 30): List<Question> {
        val subjects = listOf(
            "Polity",
            "Economy",
            "Geography",
            "Modern History",
            "AMAC",
            "Sci and Tech", // JSON alias for Science and Technology
            "Environment"
        )
        val questions = mutableListOf<Question>()
        var counter = 1
        for (subj in subjects) {
            for (i in 1..perSubjectCount) {
                questions.add(createQuestion("Q_${subj}_$i", subj))
                counter++
            }
        }
        return questions
    }

    @Test
    fun testFltQuestionCountExactly100() {
        val bank = generateMockQuestionBank(perSubjectCount = 30) // 210 questions
        val selected = TestQuestionSelector.selectQuestions(
            allQuestions = bank,
            attemptedQuestionIds = emptySet(),
            testType = TestType.FULL_LENGTH,
            random = Random(42)
        )

        assertEquals(100, selected.size)
        assertEquals(100, selected.distinctBy { it.questionId }.size)
    }

    @Test
    fun testHltQuestionCountExactly50() {
        val bank = generateMockQuestionBank(perSubjectCount = 20) // 140 questions
        val selected = TestQuestionSelector.selectQuestions(
            allQuestions = bank,
            attemptedQuestionIds = emptySet(),
            testType = TestType.HALF_LENGTH,
            random = Random(42)
        )

        assertEquals(50, selected.size)
        assertEquals(50, selected.distinctBy { it.questionId }.size)
    }

    @Test
    fun testHltSubjectRangesComputedCorrectly() {
        // FLT Ranges:
        // Polity: 12-19 -> HLT: 6-10
        // Economy: 14-22 -> HLT: 7-11
        // Geography: 9-18 -> HLT: 5-9
        // Modern History: 2-10 -> HLT: 1-5
        // AMAC: 8-14 -> HLT: 4-7
        // Science and Technology: 10-14 -> HLT: 5-7
        // Environment: 15-21 -> HLT: 8-11
        val hltRanges = TestQuestionSelector.HLT_SUBJECT_RANGES

        val polity = hltRanges.first { it.canonicalName == "Polity" }
        assertEquals(6, polity.min)
        assertEquals(10, polity.max)

        val economy = hltRanges.first { it.canonicalName == "Economy" }
        assertEquals(7, economy.min)
        assertEquals(11, economy.max)

        val geography = hltRanges.first { it.canonicalName == "Geography" }
        assertEquals(5, geography.min)
        assertEquals(9, geography.max)

        val modernHistory = hltRanges.first { it.canonicalName == "Modern History" }
        assertEquals(1, modernHistory.min)
        assertEquals(5, modernHistory.max)

        val amac = hltRanges.first { it.canonicalName == "AMAC" }
        assertEquals(4, amac.min)
        assertEquals(7, amac.max)

        val snt = hltRanges.first { it.canonicalName == "Science and Technology" }
        assertEquals(5, snt.min)
        assertEquals(7, snt.max)

        val environment = hltRanges.first { it.canonicalName == "Environment" }
        assertEquals(8, environment.min)
        assertEquals(11, environment.max)
    }

    @Test
    fun testExclusionOfPracticeAttemptedQuestions() {
        val bank = generateMockQuestionBank(perSubjectCount = 30)
        // Mark 50 questions as attempted
        val attemptedIds = bank.take(50).map { it.questionId }.toSet()

        val selected = TestQuestionSelector.selectQuestions(
            allQuestions = bank,
            attemptedQuestionIds = attemptedIds,
            testType = TestType.FULL_LENGTH,
            random = Random(123)
        )

        assertEquals(100, selected.size)
        // Verify none of the attempted questions are present
        for (q in selected) {
            assertFalse("Attempted question ${q.questionId} was selected", q.questionId in attemptedIds)
        }
    }

    @Test(expected = InsufficientQuestionsException::class)
    fun testThrowsWhenInsufficientEligibleQuestionsForFlt() {
        // Only 80 questions in bank, but 100 required
        val smallBank = (1..80).map { createQuestion("ID_$it", "Polity") }
        TestQuestionSelector.selectQuestions(
            allQuestions = smallBank,
            attemptedQuestionIds = emptySet(),
            testType = TestType.FULL_LENGTH
        )
    }

    @Test(expected = InsufficientQuestionsException::class)
    fun testThrowsWhenAttemptedQuestionsCauseShortage() {
        // 120 questions, but 30 already attempted -> only 90 eligible (100 required)
        val bank = (1..120).map { createQuestion("ID_$it", "Economy") }
        val attempted = (1..30).map { "ID_$it" }.toSet()

        TestQuestionSelector.selectQuestions(
            allQuestions = bank,
            attemptedQuestionIds = attempted,
            testType = TestType.FULL_LENGTH
        )
    }

    @Test
    fun testUnderAllocationSubjectBackfilling() {
        // Suppose "Modern History" only has 1 question in bank, but range target requires more
        // Other subjects have plenty of questions
        val questions = mutableListOf<Question>()
        questions.add(createQuestion("MH_1", "Modern History"))
        for (i in 1..40) {
            questions.add(createQuestion("POL_$i", "Polity"))
            questions.add(createQuestion("ECO_$i", "Economy"))
            questions.add(createQuestion("GEO_$i", "Geography"))
            questions.add(createQuestion("ENV_$i", "Environment"))
        }

        // Even with Modern History having only 1 question, FLT backfills up to exactly 100
        val selected = TestQuestionSelector.selectQuestions(
            allQuestions = questions,
            attemptedQuestionIds = emptySet(),
            testType = TestType.FULL_LENGTH,
            random = Random(99)
        )

        assertEquals(100, selected.size)
        assertEquals(100, selected.distinctBy { it.questionId }.size)
    }

    @Test
    fun testSubjectAliasMatchingForSciAndTech() {
        // Verify that "Sci and Tech" matches "Science and Technology"
        val range = TestQuestionSelector.FLT_SUBJECT_RANGES.first { it.canonicalName == "Science and Technology" }
        assertTrue(range.matches("Sci and Tech"))
        assertTrue(range.matches("Science and Technology"))
        assertTrue(range.matches("science & technology"))
        assertTrue(range.matches("S&T"))
    }

    @Test
    fun testFinalSelectionIsShuffled() {
        val bank = generateMockQuestionBank(perSubjectCount = 30)
        val selected = TestQuestionSelector.selectQuestions(
            allQuestions = bank,
            attemptedQuestionIds = emptySet(),
            testType = TestType.FULL_LENGTH,
            random = Random(42)
        )

        // If not shuffled, subjects would appear consecutively in blocks of 10-20.
        // Check that consecutive subjects vary frequently across the selection.
        var subjectChanges = 0
        for (i in 0 until selected.size - 1) {
            if (selected[i].subject != selected[i + 1].subject) {
                subjectChanges++
            }
        }

        // In a properly shuffled list of 100 questions from 7 subjects, subject changes should exceed 50
        assertTrue("Expected frequent subject transitions in shuffled list, got $subjectChanges", subjectChanges > 50)
    }
}
