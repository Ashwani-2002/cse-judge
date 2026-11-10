package com.csejudge.app.data

import android.database.sqlite.SQLiteConstraintException
import com.csejudge.app.data.local.AttemptedQuestionEntity
import com.csejudge.app.data.local.PracticeDao
import com.csejudge.app.data.repository.PracticeHistoryRepository
import com.csejudge.app.data.repository.PracticeHistoryRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PracticeHistoryRepositoryTest {

    private lateinit var fakeDao: FakePracticeDao
    private lateinit var repository: PracticeHistoryRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        fakeDao = FakePracticeDao()
        repository = PracticeHistoryRepositoryImpl(
            practiceDao = fakeDao,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun testRecordAttemptSuccess() = runTest(testDispatcher) {
        val result = repository.recordAttempt(
            questionId = "POL00001",
            selectedAnswer = "C",
            isCorrect = true
        )
        assertTrue(result.isSuccess)
        assertTrue(repository.isQuestionAttempted("POL00001"))
        assertEquals(1, repository.getAttemptCount())

        val attempt = repository.getAttempt("POL00001")
        assertNotNull(attempt)
        assertEquals("POL00001", attempt?.questionId)
        assertEquals("C", attempt?.selectedAnswer)
        assertTrue(attempt?.isCorrect == true)
    }

    @Test
    fun testDuplicateAttemptFailsAndDoesNotOverwrite() = runTest(testDispatcher) {
        val firstResult = repository.recordAttempt("ECO00001", "A", false)
        assertTrue(firstResult.isSuccess)

        // Attempting to record duplicate should fail
        val duplicateResult = repository.recordAttempt("ECO00001", "B", true)
        assertTrue(duplicateResult.isFailure)
        assertTrue(duplicateResult.exceptionOrNull() is SQLiteConstraintException)

        // Verify the original record remains unchanged
        val attempt = repository.getAttempt("ECO00001")
        assertEquals("A", attempt?.selectedAnswer)
        assertFalse(attempt?.isCorrect == true)
    }

    @Test
    fun testGetAllAttemptedQuestionIds() = runTest(testDispatcher) {
        repository.recordAttempt("Q1", "A", true)
        repository.recordAttempt("Q2", "B", false)
        repository.recordAttempt("Q3", "C", true)

        val ids = repository.getAllAttemptedQuestionIds()
        assertEquals(listOf("Q1", "Q2", "Q3"), ids)

        val observedIds = repository.observeAllAttemptedQuestionIds().first()
        assertEquals(listOf("Q1", "Q2", "Q3"), observedIds)
    }

    @Test
    fun testObserveAttemptCount() = runTest(testDispatcher) {
        assertEquals(0, repository.observeAttemptCount().first())

        repository.recordAttempt("Q1", "A", true)
        assertEquals(1, repository.getAttemptCount())
        assertEquals(1, repository.observeAttemptCount().first())
    }

    @Test
    fun testNonExistentAttemptReturnsNull() = runTest(testDispatcher) {
        assertFalse(repository.isQuestionAttempted("NON_EXISTENT"))
        assertNull(repository.getAttempt("NON_EXISTENT"))
    }

    private class FakePracticeDao : PracticeDao {
        private val attempts = mutableMapOf<String, AttemptedQuestionEntity>()

        override suspend fun insertAttempt(attempt: AttemptedQuestionEntity) {
            if (attempts.containsKey(attempt.questionId)) {
                throw SQLiteConstraintException("UNIQUE constraint failed: attempted_questions.question_id")
            }
            attempts[attempt.questionId] = attempt
        }

        override suspend fun isQuestionAttempted(questionId: String): Boolean {
            return attempts.containsKey(questionId)
        }

        override suspend fun getAllAttemptedQuestionIds(): List<String> {
            return attempts.keys.toList()
        }

        override fun observeAllAttemptedQuestionIds(): Flow<List<String>> {
            return flowOf(attempts.keys.toList())
        }

        override suspend fun getAttempt(questionId: String): AttemptedQuestionEntity? {
            return attempts[questionId]
        }

        override suspend fun getAttemptCount(): Int {
            return attempts.size
        }

        override fun observeAttemptCount(): Flow<Int> {
            return flowOf(attempts.size)
        }
    }
}
