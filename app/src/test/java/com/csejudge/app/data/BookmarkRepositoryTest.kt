package com.csejudge.app.data

import com.csejudge.app.data.local.BookmarkDao
import com.csejudge.app.data.local.BookmarkedQuestionEntity
import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.repository.BookmarkRepository
import com.csejudge.app.data.repository.BookmarkRepositoryImpl
import com.csejudge.app.data.repository.QuestionBankRepository
import com.csejudge.app.data.repository.QuestionBankStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BookmarkRepositoryTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeBookmarkDao
    private lateinit var fakeQuestionBankRepo: FakeQuestionBankRepo
    private lateinit var repository: BookmarkRepository

    @Before
    fun setUp() {
        fakeDao = FakeBookmarkDao()
        fakeQuestionBankRepo = FakeQuestionBankRepo()
        repository = BookmarkRepositoryImpl(
            bookmarkDao = fakeDao,
            questionBankRepository = fakeQuestionBankRepo,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun testAddBookmarkSuccessfully() = runTest(testDispatcher) {
        val result = repository.addBookmark("POL00001")
        assertTrue(result.isSuccess)
        assertTrue(repository.isBookmarked("POL00001"))
        assertEquals(1, repository.getBookmarkCount())
    }

    @Test
    fun testRemoveBookmarkSuccessfully() = runTest(testDispatcher) {
        repository.addBookmark("POL00001")
        assertTrue(repository.isBookmarked("POL00001"))

        val removeResult = repository.removeBookmark("POL00001")
        assertTrue(removeResult.isSuccess)
        assertFalse(repository.isBookmarked("POL00001"))
        assertEquals(0, repository.getBookmarkCount())
    }

    @Test
    fun testAddSameBookmarkTwiceDoesNotCreateDuplicates() = runTest(testDispatcher) {
        val r1 = repository.addBookmark("POL00001", timestamp = 1000L)
        val r2 = repository.addBookmark("POL00001", timestamp = 2000L)

        assertTrue(r1.isSuccess)
        assertTrue(r2.isSuccess)
        assertEquals(1, repository.getBookmarkCount())
        assertEquals(listOf("POL00001"), repository.getAllBookmarkedQuestionIds())
    }

    @Test
    fun testCheckingBookmarkedStatusReturnsAccurately() = runTest(testDispatcher) {
        assertFalse(repository.isBookmarked("POL00001"))
        repository.addBookmark("POL00001")
        assertTrue(repository.isBookmarked("POL00001"))
        assertFalse(repository.isBookmarked("POL00002"))
    }

    @Test
    fun testRetrievingAllBookmarksOrderedNewestFirst() = runTest(testDispatcher) {
        repository.addBookmark("POL00001", timestamp = 1000L)
        repository.addBookmark("ECO00001", timestamp = 3000L)
        repository.addBookmark("GEO00001", timestamp = 2000L)

        val ids = repository.getAllBookmarkedQuestionIds()
        // Expected newest first: ECO00001 (3000), GEO00001 (2000), POL00001 (1000)
        assertEquals(listOf("ECO00001", "GEO00001", "POL00001"), ids)
    }

    @Test
    fun testInvalidBlankQuestionIdIsRejected() = runTest(testDispatcher) {
        val result = repository.addBookmark("   ")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(0, repository.getBookmarkCount())
    }

    @Test
    fun testNonExistentQuestionIdIsRejected() = runTest(testDispatcher) {
        val result = repository.addBookmark("NON_EXISTENT_ID")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("does not exist") == true)
        assertEquals(0, repository.getBookmarkCount())
    }

    @Test
    fun testRemovingNonExistentBookmarkIsHarmless() = runTest(testDispatcher) {
        val result = repository.removeBookmark("POL00001")
        assertTrue(result.isSuccess)
        assertFalse(repository.isBookmarked("POL00001"))
    }

    @Test
    fun testObservingBookmarksFlowEmitsUpdates() = runTest(testDispatcher) {
        repository.addBookmark("POL00001")
        val ids = repository.observeAllBookmarkedQuestionIds().first()
        assertEquals(listOf("POL00001"), ids)
    }

    // ==========================================
    // Fakes
    // ==========================================

    private class FakeBookmarkDao : BookmarkDao {
        val bookmarks = mutableMapOf<String, BookmarkedQuestionEntity>()

        override suspend fun insertBookmark(bookmark: BookmarkedQuestionEntity) {
            bookmarks[bookmark.questionId] = bookmark
        }

        override suspend fun deleteBookmark(questionId: String): Int {
            return if (bookmarks.remove(questionId) != null) 1 else 0
        }

        override suspend fun isBookmarked(questionId: String): Boolean {
            return bookmarks.containsKey(questionId)
        }

        override suspend fun getAllBookmarkedQuestionIds(): List<String> {
            return bookmarks.values
                .sortedByDescending { it.bookmarkedAt }
                .map { it.questionId }
        }

        override fun observeAllBookmarkedQuestionIds(): Flow<List<String>> {
            return flowOf(getAllSyncIds())
        }

        private fun getAllSyncIds(): List<String> {
            return bookmarks.values
                .sortedByDescending { it.bookmarkedAt }
                .map { it.questionId }
        }

        override suspend fun getAllBookmarks(): List<BookmarkedQuestionEntity> {
            return bookmarks.values.sortedByDescending { it.bookmarkedAt }
        }

        override fun observeAllBookmarks(): Flow<List<BookmarkedQuestionEntity>> {
            return flowOf(bookmarks.values.sortedByDescending { it.bookmarkedAt })
        }

        override suspend fun getBookmarkCount(): Int = bookmarks.size

        override fun observeBookmarkCount(): Flow<Int> = flowOf(bookmarks.size)
    }

    private class FakeQuestionBankRepo : QuestionBankRepository {
        override var isLoaded: Boolean = true

        private val testQuestions = listOf(
            createQ("POL00001"),
            createQ("POL00002"),
            createQ("ECO00001"),
            createQ("GEO00001")
        )

        private fun createQ(id: String) = Question(
            questionId = id,
            subject = "Subject",
            microtheme = "Microtheme",
            question = listOf("Statement for $id"),
            options = Options("A", "B", "C", "D"),
            correctAnswer = "A",
            solution = listOf("Solution for $id")
        )

        override suspend fun loadQuestionBank() = QuestionBankStats(testQuestions.size, emptyMap(), emptyMap(), 0)
        override fun getAllQuestions() = testQuestions
        override fun getQuestionById(questionId: String) = testQuestions.find { it.questionId == questionId }
        override fun getAvailableSubjects() = emptyList<String>()
        override fun getAvailableMicrothemes(subject: String) = emptyList<String>()
        override fun getQuestionsForSubject(subject: String) = emptyList<Question>()
        override fun getQuestionsForSubjectAndMicrotheme(subject: String, microtheme: String) = emptyList<Question>()
    }
}
