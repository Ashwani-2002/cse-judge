package com.csejudge.app.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.repository.PracticeHistoryRepository
import com.csejudge.app.data.repository.QuestionBankRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Filter configuration for a practice session.
 */
data class PracticeFilter(
    val mode: PracticeMode = PracticeMode.RANDOM,
    val selectedSubject: String? = null,
    val selectedMicrotheme: String? = null
) {
    val isRandomMode: Boolean get() = mode == PracticeMode.RANDOM
}

/**
 * ViewModel managing practice session state, mode/subject/microtheme filtering,
 * random unattempted question selection, deferred skip-cycling, answer evaluation,
 * and asynchronous attempt persistence.
 */
class PracticeViewModel(
    private val questionBankRepository: QuestionBankRepository,
    private val practiceHistoryRepository: PracticeHistoryRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val random: Random = Random.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    private var currentFilter = PracticeFilter(mode = PracticeMode.RANDOM)
    private var currentSession: PracticeSession? = null

    private var sessionJob: Job? = null
    private val sessionGeneration = AtomicLong(0)

    init {
        startSession()
    }

    /**
     * Toggles between Random and Subject modes.
     * When random is enabled, subject and microtheme filters are cleared.
     * When random is disabled, subject selection is required before starting practice.
     */
    fun setRandomMode(enabled: Boolean) {
        if (enabled) {
            currentFilter = PracticeFilter(mode = PracticeMode.RANDOM)
            _uiState.update {
                it.copy(
                    mode = PracticeMode.RANDOM,
                    isRandomMode = true,
                    selectedSubject = null,
                    selectedMicrotheme = null,
                    availableMicrothemes = emptyList()
                )
            }
            startSession()
        } else {
            currentFilter = PracticeFilter(
                mode = PracticeMode.SUBJECT,
                selectedSubject = _uiState.value.selectedSubject,
                selectedMicrotheme = null
            )
            _uiState.update {
                it.copy(
                    mode = PracticeMode.SUBJECT,
                    isRandomMode = false,
                    selectedMicrotheme = null
                )
            }
            if (currentFilter.selectedSubject != null) {
                startSession()
            } else {
                currentSession = null
                _uiState.update {
                    it.copy(
                        currentQuestion = null,
                        isExhausted = false,
                        isSkipEnabled = false,
                        isNextEnabled = false,
                        isAnswered = false,
                        selectedAnswer = null,
                        isAnswerCorrect = null,
                        correctAnswer = null
                    )
                }
            }
        }
    }

    /**
     * Selects a subject and switches to Subject mode.
     * Resets microtheme and initiates a new practice session for this subject.
     */
    fun selectSubject(subject: String) {
        val availableSubjects = _uiState.value.availableSubjects
        if (availableSubjects.isNotEmpty() && subject !in availableSubjects) {
            _uiState.update { it.copy(errorMessage = "Invalid subject: '$subject'") }
            return
        }

        val availableMicrothemes = if (questionBankRepository.isLoaded) {
            questionBankRepository.getAvailableMicrothemes(subject)
        } else {
            emptyList()
        }

        currentFilter = PracticeFilter(
            mode = PracticeMode.SUBJECT,
            selectedSubject = subject,
            selectedMicrotheme = null
        )

        _uiState.update {
            it.copy(
                mode = PracticeMode.SUBJECT,
                isRandomMode = false,
                selectedSubject = subject,
                selectedMicrotheme = null,
                availableMicrothemes = availableMicrothemes
            )
        }

        startSession()
    }

    /**
     * Selects or clears the microtheme filter under the currently selected subject.
     * Passing null removes microtheme restriction, allowing questions from all microthemes
     * of the selected subject.
     */
    fun selectMicrotheme(microtheme: String?) {
        val subject = currentFilter.selectedSubject
        if (subject == null) {
            _uiState.update { it.copy(errorMessage = "A subject must be selected before choosing a microtheme") }
            return
        }

        val available = _uiState.value.availableMicrothemes
        if (microtheme != null && available.isNotEmpty() && microtheme !in available) {
            _uiState.update { it.copy(errorMessage = "Invalid microtheme: '$microtheme'") }
            return
        }

        currentFilter = currentFilter.copy(selectedMicrotheme = microtheme)
        _uiState.update {
            it.copy(selectedMicrotheme = microtheme)
        }

        startSession()
    }

    /**
     * Initialises a new practice session matching current filter settings.
     * Cancels any in-flight session preparation and protects against stale results.
     */
    fun startSession() {
        val gen = sessionGeneration.incrementAndGet()
        sessionJob?.cancel()

        val filter = currentFilter
        if (filter.mode == PracticeMode.SUBJECT && filter.selectedSubject.isNullOrBlank()) {
            currentSession = null
            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentQuestion = null,
                    isSkipEnabled = false,
                    isNextEnabled = false,
                    isExhausted = false,
                    errorMessage = null
                )
            }
            return
        }

        sessionJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // Ensure question bank is loaded once
                if (!questionBankRepository.isLoaded) {
                    withContext(ioDispatcher) {
                        questionBankRepository.loadQuestionBank()
                    }
                }

                val availableSubjects = questionBankRepository.getAvailableSubjects()
                val currentSubj = filter.selectedSubject
                val availableMicrothemes = if (currentSubj != null) {
                    questionBankRepository.getAvailableMicrothemes(currentSubj)
                } else {
                    emptyList()
                }

                // Retrieve attempted question IDs efficiently in a single query
                val attemptedIds = withContext(ioDispatcher) {
                    practiceHistoryRepository.getAllAttemptedQuestionIds().toSet()
                }

                // Guard against stale asynchronous session results
                if (gen != sessionGeneration.get()) return@launch

                // Retrieve eligible questions according to active filter
                val eligibleQuestions = when (filter.mode) {
                    PracticeMode.RANDOM -> questionBankRepository.getAllQuestions()
                    PracticeMode.SUBJECT -> {
                        val subj = filter.selectedSubject ?: return@launch
                        val mt = filter.selectedMicrotheme
                        if (mt != null) {
                            questionBankRepository.getQuestionsForSubjectAndMicrotheme(subj, mt)
                        } else {
                            questionBankRepository.getQuestionsForSubject(subj)
                        }
                    }
                }

                // Exclude attempted questions
                val unattemptedQuestions = eligibleQuestions.filter { it.questionId !in attemptedIds }

                // Initialise new in-memory practice session
                val session = PracticeSession(unattemptedQuestions, random)
                currentSession = session

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        availableSubjects = availableSubjects,
                        availableMicrothemes = availableMicrothemes,
                        currentQuestion = session.currentQuestion,
                        isExhausted = session.isExhausted,
                        isAnswered = false,
                        selectedAnswer = null,
                        isAnswerCorrect = null,
                        correctAnswer = null,
                        isSkipEnabled = session.currentQuestion != null,
                        isNextEnabled = false,
                        sessionStats = session.stats
                    )
                }
            } catch (e: Exception) {
                if (gen == sessionGeneration.get()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load practice questions: ${e.message}"
                        )
                    }
                }
            }
        }
    }

    /**
     * Submits an answer choice ("A", "B", "C", or "D") for the current question.
     * Evaluates correctness and persists the attempt into local Room database asynchronously.
     */
    fun submitAnswer(answer: String) {
        val state = _uiState.value
        val currentQ = state.currentQuestion ?: return
        if (state.isAnswered || state.isSubmitting || state.isLoading) return

        val normalized = answer.trim().uppercase()
        if (normalized !in Question.VALID_OPTIONS) {
            _uiState.update { it.copy(errorMessage = "Invalid option '$answer'. Must be A, B, C, or D.") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            val questionId = currentQ.questionId
            val isCorrect = normalized == currentQ.correctAnswer.trim().uppercase()

            val result = practiceHistoryRepository.recordAttempt(
                questionId = questionId,
                selectedAnswer = normalized,
                isCorrect = isCorrect
            )

            if (result.isSuccess) {
                // Ensure question hasn't changed concurrently while persisting
                if (_uiState.value.currentQuestion?.questionId == questionId) {
                    currentSession?.recordAnswer(isCorrect)
                    val updatedStats = currentSession?.stats ?: _uiState.value.sessionStats

                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isAnswered = true,
                            selectedAnswer = normalized,
                            isAnswerCorrect = isCorrect,
                            correctAnswer = currentQ.correctAnswer,
                            isSkipEnabled = false,
                            isNextEnabled = true,
                            sessionStats = updatedStats
                        )
                    }
                }
            } else {
                // Persistence failed: do not mark attempted in memory, expose recoverable error
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
     * Skips the current question:
     * Question remains unattempted (not saved to Room).
     * Question is moved to session deferred pool and next random question is selected.
     */
    fun skipQuestion() {
        val state = _uiState.value
        if (state.isAnswered || state.isSubmitting || state.isLoading || state.currentQuestion == null) return

        val session = currentSession ?: return
        val nextQ = session.skipCurrent()

        _uiState.update {
            it.copy(
                currentQuestion = nextQ,
                isExhausted = session.isExhausted,
                isAnswered = false,
                selectedAnswer = null,
                isAnswerCorrect = null,
                correctAnswer = null,
                isSkipEnabled = nextQ != null,
                isNextEnabled = false,
                sessionStats = session.stats
            )
        }
    }

    /**
     * Advances to the next question after an answer has been recorded.
     */
    fun nextQuestion() {
        val state = _uiState.value
        if (!state.isAnswered || state.isSubmitting || state.isLoading) return

        val session = currentSession ?: return
        val nextQ = session.moveToNext()

        _uiState.update {
            it.copy(
                currentQuestion = nextQ,
                isExhausted = session.isExhausted,
                isAnswered = false,
                selectedAnswer = null,
                isAnswerCorrect = null,
                correctAnswer = null,
                isSkipEnabled = nextQ != null,
                isNextEnabled = false,
                sessionStats = session.stats
            )
        }
    }

    /**
     * Dismisses the current error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Restarts a session with the current filter settings.
     */
    fun restartSession() {
        startSession()
    }
}
