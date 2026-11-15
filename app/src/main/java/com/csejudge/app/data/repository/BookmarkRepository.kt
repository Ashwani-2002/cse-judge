package com.csejudge.app.data.repository

import com.csejudge.app.data.local.BookmarkDao
import com.csejudge.app.data.local.BookmarkedQuestionEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository interface for managing user question bookmarks.
 */
interface BookmarkRepository {
    suspend fun addBookmark(questionId: String, timestamp: Long = System.currentTimeMillis()): Result<Unit>
    suspend fun removeBookmark(questionId: String): Result<Unit>
    suspend fun isBookmarked(questionId: String): Boolean
    suspend fun getAllBookmarkedQuestionIds(): List<String>
    fun observeAllBookmarkedQuestionIds(): Flow<List<String>>
    suspend fun getAllBookmarks(): List<BookmarkedQuestionEntity>
    fun observeAllBookmarks(): Flow<List<BookmarkedQuestionEntity>>
    suspend fun getBookmarkCount(): Int
    fun observeBookmarkCount(): Flow<Int>
}

class BookmarkRepositoryImpl(
    private val bookmarkDao: BookmarkDao,
    private val questionBankRepository: QuestionBankRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BookmarkRepository {

    override suspend fun addBookmark(questionId: String, timestamp: Long): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val trimmedId = questionId.trim()
            if (trimmedId.isBlank()) {
                throw IllegalArgumentException("Question ID cannot be blank")
            }

            // Ensure question bank is loaded before validating existence
            if (!questionBankRepository.isLoaded) {
                questionBankRepository.loadQuestionBank()
            }

            val question = questionBankRepository.getQuestionById(trimmedId)
                ?: throw IllegalArgumentException("Question '$trimmedId' does not exist in the question bank")

            val entity = BookmarkedQuestionEntity(
                questionId = question.questionId,
                bookmarkedAt = timestamp
            )
            bookmarkDao.insertBookmark(entity)
        }
    }

    override suspend fun removeBookmark(questionId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val trimmedId = questionId.trim()
            bookmarkDao.deleteBookmark(trimmedId)
            Unit
        }
    }

    override suspend fun isBookmarked(questionId: String): Boolean = withContext(ioDispatcher) {
        bookmarkDao.isBookmarked(questionId.trim())
    }

    override suspend fun getAllBookmarkedQuestionIds(): List<String> = withContext(ioDispatcher) {
        bookmarkDao.getAllBookmarkedQuestionIds()
    }

    override fun observeAllBookmarkedQuestionIds(): Flow<List<String>> {
        return bookmarkDao.observeAllBookmarkedQuestionIds()
    }

    override suspend fun getAllBookmarks(): List<BookmarkedQuestionEntity> = withContext(ioDispatcher) {
        bookmarkDao.getAllBookmarks()
    }

    override fun observeAllBookmarks(): Flow<List<BookmarkedQuestionEntity>> {
        return bookmarkDao.observeAllBookmarks()
    }

    override suspend fun getBookmarkCount(): Int = withContext(ioDispatcher) {
        bookmarkDao.getBookmarkCount()
    }

    override fun observeBookmarkCount(): Flow<Int> {
        return bookmarkDao.observeBookmarkCount()
    }
}
