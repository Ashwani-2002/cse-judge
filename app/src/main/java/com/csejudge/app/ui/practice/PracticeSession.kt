package com.csejudge.app.ui.practice

import com.csejudge.app.data.model.Question
import kotlin.random.Random

/**
 * Encapsulates the in-memory pool management, random selection, and skip-cycling
 * logic for an individual practice session.
 */
class PracticeSession(
    initialEligibleQuestions: List<Question>,
    private val random: Random = Random.Default
) {
    private val activePool: MutableList<Question> = initialEligibleQuestions.toMutableList()
    private val deferredPool: MutableList<Question> = mutableListOf()

    var currentQuestion: Question? = null
        private set

    var isExhausted: Boolean = false
        private set

    var answeredCount: Int = 0
        private set
    var correctCount: Int = 0
        private set
    var incorrectCount: Int = 0
        private set
    var skippedCount: Int = 0
        private set

    val stats: PracticeSessionStats
        get() = PracticeSessionStats(
            answeredCount = answeredCount,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            skippedCount = skippedCount
        )

    init {
        selectInitialQuestion()
    }

    private fun selectInitialQuestion() {
        if (activePool.isEmpty()) {
            currentQuestion = null
            isExhausted = true
        } else {
            val index = random.nextInt(activePool.size)
            currentQuestion = activePool.removeAt(index)
            isExhausted = false
        }
    }

    /**
     * Skips the current question:
     * 1. Preserves the question as unattempted.
     * 2. Moves it to the session deferred pool.
     * 3. Selects another random question from the active pool.
     * 4. If the active pool is exhausted, recycles deferred questions while
     *    avoiding immediately repeating the just-skipped question if other questions exist.
     */
    fun skipCurrent(): Question? {
        val skipped = currentQuestion ?: return null
        skippedCount++

        deferredPool.add(skipped)
        currentQuestion = null

        if (activePool.isEmpty() && deferredPool.isNotEmpty()) {
            activePool.addAll(deferredPool)
            deferredPool.clear()

            if (activePool.size > 1) {
                val candidateIndices = activePool.indices.filter { activePool[it].questionId != skipped.questionId }
                val chosenIndex = candidateIndices.random(random)
                currentQuestion = activePool.removeAt(chosenIndex)
            } else {
                currentQuestion = activePool.removeAt(0)
            }
        } else if (activePool.isNotEmpty()) {
            val index = random.nextInt(activePool.size)
            currentQuestion = activePool.removeAt(index)
        } else {
            isExhausted = true
        }

        return currentQuestion
    }

    /**
     * Marks the current question as answered in the session.
     * Answered questions are never placed into the deferred pool and never reappear.
     */
    fun recordAnswer(isCorrect: Boolean) {
        if (currentQuestion == null) return
        answeredCount++
        if (isCorrect) correctCount++ else incorrectCount++
    }

    /**
     * Advances to the next question after an answer has been recorded.
     * If the active pool is empty, recycles deferred questions.
     * Marks session as exhausted if no active or deferred questions remain.
     */
    fun moveToNext(): Question? {
        currentQuestion = null

        if (activePool.isEmpty() && deferredPool.isNotEmpty()) {
            activePool.addAll(deferredPool)
            deferredPool.clear()
        }

        if (activePool.isNotEmpty()) {
            val index = random.nextInt(activePool.size)
            currentQuestion = activePool.removeAt(index)
        } else {
            isExhausted = true
        }

        return currentQuestion
    }
}
