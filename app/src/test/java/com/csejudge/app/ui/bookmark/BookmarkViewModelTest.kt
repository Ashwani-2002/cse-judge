package com.csejudge.app.ui.bookmark

import com.csejudge.app.data.local.AttemptedQuestionEntity
import com.csejudge.app.data.local.BookmarkedQuestionEntity
import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.repository.BookmarkRepository
import com.csejudge.app.data.repository.PracticeHistoryRepository
import com.csejudge.app.data.repository.QuestionBankRepository
import com.csejudge.app.data.repository.QuestionBankStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeQuestionBankRepo: FakeQuestionBankRepository
    private lateinit var fakeBookmarkRepo: FakeBookmarkRepository
    private lateinit var fakePracticeRepo: FakePracticeHistoryRepository
    private lateinit var viewModel: BookmarkViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeQuestionBankRepo = FakeQuestionBankRepository()
        fakeBookmarkRepo = FakeBookmarkRepository()
        fakePracticeRepo = FakePracticeHistoryRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun initViewModel(): BookmarkViewModel {
        return BookmarkViewModel(
            questionBankRepository = fakeQuestionBankRepo,
            bookmarkRepository = fakeBookmarkRepo,
            practiceHistoryRepository = fakePracticeRepo,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun testEmptyBookmarkStateInitialisation() = runTest(testDispatcher) {
        viewModel = initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.totalBookmarkCount)
        assertTrue(state.subjects.isEmpty())
        assertNull(state.selectedSubject)
        assertTrue(state.microthemes.isEmpty())
        assertNull(state.selectedMicrotheme)
        assertTrue(state.questions.isEmpty())
        assertNull(state.currentQuestionState)
    }

    @Test
    fun testObservesBookmarkCountAndSubjectGrouping() = runTest(testDispatcher) {
        // Pre-populate bookmarks in Polity (2 questions) and Economy (1 question)
        fakeBookmarkRepo.addBookmark("POL00001")
        fakeBookmarkRepo.addBookmark("POL00002")
        fakeBookmarkRepo.addBookmark("ECO00001")

        viewModel = initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.totalBookmarkCount)
        assertEquals(2, state.subjects.size)

        val politySubject = state.subjects.find { it.subject == "Polity" }
        assertNotNull(politySubject)
        assertEquals(2, politySubject?.questionCount)

        val ecoSubject = state.subjects.find { it.subject == "Economy" }
        assertNotNull(ecoSubject)
        assertEquals(1, ecoSubject?.questionCount)

        // Modern History has 0 bookmarks, must NOT be present
        assertNull(state.subjects.find { it.subject == "Modern History" })
    }

    @Test
    fun testSelectSubjectPopulatesOnlyMicrothemesWithBookmarks() = runTest(testDispatcher) {
        // POL00001 is Preamble; POL00002 is Fundamental Rights; POL00003 is Preamble (unbookmarked)
        fakeBookmarkRepo.addBookmark("POL00001")
        fakeBookmarkRepo.addBookmark("POL00002")

        viewModel = initViewModel()
        advanceUntilIdle()

        viewModel.selectSubject("Polity")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Polity", state.selectedSubject)
        assertEquals(2, state.microthemes.size)

        val preambleTheme = state.microthemes.find { it.microtheme == "Preamble" }
        assertNotNull(preambleTheme)
        assertEquals(1, preambleTheme?.questionCount)

        val rightsTheme = state.microthemes.find { it.microtheme == "Fundamental Rights" }
        assertNotNull(rightsTheme)
        assertEquals(1, rightsTheme?.questionCount)
    }

    @Test
    fun testSelectMicrothemeLoadsBookmarkedQuestionsAndDetectsUnattemptedState() = runTest(testDispatcher) {
        fakeBookmarkRepo.addBookmark("POL00001")

        viewModel = initViewModel()
        advanceUntilIdle()

        viewModel.selectSubject("Polity")
        advanceUntilIdle()

        viewModel.selectMicrotheme("Polity", "Preamble")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.questions.size)
        assertEquals("POL00001", state.questions[0].questionId)

        val qState = state.currentQuestionState
        assertNotNull(qState)
        assertEquals("POL00001", qState?.question?.questionId)
        assertTrue(qState?.isBookmarked == true)
        // Question was never attempted
        assertFalse(qState?.isAttempted == true)
        assertNull(qState?.selectedAnswer)
        assertNull(qState?.isCorrect)
    }

    @Test
    fun testPreviouslyAttemptedQuestionLoadsAttemptDetails() = runTest(testDispatcher) {
        // User previously attempted POL00001 with answer "A" (which is correct)
        fakePracticeRepo.recordAttempt("POL00001", "A", true)
        fakeBookmarkRepo.addBookmark("POL00001")

        viewModel = initViewModel()
        advanceUntilIdle()

        viewModel.selectMicrotheme("Polity", "Preamble")
        advanceUntilIdle()

        val qState = viewModel.uiState.value.currentQuestionState
        assertNotNull(qState)
        assertTrue(qState?.isAttempted == true)
        assertEquals("A", qState?.selectedAnswer)
        assertEquals(true, qState?.isCorrect)
    }

    @Test
    fun testReattemptUpdatesExistingAttemptWithoutDuplicates() = runTest(testDispatcher) {
        // User initially answered incorrectly with "B" (correct is "A")
        fakePracticeRepo.recordAttempt("POL00001", "B", false)
        fakeBookmarkRepo.addBookmark("POL00001")

        viewModel = initViewModel()
        advanceUntilIdle()

        viewModel.selectMicrotheme("Polity", "Preamble")
        advanceUntilIdle()

        assertEquals("B", viewModel.uiState.value.currentQuestionState?.selectedAnswer)
        assertEquals(false, viewModel.uiState.value.currentQuestionState?.isCorrect)

        // User re-attempts with "A"
        viewModel.answerQuestion("A")
        advanceUntilIdle()

        val qState = viewModel.uiState.value.currentQuestionState
        assertEquals("A", qState?.selectedAnswer)
        assertEquals(true, qState?.isCorrect)

        // Verify repository was updated with upsert and has NO duplicate entries
        assertEquals(1, fakePracticeRepo.attempts.size)
        val updatedAttempt = fakePracticeRepo.attempts["POL00001"]
        assertNotNull(updatedAttempt)
        assertEquals("A", updatedAttempt?.selectedAnswer)
        assertEquals(true, updatedAttempt?.isCorrect)
    }

    @Test
    fun testBookmarkRemovalPreservesAttemptHistory() = runTest(testDispatcher) {
        // Question is bookmarked AND attempted
        fakePracticeRepo.recordAttempt("POL00001", "A", true)
        fakeBookmarkRepo.addBookmark("POL00001")

        viewModel = initViewModel()
        advanceUntilIdle()

        viewModel.selectMicrotheme("Polity", "Preamble")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.currentQuestionState?.isBookmarked == true)
        assertTrue(viewModel.uiState.value.currentQuestionState?.isAttempted == true)

        // User unbookmarks the question
        viewModel.toggleBookmarkCurrentQuestion()
        advanceUntilIdle()

        // Question is now unbookmarked
        assertFalse(viewModel.uiState.value.currentQuestionState?.isBookmarked == true)
        assertFalse(fakeBookmarkRepo.isBookmarked("POL00001"))

        // BUT attempt history MUST be preserved!
        assertTrue(viewModel.uiState.value.currentQuestionState?.isAttempted == true)
        assertEquals("A", viewModel.uiState.value.currentQuestionState?.selectedAnswer)
        assertNotNull(fakePracticeRepo.attempts["POL00001"])

        // If user bookmarks again, attempt history is still present
        viewModel.toggleBookmarkCurrentQuestion()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.currentQuestionState?.isBookmarked == true)
        assertTrue(viewModel.uiState.value.currentQuestionState?.isAttempted == true)
        assertEquals("A", viewModel.uiState.value.currentQuestionState?.selectedAnswer)
    }

    @Test
    fun testQuestionNavigationNextAndPrevious() = runTest(testDispatcher) {
        fakeBookmarkRepo.addBookmark("POL00001")
        fakeBookmarkRepo.addBookmark("POL00004") // also in Preamble

        viewModel = initViewModel()
        advanceUntilIdle()

        viewModel.selectMicrotheme("Polity", "Preamble")
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.currentQuestionIndex)
        assertEquals("POL00001", viewModel.uiState.value.currentQuestionState?.question?.questionId)
        assertFalse(viewModel.uiState.value.hasPreviousQuestion)
        assertTrue(viewModel.uiState.value.hasNextQuestion)

        viewModel.nextQuestion()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.currentQuestionIndex)
        assertEquals("POL00004", viewModel.uiState.value.currentQuestionState?.question?.questionId)
        assertTrue(viewModel.uiState.value.hasPreviousQuestion)
        assertFalse(viewModel.uiState.value.hasNextQuestion)

        viewModel.previousQuestion()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.currentQuestionIndex)
        assertEquals("POL00001", viewModel.uiState.value.currentQuestionState?.question?.questionId)
    }

    // ==========================================
    // Test Fakes
    // ==========================================

    private class FakeQuestionBankRepository : QuestionBankRepository {
        override var isLoaded: Boolean = true

        private val allQuestions = listOf(
            createTestQuestion("POL00001", "Polity", "Preamble", "A"),
            createTestQuestion("POL00002", "Polity", "Fundamental Rights", "B"),
            createTestQuestion("POL00003", "Polity", "Preamble", "C"),
            createTestQuestion("POL00004", "Polity", "Preamble", "D"),
            createTestQuestion("ECO00001", "Economy", "Banking", "C"),
            createTestQuestion("MH00001", "Modern History", "1857 Revolt", "D")
        )

        private fun createTestQuestion(id: String, subject: String, microtheme: String, correct: String): Question {
            return Question(
                questionId = id,
                subject = subject,
                microtheme = microtheme,
                question = listOf("Statement 1 for $id", "Statement 2 for $id"),
                options = Options("Option A", "Option B", "Option C", "Option D"),
                correctAnswer = correct,
                solution = listOf("Solution for $id")
            )
        }

        override suspend fun loadQuestionBank(): QuestionBankStats {
            isLoaded = true
            return QuestionBankStats(allQuestions.size, emptyMap(), emptyMap(), 0)
        }

        override fun getAllQuestions(): List<Question> = allQuestions
        override fun getQuestionById(questionId: String): Question? = allQuestions.find { it.questionId == questionId }
        override fun getAvailableSubjects(): List<String> = allQuestions.map { it.subject }.distinct()
        override fun getAvailableMicrothemes(subject: String): List<String> =
            allQuestions.filter { it.subject == subject }.map { it.microtheme }.distinct()
        override fun getQuestionsForSubject(subject: String): List<Question> =
            allQuestions.filter { it.subject == subject }
        override fun getQuestionsForSubjectAndMicrotheme(subject: String, microtheme: String): List<Question> =
            allQuestions.filter { it.subject == subject && it.microtheme == microtheme }
    }

    private class FakeBookmarkRepository : BookmarkRepository {
        private val bookmarks = mutableMapOf<String, Long>()
        private val countFlow = MutableStateFlow(0)
        private val idsFlow = MutableStateFlow<List<String>>(emptyList())

        override suspend fun addBookmark(questionId: String, timestamp: Long): Result<Unit> {
            bookmarks[questionId] = timestamp
            notifyFlows()
            return Result.success(Unit)
        }

        override suspend fun removeBookmark(questionId: String): Result<Unit> {
            bookmarks.remove(questionId)
            notifyFlows()
            return Result.success(Unit)
        }

        override suspend fun isBookmarked(questionId: String): Boolean = bookmarks.containsKey(questionId)
        override suspend fun getAllBookmarkedQuestionIds(): List<String> = bookmarks.keys.toList()
        override fun observeAllBookmarkedQuestionIds(): Flow<List<String>> = idsFlow.asStateFlow()
        override suspend fun getAllBookmarks(): List<BookmarkedQuestionEntity> =
            bookmarks.map { BookmarkedQuestionEntity(it.key, it.value) }
        override fun observeAllBookmarks(): Flow<List<BookmarkedQuestionEntity>> =
            MutableStateFlow(bookmarks.map { BookmarkedQuestionEntity(it.key, it.value) })
        override suspend fun getBookmarkCount(): Int = bookmarks.size
        override fun observeBookmarkCount(): Flow<Int> = countFlow.asStateFlow()

        private fun notifyFlows() {
            countFlow.value = bookmarks.size
            idsFlow.value = bookmarks.keys.toList()
        }
    }

    private class FakePracticeHistoryRepository : PracticeHistoryRepository {
        val attempts = mutableMapOf<String, AttemptedQuestionEntity>()
        private val attemptedIdsFlow = MutableStateFlow<List<String>>(emptyList())
        private val countFlow = MutableStateFlow(0)
        private val attemptsFlow = MutableStateFlow<List<AttemptedQuestionEntity>>(emptyList())

        override suspend fun recordAttempt(
            questionId: String,
            selectedAnswer: String,
            isCorrect: Boolean
        ): Result<Unit> {
            val entity = AttemptedQuestionEntity(
                questionId = questionId,
                selectedAnswer = selectedAnswer,
                isCorrect = isCorrect,
                attemptedAt = System.currentTimeMillis()
            )
            attempts[questionId] = entity
            notifyFlows()
            return Result.success(Unit)
        }

        override suspend fun recordOrUpdateAttempt(
            questionId: String,
            selectedAnswer: String,
            isCorrect: Boolean,
            attemptedAt: Long
        ): Result<Unit> {
            val entity = AttemptedQuestionEntity(
                questionId = questionId,
                selectedAnswer = selectedAnswer,
                isCorrect = isCorrect,
                attemptedAt = attemptedAt
            )
            attempts[questionId] = entity
            notifyFlows()
            return Result.success(Unit)
        }

        override suspend fun isQuestionAttempted(questionId: String): Boolean = attempts.containsKey(questionId)
        override suspend fun getAllAttemptedQuestionIds(): List<String> = attempts.keys.toList()
        override fun observeAllAttemptedQuestionIds(): Flow<List<String>> = attemptedIdsFlow.asStateFlow()
        override suspend fun getAttempt(questionId: String): AttemptedQuestionEntity? = attempts[questionId]
        override suspend fun getAttemptCount(): Int = attempts.size
        override fun observeAttemptCount(): Flow<Int> = countFlow.asStateFlow()
        override suspend fun getAllAttempts(): List<AttemptedQuestionEntity> = attempts.values.toList()
        override fun observeAllAttempts(): Flow<List<AttemptedQuestionEntity>> = attemptsFlow.asStateFlow()

        private fun notifyFlows() {
            attemptedIdsFlow.value = attempts.keys.toList()
            countFlow.value = attempts.size
            attemptsFlow.value = attempts.values.toList()
        }
    }
}
