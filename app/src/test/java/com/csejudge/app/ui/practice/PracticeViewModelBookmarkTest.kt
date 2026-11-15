package com.csejudge.app.ui.practice

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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelBookmarkTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var fakeQuestionBankRepo: FakeQuestionBankRepo
    private lateinit var fakeHistoryRepo: FakeHistoryRepo
    private lateinit var fakeBookmarkRepo: FakeBookmarkRepo

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeQuestionBankRepo = FakeQuestionBankRepo()
        fakeHistoryRepo = FakeHistoryRepo()
        fakeBookmarkRepo = FakeBookmarkRepo()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PracticeViewModel {
        return PracticeViewModel(
            questionBankRepository = fakeQuestionBankRepo,
            practiceHistoryRepository = fakeHistoryRepo,
            bookmarkRepository = fakeBookmarkRepo,
            ioDispatcher = testDispatcher,
            random = Random(42)
        )
    }

    @Test
    fun testInitialQuestionBookmarkStatusIsLoadedAccurately() = runTest(testDispatcher) {
        // Pre-bookmark POL00001
        fakeBookmarkRepo.addBookmark("POL00001")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.selectSubject("Polity")
        vm.selectMicrotheme("Preamble")
        advanceUntilIdle()

        val q = vm.uiState.value.currentQuestion
        assertNotNull(q)
        if (q?.questionId == "POL00001") {
            assertTrue("POL00001 should be marked bookmarked in UI state", vm.uiState.value.isBookmarked)
        } else {
            assertFalse(vm.uiState.value.isBookmarked)
        }
    }

    @Test
    fun testTogglingBookmarkUpdatesUIStateAndRepository() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val q = vm.uiState.value.currentQuestion!!
        assertFalse(vm.uiState.value.isBookmarked)
        assertFalse(fakeBookmarkRepo.isBookmarked(q.questionId))

        // Toggle ON
        vm.toggleBookmark()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isBookmarked)
        assertTrue(fakeBookmarkRepo.isBookmarked(q.questionId))

        // Toggle OFF
        vm.toggleBookmark()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isBookmarked)
        assertFalse(fakeBookmarkRepo.isBookmarked(q.questionId))
    }

    @Test
    fun testBookmarkStatusUpdatesWhenAdvancingQuestions() = runTest(testDispatcher) {
        // Pre-bookmark POL00002
        fakeBookmarkRepo.addBookmark("POL00002")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.selectSubject("Polity")
        vm.selectMicrotheme("Preamble")
        advanceUntilIdle()

        // Say first question is POL00001 (not bookmarked)
        if (vm.uiState.value.currentQuestion?.questionId == "POL00001") {
            assertFalse(vm.uiState.value.isBookmarked)
            vm.skipQuestion()
            advanceUntilIdle()
            assertEquals("POL00002", vm.uiState.value.currentQuestion?.questionId)
            assertTrue("POL00002 should reflect bookmarked state after skip", vm.uiState.value.isBookmarked)
        }
    }

    @Test
    fun testBookmarksSurviveFilterChanges() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val q = vm.uiState.value.currentQuestion!!
        vm.toggleBookmark()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isBookmarked)

        // Change filters
        vm.selectSubject("Economy")
        advanceUntilIdle()
        vm.selectSubject("Polity")
        advanceUntilIdle()

        // Bookmark record in repo is intact
        assertTrue(fakeBookmarkRepo.isBookmarked(q.questionId))
    }

    // ==========================================
    // Fakes
    // ==========================================

    private class FakeBookmarkRepo : BookmarkRepository {
        val bookmarks = mutableMapOf<String, BookmarkedQuestionEntity>()

        override suspend fun addBookmark(questionId: String, timestamp: Long): Result<Unit> {
            bookmarks[questionId] = BookmarkedQuestionEntity(questionId, timestamp)
            return Result.success(Unit)
        }

        override suspend fun removeBookmark(questionId: String): Result<Unit> {
            bookmarks.remove(questionId)
            return Result.success(Unit)
        }

        override suspend fun isBookmarked(questionId: String): Boolean = bookmarks.containsKey(questionId)
        override suspend fun getAllBookmarkedQuestionIds(): List<String> = bookmarks.keys.toList()
        override fun observeAllBookmarkedQuestionIds(): Flow<List<String>> = flowOf(bookmarks.keys.toList())
        override suspend fun getAllBookmarks(): List<BookmarkedQuestionEntity> = bookmarks.values.toList()
        override fun observeAllBookmarks(): Flow<List<BookmarkedQuestionEntity>> = flowOf(bookmarks.values.toList())
        override suspend fun getBookmarkCount(): Int = bookmarks.size
        override fun observeBookmarkCount(): Flow<Int> = flowOf(bookmarks.size)
    }

    private class FakeQuestionBankRepo : QuestionBankRepository {
        override var isLoaded: Boolean = true

        private val questions = listOf(
            createQ("POL00001", "Polity", "Preamble"),
            createQ("POL00002", "Polity", "Preamble"),
            createQ("ECO00001", "Economy", "Banking")
        )

        private fun createQ(id: String, subj: String, mt: String) = Question(
            questionId = id,
            subject = subj,
            microtheme = mt,
            question = listOf("Statement for $id"),
            options = Options("A", "B", "C", "D"),
            correctAnswer = "A",
            solution = listOf("Solution for $id")
        )

        override suspend fun loadQuestionBank() = QuestionBankStats(questions.size, emptyMap(), emptyMap(), 0)
        override fun getAllQuestions() = questions
        override fun getQuestionById(questionId: String) = questions.find { it.questionId == questionId }
        override fun getAvailableSubjects() = questions.map { it.subject }.distinct()
        override fun getAvailableMicrothemes(subject: String) = questions.filter { it.subject == subject }.map { it.microtheme }.distinct()
        override fun getQuestionsForSubject(subject: String) = questions.filter { it.subject == subject }
        override fun getQuestionsForSubjectAndMicrotheme(subject: String, microtheme: String) =
            questions.filter { it.subject == subject && it.microtheme == microtheme }
    }

    private class FakeHistoryRepo : PracticeHistoryRepository {
        val attempts = mutableMapOf<String, AttemptedQuestionEntity>()

        override suspend fun recordAttempt(questionId: String, selectedAnswer: String, isCorrect: Boolean): Result<Unit> {
            attempts[questionId] = AttemptedQuestionEntity(questionId, selectedAnswer, isCorrect)
            return Result.success(Unit)
        }

        override suspend fun recordOrUpdateAttempt(
            questionId: String,
            selectedAnswer: String,
            isCorrect: Boolean,
            attemptedAt: Long
        ): Result<Unit> {
            attempts[questionId] = AttemptedQuestionEntity(questionId, selectedAnswer, isCorrect, attemptedAt)
            return Result.success(Unit)
        }

        override suspend fun isQuestionAttempted(questionId: String) = attempts.containsKey(questionId)
        override suspend fun getAllAttemptedQuestionIds() = attempts.keys.toList()
        override fun observeAllAttemptedQuestionIds() = flowOf(attempts.keys.toList())
        override suspend fun getAttempt(questionId: String) = attempts[questionId]
        override suspend fun getAttemptCount() = attempts.size
        override fun observeAttemptCount() = flowOf(attempts.size)
        override suspend fun getAllAttempts(): List<AttemptedQuestionEntity> = attempts.values.toList()
        override fun observeAllAttempts(): Flow<List<AttemptedQuestionEntity>> = flowOf(attempts.values.toList())
    }
}
