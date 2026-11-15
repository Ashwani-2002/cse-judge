package com.csejudge.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PracticeDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: PracticeDao
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.practiceDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAttemptAndRetrieve() = runBlocking {
        assertFalse(dao.isQuestionAttempted("POL00001"))
        assertEquals(0, dao.getAttemptCount())

        val attempt = AttemptedQuestionEntity(
            questionId = "POL00001",
            selectedAnswer = "C",
            isCorrect = true,
            attemptedAt = 1700000000000L
        )
        dao.insertAttempt(attempt)

        assertTrue(dao.isQuestionAttempted("POL00001"))
        assertEquals(1, dao.getAttemptCount())

        val retrieved = dao.getAttempt("POL00001")
        assertNotNull(retrieved)
        assertEquals("POL00001", retrieved?.questionId)
        assertEquals("C", retrieved?.selectedAnswer)
        assertEquals(true, retrieved?.isCorrect)
        assertEquals(1700000000000L, retrieved?.attemptedAt)
    }

    @Test
    fun testStoreBothCorrectAndIncorrectAttempts() = runBlocking {
        val correctAttempt = AttemptedQuestionEntity(
            questionId = "POL00001",
            selectedAnswer = "C",
            isCorrect = true,
            attemptedAt = 1000L
        )
        val incorrectAttempt = AttemptedQuestionEntity(
            questionId = "ECO00001",
            selectedAnswer = "A",
            isCorrect = false,
            attemptedAt = 2000L
        )

        dao.insertAttempt(correctAttempt)
        dao.insertAttempt(incorrectAttempt)

        assertEquals(2, dao.getAttemptCount())
        assertEquals(true, dao.getAttempt("POL00001")?.isCorrect)
        assertEquals(false, dao.getAttempt("ECO00001")?.isCorrect)
    }

    @Test
    fun testDuplicateAttemptThrowsExceptionAndDoesNotOverwrite() = runBlocking {
        val initialAttempt = AttemptedQuestionEntity(
            questionId = "ENV00001",
            selectedAnswer = "B",
            isCorrect = true,
            attemptedAt = 5000L
        )
        dao.insertAttempt(initialAttempt)

        val duplicateAttempt = AttemptedQuestionEntity(
            questionId = "ENV00001",
            selectedAnswer = "D",
            isCorrect = false,
            attemptedAt = 9000L
        )

        try {
            dao.insertAttempt(duplicateAttempt)
            fail("Expected SQLiteConstraintException when inserting duplicate questionId")
        } catch (e: SQLiteConstraintException) {
            // Expected
        }

        // Verify the original record was NOT overwritten
        val current = dao.getAttempt("ENV00001")
        assertNotNull(current)
        assertEquals("B", current?.selectedAnswer)
        assertEquals(true, current?.isCorrect)
        assertEquals(5000L, current?.attemptedAt)
    }

    @Test
    fun testGetAllAttemptedQuestionIds() = runBlocking {
        val ids = listOf("AMAC00001", "ECO00001", "GEO00001", "MH00001", "SNT00001")
        for ((idx, id) in ids.withIndex()) {
            dao.insertAttempt(
                AttemptedQuestionEntity(
                    questionId = id,
                    selectedAnswer = "A",
                    isCorrect = true,
                    attemptedAt = idx.toLong()
                )
            )
        }

        val allAttemptedIds = dao.getAllAttemptedQuestionIds()
        assertEquals(ids.size, allAttemptedIds.size)
        assertEquals(ids.sorted(), allAttemptedIds.sorted())
    }

    @Test
    fun testNonExistentAttemptReturnsNull() = runBlocking {
        assertNull(dao.getAttempt("NON_EXISTENT_ID"))
        assertFalse(dao.isQuestionAttempted("NON_EXISTENT_ID"))
    }

    @Test
    fun testUpsertInsertsNewAttemptWhenNoneExists() = runBlocking {
        val attempt = AttemptedQuestionEntity(
            questionId = "POL00010",
            selectedAnswer = "A",
            isCorrect = true,
            attemptedAt = 1000L
        )
        dao.upsertAttempt(attempt)

        val retrieved = dao.getAttempt("POL00010")
        assertNotNull(retrieved)
        assertEquals("POL00010", retrieved?.questionId)
        assertEquals("A", retrieved?.selectedAnswer)
        assertTrue(retrieved?.isCorrect == true)
        assertEquals(1000L, retrieved?.attemptedAt)
        assertEquals(1, dao.getAttemptCount())
    }

    @Test
    fun testUpsertUpdatesExistingAttemptWithoutCreatingDuplicate() = runBlocking {
        val initial = AttemptedQuestionEntity(
            questionId = "POL00010",
            selectedAnswer = "A",
            isCorrect = true,
            attemptedAt = 1000L
        )
        dao.insertAttempt(initial)
        assertEquals(1, dao.getAttemptCount())

        // Upsert re-attempt: changed answer to C, incorrect, new timestamp
        val reattempt = AttemptedQuestionEntity(
            questionId = "POL00010",
            selectedAnswer = "C",
            isCorrect = false,
            attemptedAt = 2500L
        )
        dao.upsertAttempt(reattempt)

        // Count must still be 1 (exactly one record per questionId)
        assertEquals(1, dao.getAttemptCount())
        val updated = dao.getAttempt("POL00010")
        assertNotNull(updated)
        assertEquals("POL00010", updated?.questionId)
        assertEquals("C", updated?.selectedAnswer)
        assertFalse(updated?.isCorrect == true)
        assertEquals(2500L, updated?.attemptedAt)
    }

    @Test
    fun testDataPersistsAcrossDatabaseReopening() = runBlocking {
        val dbFile = File(context.filesDir, "test_persistence.db")
        if (dbFile.exists()) dbFile.delete()

        // 1. Open persistent DB, insert attempt, and close
        var persistentDb = Room.databaseBuilder(context, AppDatabase::class.java, "test_persistence.db")
            .allowMainThreadQueries()
            .build()
        persistentDb.practiceDao().insertAttempt(
            AttemptedQuestionEntity(
                questionId = "POL00005",
                selectedAnswer = "A",
                isCorrect = true,
                attemptedAt = 99999L
            )
        )
        persistentDb.close()

        // 2. Reopen DB from same file and verify attempt is still there
        persistentDb = Room.databaseBuilder(context, AppDatabase::class.java, "test_persistence.db")
            .allowMainThreadQueries()
            .build()
        val retrieved = persistentDb.practiceDao().getAttempt("POL00005")
        assertNotNull(retrieved)
        assertEquals("POL00005", retrieved?.questionId)
        assertEquals("A", retrieved?.selectedAnswer)
        assertEquals(true, retrieved?.isCorrect)
        assertEquals(99999L, retrieved?.attemptedAt)
        persistentDb.close()

        // Cleanup
        dbFile.delete()
        Unit
    }

    @Test
    fun testGetAllAttemptsAndObserveAllAttempts() = runBlocking {
        assertEquals(0, dao.getAllAttempts().size)

        val attempt1 = AttemptedQuestionEntity("POL00001", "A", true, 1000L)
        val attempt2 = AttemptedQuestionEntity("ECO00001", "B", false, 2000L)

        dao.insertAttempt(attempt1)
        dao.insertAttempt(attempt2)

        val allAttempts = dao.getAllAttempts()
        assertEquals(2, allAttempts.size)
        assertTrue(allAttempts.any { it.questionId == "POL00001" && it.isCorrect })
        assertTrue(allAttempts.any { it.questionId == "ECO00001" && !it.isCorrect })
    }
}
