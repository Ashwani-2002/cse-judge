package com.csejudge.app.ui.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.csejudge.app.data.local.TestEntity
import com.csejudge.app.data.local.TestQuestionEntity
import com.csejudge.app.data.model.TestReport
import com.csejudge.app.data.model.TestType
import com.csejudge.app.data.repository.BookmarkRepository
import com.csejudge.app.data.repository.PracticeHistoryRepository
import com.csejudge.app.data.repository.QuestionBankRepository
import com.csejudge.app.data.repository.TestQuestionSelector
import com.csejudge.app.data.repository.TestRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class TestViewModel(
    private val questionBankRepository: QuestionBankRepository,
    private val practiceHistoryRepository: PracticeHistoryRepository,
    private val testRepository: TestRepository,
    private val bookmarkRepository: BookmarkRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(TestUiState())
    val uiState: StateFlow<TestUiState> = _uiState.asStateFlow()

    // Observes past completed tests reactively from Room
    val pastTests: StateFlow<List<TestEntity>> = testRepository.observeCompletedTests()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Observes bookmarked question IDs reactively
    val bookmarkedQuestionIds: StateFlow<Set<String>> = (bookmarkRepository?.observeAllBookmarkedQuestionIds()
        ?: MutableStateFlow(emptyList()))
        .map { it.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    private var timerJob: Job? = null
    private var testStartTime: Long = 0L
    private val isSubmittingGuard = AtomicBoolean(false)

    /**
     * Checks unattempted question availability for the specified test type.
     */
    fun checkEligibility(testType: TestType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, testType = testType, errorMessage = null) }
            try {
                if (!questionBankRepository.isLoaded) {
                    questionBankRepository.loadQuestionBank()
                }
                val attemptedIds = practiceHistoryRepository.getAllAttemptedQuestionIds().toSet()
                val allQuestions = questionBankRepository.getAllQuestions()
                val unattemptedCount = allQuestions.count { it.questionId !in attemptedIds }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        availableUnattemptedQuestions = unattemptedCount
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to check question bank: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Starts a new test session.
     * Selects unattempted questions using the Stage 13 distribution rules,
     * initializes timer (120:00 for FLT, 60:00 for HLT), and begins countdown.
     *
     * @param testType The test format (FLT or HLT).
     * @param overrideDurationSeconds Optional duration in seconds for testing/audit purposes.
     *                                If null, uses the production duration (7200s or 3600s).
     */
    fun startTest(testType: TestType, overrideDurationSeconds: Int? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    testType = testType,
                    isCompleted = false,
                    isAborted = false,
                    abortReason = null,
                    completedTestId = null,
                    currentReport = null,
                    errorMessage = null
                )
            }

            try {
                if (!questionBankRepository.isLoaded) {
                    questionBankRepository.loadQuestionBank()
                }
                val attemptedIds = practiceHistoryRepository.getAllAttemptedQuestionIds().toSet()
                val allQuestions = questionBankRepository.getAllQuestions()

                val selectedQuestions = withContext(ioDispatcher) {
                    TestQuestionSelector.selectQuestions(
                        allQuestions = allQuestions,
                        attemptedQuestionIds = attemptedIds,
                        testType = testType
                    )
                }

                testStartTime = System.currentTimeMillis()
                isSubmittingGuard.set(false)

                val initialSeconds = overrideDurationSeconds ?: testType.durationSeconds

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        testType = testType,
                        questions = selectedQuestions,
                        currentQuestionIndex = 0,
                        selectedAnswers = emptyMap(),
                        markedForReviewIndices = emptySet(),
                        remainingSeconds = initialSeconds,
                        isTimerActive = true,
                        isPaletteOpen = false,
                        showSubmitConfirmation = false,
                        showAbortConfirmation = false,
                        isSubmitting = false
                    )
                }

                startTimer()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to start test"
                    )
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0 && !_uiState.value.isCompleted && !_uiState.value.isAborted && !_uiState.value.isSubmitting) {
                delay(1000L)
                var reachedZero = false
                _uiState.update { state ->
                    if (state.remainingSeconds > 0 && !state.isCompleted && !state.isAborted && !state.isSubmitting) {
                        val newSeconds = state.remainingSeconds - 1
                        if (newSeconds == 0) {
                            reachedZero = true
                        }
                        state.copy(remainingSeconds = newSeconds)
                    } else {
                        state
                    }
                }
                if (reachedZero) {
                    break
                }
            }

            // Countdown reached zero -> auto-submit
            val state = _uiState.value
            if (state.remainingSeconds <= 0 && !state.isCompleted && !state.isAborted && !state.isSubmitting && state.questions.isNotEmpty()) {
                submitTest(isAutoSubmit = true)
            }
        }
    }

    /**
     * For testing / audit purposes only: allows setting remaining seconds in an active test session
     * without modifying production test configurations.
     */
    fun setRemainingSecondsForTesting(seconds: Int) {
        val state = _uiState.value
        if (state.questions.isNotEmpty() && !state.isCompleted && !state.isAborted && !state.isSubmitting) {
            _uiState.update { it.copy(remainingSeconds = seconds) }
        }
    }

    /**
     * Selects an option for the current question without revealing correctness.
     * Rejects selections if test is completed, aborted, submitting, or timer has expired.
     */
    fun selectOption(option: String) {
        val state = _uiState.value
        if (state.isCompleted || state.isAborted || state.isSubmitting || state.remainingSeconds <= 0 || isSubmittingGuard.get() || state.questions.isEmpty()) return

        val normalized = option.trim().uppercase()
        val currentAnswers = state.selectedAnswers.toMutableMap()
        currentAnswers[state.currentQuestionIndex] = normalized

        _uiState.update {
            it.copy(selectedAnswers = currentAnswers)
        }
    }

    /**
     * Toggles Mark for Review on the current question.
     * Works whether or not an answer has been selected.
     * Rejects updates if test is completed, aborted, submitting, or timer has expired.
     */
    fun toggleMarkForReview() {
        val state = _uiState.value
        if (state.isCompleted || state.isAborted || state.isSubmitting || state.remainingSeconds <= 0 || isSubmittingGuard.get() || state.questions.isEmpty()) return

        val currentMarks = state.markedForReviewIndices.toMutableSet()
        val currentIndex = state.currentQuestionIndex
        if (currentIndex in currentMarks) {
            currentMarks.remove(currentIndex)
        } else {
            currentMarks.add(currentIndex)
        }

        _uiState.update {
            it.copy(markedForReviewIndices = currentMarks)
        }
    }

    /**
     * Navigates directly to a question by 0-based index.
     * Closes the question palette.
     */
    fun navigateToQuestion(index: Int) {
        val state = _uiState.value
        if (state.isCompleted || state.isAborted || state.isSubmitting || state.remainingSeconds <= 0 || isSubmittingGuard.get()) return
        if (index in 0 until state.totalQuestions) {
            _uiState.update {
                it.copy(currentQuestionIndex = index, isPaletteOpen = false)
            }
        }
    }

    /**
     * Advances to the next question.
     * If on the final question, remains on the final question.
     */
    fun nextQuestion() {
        val state = _uiState.value
        if (state.isCompleted || state.isAborted || state.isSubmitting || state.remainingSeconds <= 0 || isSubmittingGuard.get()) return
        if (state.currentQuestionIndex < state.totalQuestions - 1) {
            _uiState.update {
                it.copy(currentQuestionIndex = state.currentQuestionIndex + 1)
            }
        }
    }

    fun openPalette() {
        val state = _uiState.value
        if (state.isCompleted || state.isAborted || state.isSubmitting || state.remainingSeconds <= 0 || isSubmittingGuard.get()) return
        _uiState.update { it.copy(isPaletteOpen = true) }
    }

    fun closePalette() {
        _uiState.update { it.copy(isPaletteOpen = false) }
    }

    fun showSubmitDialog() {
        val state = _uiState.value
        if (state.isCompleted || state.isAborted || state.isSubmitting || state.remainingSeconds <= 0 || isSubmittingGuard.get()) return
        _uiState.update { it.copy(showSubmitConfirmation = true) }
    }

    fun dismissSubmitDialog() {
        _uiState.update { it.copy(showSubmitConfirmation = false) }
    }

    fun showAbortDialog() {
        val state = _uiState.value
        if (state.isCompleted || state.isAborted || state.isSubmitting || state.remainingSeconds <= 0 || isSubmittingGuard.get()) return
        _uiState.update { it.copy(showAbortConfirmation = true) }
    }

    fun dismissAbortDialog() {
        _uiState.update { it.copy(showAbortConfirmation = false) }
    }

    /**
     * Submits the test either via explicit user confirmation or timer countdown expiry.
     * Evaluates answers, calculates score, and atomically persists the test report
     * and unified Practice attempt records.
     */
    fun submitTest(isAutoSubmit: Boolean = false) {
        if (!isSubmittingGuard.compareAndSet(false, true)) {
            return // Prevent duplicate submission
        }

        timerJob?.cancel()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    isTimerActive = false,
                    showSubmitConfirmation = false,
                    showAbortConfirmation = false,
                    isPaletteOpen = false
                )
            }

            try {
                val state = _uiState.value
                val testType = state.testType ?: TestType.FULL_LENGTH
                val completedAt = System.currentTimeMillis()
                val durationTakenSeconds = ((completedAt - testStartTime) / 1000).toInt()
                    .coerceAtMost(testType.durationSeconds)

                val testId = UUID.randomUUID().toString()

                var correctCount = 0
                var incorrectCount = 0
                var unansweredCount = 0

                val questionEntities = state.questions.mapIndexed { index, q ->
                    val selected = state.selectedAnswers[index]
                    val isMarked = index in state.markedForReviewIndices
                    val status = when {
                        selected == null -> {
                            unansweredCount++
                            "UNANSWERED"
                        }
                        selected.equals(q.correctAnswer.trim(), ignoreCase = true) -> {
                            correctCount++
                            "CORRECT"
                        }
                        else -> {
                            incorrectCount++
                            "INCORRECT"
                        }
                    }

                    TestQuestionEntity(
                        testId = testId,
                        questionOrder = index + 1,
                        questionId = q.questionId,
                        subject = q.subject,
                        microtheme = q.microtheme,
                        questionStatementJson = json.encodeToString(q.question),
                        optionsJson = json.encodeToString(q.options),
                        correctAnswer = q.correctAnswer,
                        solutionJson = json.encodeToString(q.solution),
                        selectedAnswer = selected,
                        isMarkedForReview = isMarked,
                        status = status
                    )
                }

                val attemptedCount = correctCount + incorrectCount
                val score = (correctCount * 2.0) - (incorrectCount * (2.0 / 3.0))
                val accuracy = if (attemptedCount > 0) {
                    (correctCount.toDouble() / attemptedCount.toDouble()) * 100.0
                } else {
                    -1.0
                }

                val testEntity = TestEntity(
                    testId = testId,
                    testType = testType.name,
                    startedAt = testStartTime,
                    completedAt = completedAt,
                    durationSeconds = durationTakenSeconds,
                    totalQuestions = state.totalQuestions,
                    attemptedCount = attemptedCount,
                    correctCount = correctCount,
                    incorrectCount = incorrectCount,
                    unansweredCount = unansweredCount,
                    score = score,
                    maxScore = testType.maxScore,
                    accuracy = accuracy,
                    status = "COMPLETED"
                )

                // Atomic transaction: test summary + question snapshots + unified practice attempts
                val result = testRepository.saveCompletedTest(testEntity, questionEntities)

                if (result.isSuccess) {
                    val report = testRepository.getTestReport(testId)
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isCompleted = true,
                            completedTestId = testId,
                            currentReport = report
                        )
                    }
                } else {
                    isSubmittingGuard.set(false)
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Failed to save test result: ${result.exceptionOrNull()?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                isSubmittingGuard.set(false)
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Error during test submission: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Aborts an active test session immediately.
     * Invoked when the user leaves the app (backgrounding) or exits via back button.
     * Discards all answers, cancels timer, and does not save any test report or practice attempts.
     * Cannot abort if test is already completed, submitting, or timer has expired.
     */
    fun abortTest(reason: String = "Test aborted") {
        val state = _uiState.value
        // Only abort if a test is genuinely active and not yet completed or submitting
        if (state.questions.isEmpty() || state.isCompleted || state.isSubmitting || state.remainingSeconds <= 0 || isSubmittingGuard.get()) return

        timerJob?.cancel()
        isSubmittingGuard.set(false)

        _uiState.update {
            it.copy(
                isTimerActive = false,
                questions = emptyList(),
                selectedAnswers = emptyMap(),
                markedForReviewIndices = emptySet(),
                isPaletteOpen = false,
                showSubmitConfirmation = false,
                showAbortConfirmation = false,
                isSubmitting = false,
                isCompleted = false,
                isAborted = true,
                abortReason = reason
            )
        }
    }

    /**
     * Clears the abort state and reason banner.
     */
    fun clearAbortedState() {
        _uiState.update {
            it.copy(isAborted = false, abortReason = null)
        }
    }

    /**
     * Loads a completed test report by test ID for historical review.
     */
    fun loadTestReport(testId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val report = testRepository.getTestReport(testId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    completedTestId = testId,
                    currentReport = report
                )
            }
        }
    }

    /**
     * Toggles bookmark for a question from the completed Test Report screen.
     */
    fun toggleBookmark(questionId: String) {
        viewModelScope.launch {
            val repository = bookmarkRepository ?: return@launch
            val isBookmarked = repository.isBookmarked(questionId)
            if (isBookmarked) {
                repository.removeBookmark(questionId)
            } else {
                repository.addBookmark(questionId)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
