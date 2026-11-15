package com.csejudge.app.data

import com.csejudge.app.data.local.AttemptedQuestionEntity
import com.csejudge.app.data.local.PracticeDao
import com.csejudge.app.data.repository.PracticeHistoryRepository
import com.csejudge.app.data.repository.PracticeHistoryRepositoryImpl
import kotlinx.coroutines.flow.Flow
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

class PracticeHistoryReattemptTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakePracticeDao
    private lateinit var repository: PracticeHistoryRepository

    @Before
    fun setUp() {
        fakeDao = FakePracticeDao()
        repository = PracticeHistoryRepositoryImpl(
            practiceDao = fakeDao,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun testInsertingNewAttemptViaRecordOrUpdateWhenNoPreviousAttemptExists() = runTest(testDispatcher) {
        val result = repository.recordOrUpdateAttempt("POL00001", "A", true, 1000L)
        assertTrue(result.isSuccess)

        val attempt = repository.getAttempt("POL00001")
        assertNotNull(attempt)
        assertEquals("POL00001", attempt?.questionId)
        assertEquals("A", attempt?.selectedAnswer)
        assertTrue(attempt?.isCorrect == true)
        assertEquals(1000L, attempt?.attemptedAt)
        assertEquals(1, repository.getAttemptCount())
    }

    @Test
    fun testUpdatingExistingCorrectAttemptToIncorrect() = runTest(testDispatcher) {
        // Initial attempt: correct
        repository.recordOrUpdateAttempt("POL00001", "A", true, 1000L)
        assertTrue(repository.getAttempt("POL00001")?.isCorrect == true)

        // Re-answer: incorrect
        val updateResult = repository.recordOrUpdateAttempt("POL00001", "C", false, 2000L)
        assertTrue(updateResult.isSuccess)

        val updated = repository.getAttempt("POL00001")
        assertNotNull(updated)
        assertEquals("POL00001", updated?.questionId)
        assertEquals("C", updated?.selectedAnswer)
        assertFalse(updated?.isCorrect == true)
        assertEquals(2000L, updated?.attemptedAt)
        // Exactly one database record exists
        assertEquals(1, repository.getAttemptCount())
    }

    @Test
    fun testUpdatingExistingIncorrectAttemptToCorrect() = runTest(testDispatcher) {
        // Initial attempt: incorrect
        repository.recordOrUpdateAttempt("POL00001", "B", false, 1000L)
        assertFalse(repository.getAttempt("POL00001")?.isCorrect == true)

        // Re-answer: correct
        val updateResult = repository.recordOrUpdateAttempt("POL00001", "A", true, 2500L)
        assertTrue(updateResult.isSuccess)

        val updated = repository.getAttempt("POL00001")
        assertNotNull(updated)
        assertEquals("A", updated?.selectedAnswer)
        assertTrue(updated?.isCorrect == true)
        assertEquals(2500L, updated?.attemptedAt)
        assertEquals(1, repository.getAttemptCount())
    }

    @Test
    fun testNormalPracticeRecordAttemptStillRejectsDuplicateEntries() = runTest(testDispatcher) {
        // First normal attempt succeeds
        val r1 = repository.recordAttempt("POL00001", "A", true)
        assertTrue(r1.isSuccess)

        // Second normal attempt using standard recordAttempt fails with exception (ABORT behavior)
        val r2 = repository.recordAttempt("POL00001", "B", false)
        assertTrue(r2.isFailure)
        assertEquals(1, repository.getAttemptCount())
        assertEquals("A", repository.getAttempt("POL00001")?.selectedAnswer)
    }

    // ==========================================
    // Fake DAO
    // ==========================================

    private class FakePracticeDao : PracticeDao {
        val attempts = mutableMapOf<String, AttemptedQuestionEntity>()

        override suspend fun insertAttempt(attempt: AttemptedQuestionEntity) {
            if (attempts.containsKey(attempt.questionId)) {
                throw android.database.sqlite.SQLiteConstraintException("UNIQUE constraint failed: question_id")
            }
            attempts[attempt.questionId] = attempt
        }

        override suspend fun upsertAttempt(attempt: AttemptedQuestionEntity) {
            // Upsert replaces the existing record or inserts a new one
            attempts[attempt.questionId] = attempt
        }

        override suspend fun isQuestionAttempted(questionId: String): Boolean = attempts.containsKey(questionId)

        override suspend fun getAllAttemptedQuestionIds(): List<String> = attempts.keys.toList()

        override fun observeAllAttemptedQuestionIds(): Flow<List<String>> = flowOf(attempts.keys.toList())

        override suspend fun getAttempt(questionId: String): AttemptedQuestionEntity? = attempts[questionId]

        override suspend fun getAttemptCount(): Int = attempts.size

        override fun observeAttemptCount(): Flow<Int> = flowOf(attempts.size)

        override suspend fun getAllAttempts(): List<AttemptedQuestionEntity> = attempts.values.toList()

        override fun observeAllAttempts(): Flow<List<AttemptedQuestionEntity>> = flowOf(attempts.values.toList())
    }
}
