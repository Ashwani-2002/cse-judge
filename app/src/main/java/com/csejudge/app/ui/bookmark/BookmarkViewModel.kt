package com.csejudge.app.ui.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.repository.BookmarkRepository
import com.csejudge.app.data.repository.PracticeHistoryRepository
import com.csejudge.app.data.repository.QuestionBankRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel managing the Bookmarked Questions workflow:
 * - Hierarchy: Subjects -> Microthemes -> Questions
 * - Observes reactive bookmark count and bookmarked IDs
 * - Retrieves questions from QuestionBankRepository using questionId
 * - Queries attempt history for previous attempt information
 * - Re-attempt workflow: updates existing attempt records in-place via upsert
 * - Bookmark removal/addition: strictly independent from attempt persistence
 */
class BookmarkViewModel(
    private val questionBankRepository: QuestionBankRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val practiceHistoryRepository: PracticeHistoryRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookmarkUiState(isLoading = true))
    val uiState: StateFlow<BookmarkUiState> = _uiState.asStateFlow()

    init {
        observeBookmarkData()
    }

    private fun observeBookmarkData() {
        // Observe reactive total bookmark count
        viewModelScope.launch(ioDispatcher) {
            bookmarkRepository.observeBookmarkCount().collectLatest { count ->
                _uiState.update { it.copy(totalBookmarkCount = count) }
            }
        }

        // Observe reactive bookmark IDs and reconstruct hierarchy
        viewModelScope.launch(ioDispatcher) {
            bookmarkRepository.observeAllBookmarkedQuestionIds().collectLatest { bookmarkedIds ->
                try {
                    if (!questionBankRepository.isLoaded) {
                        questionBankRepository.loadQuestionBank()
                    }

                    // Retrieve valid questions by permanent questionId
                    val bookmarkedQuestions = bookmarkedIds.mapNotNull { id ->
                        questionBankRepository.getQuestionById(id)
                    }

                    // Group by subject, preserving natural appearance order in question bank
                    val subjects = bookmarkedQuestions
                        .groupBy { it.subject }
                        .map { (subject, list) ->
                            BookmarkSubjectItem(subject = subject, questionCount = list.size)
                        }

                    val currentSubject = _uiState.value.selectedSubject

                    val microthemes = if (currentSubject != null) {
                        val questionsInSubject = bookmarkedQuestions.filter { it.subject == currentSubject }
                        questionsInSubject
                            .groupBy { it.microtheme }
                            .map { (microtheme, list) ->
                                BookmarkMicrothemeItem(
                                    subject = currentSubject,
                                    microtheme = microtheme,
                                    questionCount = list.size
                                )
                            }
                    } else {
                        emptyList()
                    }

                    _uiState.update { state ->
                        val updatedQuestionState = state.currentQuestionState?.let { qState ->
                            qState.copy(isBookmarked = bookmarkedIds.contains(qState.question.questionId))
                        }
                        state.copy(
                            isLoading = false,
                            subjects = subjects,
                            microthemes = microthemes,
                            currentQuestionState = updatedQuestionState ?: state.currentQuestionState
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load bookmarked questions: ${e.message}"
                        )
                    }
                }
            }
        }
    }

    /**
     * Resolves question attempt information and bookmark state.
     */
    private suspend fun resolveQuestionState(question: Question): BookmarkQuestionState {
        val attempt = practiceHistoryRepository.getAttempt(question.questionId)
        val isBookmarked = bookmarkRepository.isBookmarked(question.questionId)
        return BookmarkQuestionState(
            question = question,
            isBookmarked = isBookmarked,
            isAttempted = attempt != null,
            selectedAnswer = attempt?.selectedAnswer,
            isCorrect = attempt?.isCorrect,
            attemptedAt = attempt?.attemptedAt
        )
    }

    /**
     * Selects a subject and populates microthemes containing bookmarked questions.
     */
    fun selectSubject(subject: String) {
        viewModelScope.launch(ioDispatcher) {
            if (!questionBankRepository.isLoaded) {
                questionBankRepository.loadQuestionBank()
            }

            val bookmarkedIds = bookmarkRepository.getAllBookmarkedQuestionIds()
            val questions = bookmarkedIds
                .mapNotNull { questionBankRepository.getQuestionById(it) }
                .filter { it.subject == subject }

            val microthemes = questions
                .groupBy { it.microtheme }
                .map { (mt, list) ->
                    BookmarkMicrothemeItem(subject = subject, microtheme = mt, questionCount = list.size)
                }

            _uiState.update {
                it.copy(
                    selectedSubject = subject,
                    selectedMicrotheme = null,
                    microthemes = microthemes,
                    questions = emptyList(),
                    currentQuestionIndex = 0,
                    currentQuestionState = null
                )
            }
        }
    }

    /**
     * Selects a microtheme under the chosen subject and loads its bookmarked questions.
     */
    fun selectMicrotheme(subject: String, microtheme: String) {
        viewModelScope.launch(ioDispatcher) {
            if (!questionBankRepository.isLoaded) {
                questionBankRepository.loadQuestionBank()
            }

            val bookmarkedIds = bookmarkRepository.getAllBookmarkedQuestionIds()
            val questions = bookmarkedIds
                .mapNotNull { questionBankRepository.getQuestionById(it) }
                .filter { it.subject == subject && it.microtheme == microtheme }

            val firstQuestionState = if (questions.isNotEmpty()) {
                resolveQuestionState(questions[0])
            } else {
                null
            }

            _uiState.update {
                it.copy(
                    selectedSubject = subject,
                    selectedMicrotheme = microtheme,
                    questions = questions,
                    currentQuestionIndex = 0,
                    currentQuestionState = firstQuestionState
                )
            }
        }
    }

    /**
     * Navigates to the next bookmarked question in the active microtheme.
     */
    fun nextQuestion() {
        val state = _uiState.value
        if (state.currentQuestionIndex < state.questions.size - 1) {
            val nextIndex = state.currentQuestionIndex + 1
            val nextQuestion = state.questions[nextIndex]
            viewModelScope.launch(ioDispatcher) {
                val qState = resolveQuestionState(nextQuestion)
                _uiState.update {
                    it.copy(
                        currentQuestionIndex = nextIndex,
                        currentQuestionState = qState
                    )
                }
            }
        }
    }

    /**
     * Navigates to the previous bookmarked question in the active microtheme.
     */
    fun previousQuestion() {
        val state = _uiState.value
        if (state.currentQuestionIndex > 0) {
            val prevIndex = state.currentQuestionIndex - 1
            val prevQuestion = state.questions[prevIndex]
            viewModelScope.launch(ioDispatcher) {
                val qState = resolveQuestionState(prevQuestion)
                _uiState.update {
                    it.copy(
                        currentQuestionIndex = prevIndex,
                        currentQuestionState = qState
                    )
                }
            }
        }
    }

    /**
     * Submits an answer (first-time attempt or re-attempt) for the currently displayed question.
     * Evaluates correctness and updates the attempt record in Room via upsertAttempt,
     * replacing previous attempt data without creating duplicates.
     */
    fun answerQuestion(answer: String) {
        val state = _uiState.value
        val qState = state.currentQuestionState ?: return
        val currentQ = qState.question
        if (state.isSubmitting) return

        val normalized = answer.trim().uppercase()
        if (normalized !in Question.VALID_OPTIONS) {
            _uiState.update { it.copy(errorMessage = "Invalid option '$answer'. Must be A, B, C, or D.") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch(ioDispatcher) {
            val isCorrect = normalized == currentQ.correctAnswer.trim().uppercase()
            val now = System.currentTimeMillis()

            val result = practiceHistoryRepository.recordOrUpdateAttempt(
                questionId = currentQ.questionId,
                selectedAnswer = normalized,
                isCorrect = isCorrect,
                attemptedAt = now
            )

            if (result.isSuccess) {
                _uiState.update { s ->
                    val current = s.currentQuestionState
                    if (current != null && current.question.questionId == currentQ.questionId) {
                        s.copy(
                            isSubmitting = false,
                            currentQuestionState = current.copy(
                                isAttempted = true,
                                selectedAnswer = normalized,
                                isCorrect = isCorrect,
                                attemptedAt = now
                            )
                        )
                    } else {
                        s.copy(isSubmitting = false)
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Failed to save attempt: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    /**
     * Toggles bookmark status for the current question.
     * Uses debounce guard (isBookmarkToggling).
     * Removing a bookmark strictly deletes only the bookmark record;
     * the attempt record is preserved in full.
     */
    fun toggleBookmarkCurrentQuestion() {
        val state = _uiState.value
        val qState = state.currentQuestionState ?: return
        val currentQ = qState.question
        if (state.isBookmarkToggling) return

        val currentlyBookmarked = qState.isBookmarked
        val questionId = currentQ.questionId

        _uiState.update { it.copy(isBookmarkToggling = true) }

        viewModelScope.launch(ioDispatcher) {
            val result = if (currentlyBookmarked) {
                bookmarkRepository.removeBookmark(questionId)
            } else {
                bookmarkRepository.addBookmark(questionId)
            }

            if (result.isSuccess) {
                _uiState.update { s ->
                    val current = s.currentQuestionState
                    if (current != null && current.question.questionId == questionId) {
                        s.copy(
                            isBookmarkToggling = false,
                            currentQuestionState = current.copy(
                                isBookmarked = !currentlyBookmarked
                            )
                        )
                    } else {
                        s.copy(isBookmarkToggling = false)
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isBookmarkToggling = false,
                        errorMessage = "Failed to update bookmark: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    /**
     * Resets microtheme selection when navigating back from questions to microthemes.
     */
    fun clearMicrotheme() {
        _uiState.update {
            it.copy(
                selectedMicrotheme = null,
                questions = emptyList(),
                currentQuestionIndex = 0,
                currentQuestionState = null
            )
        }
    }

    /**
     * Resets subject selection when navigating back from microthemes to subjects.
     */
    fun clearSubject() {
        _uiState.update {
            it.copy(
                selectedSubject = null,
                selectedMicrotheme = null,
                microthemes = emptyList(),
                questions = emptyList(),
                currentQuestionIndex = 0,
                currentQuestionState = null
            )
        }
    }

    /**
     * Clears error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
