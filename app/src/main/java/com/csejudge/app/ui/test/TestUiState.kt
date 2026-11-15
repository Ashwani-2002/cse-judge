package com.csejudge.app.ui.test

import com.csejudge.app.data.model.Question
import com.csejudge.app.data.model.TestReport
import com.csejudge.app.data.model.TestType
import java.util.Locale

/**
 * Palette status for circular question indicator.
 */
enum class PaletteQuestionState {
    UNANSWERED_RED,      // Unanswered and not marked for review
    MARKED_FOR_REVIEW_PURPLE, // Marked for review (regardless of answered status)
    ANSWERED_GREEN       // Answered and not marked for review
}

/**
 * UI State for the Tests Module.
 */
data class TestUiState(
    val isLoading: Boolean = false,
    val testType: TestType? = null,
    val questions: List<Question> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<Int, String> = emptyMap(), // questionIndex -> "A"|"B"|"C"|"D"
    val markedForReviewIndices: Set<Int> = emptySet(),   // questionIndex
    val remainingSeconds: Int = 0,
    val isTimerActive: Boolean = false,
    val isPaletteOpen: Boolean = false,
    val showSubmitConfirmation: Boolean = false,
    val showAbortConfirmation: Boolean = false,
    val isSubmitting: Boolean = false,
    val isCompleted: Boolean = false,
    val isAborted: Boolean = false,
    val abortReason: String? = null,
    val completedTestId: String? = null,
    val currentReport: TestReport? = null,
    val errorMessage: String? = null,
    val availableUnattemptedQuestions: Int = 0
) {
    val totalQuestions: Int get() = questions.size

    val currentQuestion: Question?
        get() = questions.getOrNull(currentQuestionIndex)

    val currentSelectedAnswer: String?
        get() = selectedAnswers[currentQuestionIndex]

    val isCurrentMarkedForReview: Boolean
        get() = currentQuestionIndex in markedForReviewIndices

    val formattedTimer: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }

    fun getPaletteState(index: Int): PaletteQuestionState {
        return when {
            index in markedForReviewIndices -> PaletteQuestionState.MARKED_FOR_REVIEW_PURPLE
            !selectedAnswers[index].isNullOrBlank() -> PaletteQuestionState.ANSWERED_GREEN
            else -> PaletteQuestionState.UNANSWERED_RED
        }
    }
}
