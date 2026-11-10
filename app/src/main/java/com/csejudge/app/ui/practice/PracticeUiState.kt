package com.csejudge.app.ui.practice

import com.csejudge.app.data.model.Question

/**
 * Immutable UI state for the Practice screen.
 */
data class PracticeUiState(
    val mode: PracticeMode = PracticeMode.RANDOM,
    val isRandomMode: Boolean = true,
    val availableSubjects: List<String> = emptyList(),
    val selectedSubject: String? = null,
    val availableMicrothemes: List<String> = emptyList(),
    val selectedMicrotheme: String? = null,
    val currentQuestion: Question? = null,
    val isAnswered: Boolean = false,
    val selectedAnswer: String? = null,
    val isAnswerCorrect: Boolean? = null,
    val correctAnswer: String? = null,
    val isSkipEnabled: Boolean = false,
    val isNextEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isExhausted: Boolean = false,
    val sessionStats: PracticeSessionStats = PracticeSessionStats()
) {
    val sessionAnsweredCount: Int get() = sessionStats.answeredCount
    val sessionCorrectCount: Int get() = sessionStats.correctCount
    val sessionIncorrectCount: Int get() = sessionStats.incorrectCount
    val sessionSkippedCount: Int get() = sessionStats.skippedCount
}
