package com.csejudge.app.ui.practice

/**
 * Lightweight in-memory statistics for the current practice session.
 * Skipped questions are tracked separately from attempted/answered questions.
 */
data class PracticeSessionStats(
    val answeredCount: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val skippedCount: Int = 0
)
