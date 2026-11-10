package com.csejudge.app.data.repository

import com.csejudge.app.data.local.AttemptedQuestionEntity
import com.csejudge.app.data.local.PracticeDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository interface for managing user attempt history.
 */
interface PracticeHistoryRepository {
    suspend fun recordAttempt(questionId: String, selectedAnswer: String, isCorrect: Boolean): Result<Unit>
    suspend fun isQuestionAttempted(questionId: String): Boolean
    suspend fun getAllAttemptedQuestionIds(): List<String>
    fun observeAllAttemptedQuestionIds(): Flow<List<String>>
    suspend fun getAttempt(questionId: String): AttemptedQuestionEntity?
    suspend fun getAttemptCount(): Int
    fun observeAttemptCount(): Flow<Int>
}

class PracticeHistoryRepositoryImpl(
    private val practiceDao: PracticeDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PracticeHistoryRepository {

    override suspend fun recordAttempt(
        questionId: String,
        selectedAnswer: String,
        isCorrect: Boolean
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val entity = AttemptedQuestionEntity(
                questionId = questionId,
                selectedAnswer = selectedAnswer,
                isCorrect = isCorrect,
                attemptedAt = System.currentTimeMillis()
            )
            practiceDao.insertAttempt(entity)
        }
    }

    override suspend fun isQuestionAttempted(questionId: String): Boolean = withContext(ioDispatcher) {
        practiceDao.isQuestionAttempted(questionId)
    }

    override suspend fun getAllAttemptedQuestionIds(): List<String> = withContext(ioDispatcher) {
        practiceDao.getAllAttemptedQuestionIds()
    }

    override fun observeAllAttemptedQuestionIds(): Flow<List<String>> {
        return practiceDao.observeAllAttemptedQuestionIds()
    }

    override suspend fun getAttempt(questionId: String): AttemptedQuestionEntity? = withContext(ioDispatcher) {
        practiceDao.getAttempt(questionId)
    }

    override suspend fun getAttemptCount(): Int = withContext(ioDispatcher) {
        practiceDao.getAttemptCount()
    }

    override fun observeAttemptCount(): Flow<Int> {
        return practiceDao.observeAttemptCount()
    }
}
