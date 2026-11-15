package com.csejudge.app.data.repository

import com.csejudge.app.data.model.Question
import com.csejudge.app.data.model.TestType
import java.util.Random

class InsufficientQuestionsException(
    val required: Int,
    val available: Int,
    message: String = "Insufficient unattempted questions available: $available found, but $required required to start ${if (required == 100) "Full Length Test" else "Half Length Test"}."
) : IllegalStateException(message)

data class SubjectAllocationRange(
    val canonicalName: String,
    val min: Int,
    val max: Int,
    val aliases: List<String>
) {
    fun toHalfLength(): SubjectAllocationRange {
        val hltMin = maxOf(1, Math.round(min / 2.0).toInt())
        val hltMax = maxOf(1, Math.round(max / 2.0).toInt())
        return SubjectAllocationRange(canonicalName, hltMin, hltMax, aliases)
    }

    fun matches(subject: String): Boolean {
        val normTarget = normalize(subject)
        return aliases.any { normalize(it) == normTarget }
    }

    companion object {
        fun normalize(name: String): String =
            name.lowercase()
                .replace("&", "and")
                .replace("[^a-z0-9]".toRegex(), "")
    }
}

object TestQuestionSelector {

    val FLT_SUBJECT_RANGES = listOf(
        SubjectAllocationRange("Polity", 12, 19, listOf("Polity", "Indian Polity")),
        SubjectAllocationRange("Economy", 14, 22, listOf("Economy", "Indian Economy")),
        SubjectAllocationRange("Geography", 9, 18, listOf("Geography")),
        SubjectAllocationRange("Modern History", 2, 10, listOf("Modern History", "Modern Indian History")),
        SubjectAllocationRange("AMAC", 8, 14, listOf("AMAC", "Ancient, Medieval & Art and Culture", "Art and Culture")),
        SubjectAllocationRange("Science and Technology", 10, 14, listOf("Science and Technology", "Sci and Tech", "Science & Technology", "S&T")),
        SubjectAllocationRange("Environment", 15, 21, listOf("Environment", "Environment and Ecology"))
    )

    val HLT_SUBJECT_RANGES = FLT_SUBJECT_RANGES.map { it.toHalfLength() }

    /**
     * Selects questions for a test following the strict Stage 13 distribution and eligibility rules.
     *
     * @param allQuestions Complete master question bank.
     * @param attemptedQuestionIds Set of question IDs already attempted in Practice or previous completed tests.
     * @param testType Full Length or Half Length test.
     * @param random Optional pseudo-random generator (allows reproducible testing).
     * @return Ordered list of exactly [testType.totalQuestions] unique questions shuffled across subjects.
     * @throws InsufficientQuestionsException if eligible unattempted questions are insufficient.
     */
    fun selectQuestions(
        allQuestions: List<Question>,
        attemptedQuestionIds: Set<String>,
        testType: TestType,
        random: Random = Random()
    ): List<Question> {
        val targetCount = testType.totalQuestions
        val ranges = if (testType == TestType.FULL_LENGTH) FLT_SUBJECT_RANGES else HLT_SUBJECT_RANGES

        // 1. Filter out all attempted questions under unified history rules
        val eligibleQuestions = allQuestions.filter { it.questionId !in attemptedQuestionIds }

        if (eligibleQuestions.size < targetCount) {
            throw InsufficientQuestionsException(
                required = targetCount,
                available = eligibleQuestions.size
            )
        }

        val selectedQuestions = mutableListOf<Question>()
        val selectedIds = mutableSetOf<String>()

        // 2. Select target allocation for each configured subject
        for (range in ranges) {
            val matchingSubjectQuestions = eligibleQuestions
                .filter { range.matches(it.subject) && it.questionId !in selectedIds }
                .shuffled(random)

            val allocation = if (range.max >= range.min) {
                random.nextInt(range.max - range.min + 1) + range.min
            } else {
                range.min
            }

            val countToTake = minOf(allocation, matchingSubjectQuestions.size)
            val taken = matchingSubjectQuestions.take(countToTake)
            selectedQuestions.addAll(taken)
            taken.forEach { selectedIds.add(it.questionId) }
        }

        // 3. Over-allocation correction: If selection exceeds target, randomly trim
        if (selectedQuestions.size > targetCount) {
            selectedQuestions.shuffle(random)
            val excess = selectedQuestions.size - targetCount
            repeat(excess) {
                val removed = selectedQuestions.removeAt(selectedQuestions.lastIndex)
                selectedIds.remove(removed.questionId)
            }
        }

        // 4. Under-allocation filling: If below target, fill from remaining eligible questions
        if (selectedQuestions.size < targetCount) {
            val needed = targetCount - selectedQuestions.size
            val remainingPool = eligibleQuestions
                .filter { it.questionId !in selectedIds }
                .shuffled(random)

            if (remainingPool.size < needed) {
                throw InsufficientQuestionsException(
                    required = targetCount,
                    available = selectedQuestions.size + remainingPool.size
                )
            }

            val fillQuestions = remainingPool.take(needed)
            selectedQuestions.addAll(fillQuestions)
            fillQuestions.forEach { selectedIds.add(it.questionId) }
        }

        // 5. Invariant check: exactly targetCount unique questions
        check(selectedQuestions.size == targetCount) {
            "Expected exactly $targetCount questions, but got ${selectedQuestions.size}"
        }
        check(selectedQuestions.distinctBy { it.questionId }.size == targetCount) {
            "Selected questions contained duplicates"
        }

        // 6. Shuffle final selection so questions are not grouped by subject
        return selectedQuestions.shuffled(random)
    }
}
