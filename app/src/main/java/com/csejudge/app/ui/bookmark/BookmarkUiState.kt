package com.csejudge.app.ui.bookmark

import com.csejudge.app.data.model.Question

/**
 * Item representing a subject with active bookmarked questions.
 */
data class BookmarkSubjectItem(
    val subject: String,
    val questionCount: Int
)

/**
 * Item representing a microtheme with active bookmarked questions under a specific subject.
 */
data class BookmarkMicrothemeItem(
    val subject: String,
    val microtheme: String,
    val questionCount: Int
)

/**
 * State of a single bookmarked question, including its previous attempt history if attempted.
 */
data class BookmarkQuestionState(
    val question: Question,
    val isBookmarked: Boolean,
    val isAttempted: Boolean,
    val selectedAnswer: String? = null,
    val isCorrect: Boolean? = null,
    val attemptedAt: Long? = null
)

/**
 * Immutable UI state for the Bookmarked Questions feature.
 */
data class BookmarkUiState(
    val totalBookmarkCount: Int = 0,
    val subjects: List<BookmarkSubjectItem> = emptyList(),
    val selectedSubject: String? = null,
    val microthemes: List<BookmarkMicrothemeItem> = emptyList(),
    val selectedMicrotheme: String? = null,
    val questions: List<Question> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val currentQuestionState: BookmarkQuestionState? = null,
    val isLoading: Boolean = false,
    val isBookmarkToggling: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val totalQuestionsInCurrentMicrotheme: Int get() = questions.size
    val hasPreviousQuestion: Boolean get() = currentQuestionIndex > 0
    val hasNextQuestion: Boolean get() = currentQuestionIndex < questions.size - 1
}
