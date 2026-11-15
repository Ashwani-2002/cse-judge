package com.csejudge.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val dbName = "migration_test.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val dbFile = context.getDatabasePath(dbName)
        if (dbFile.exists()) {
            context.deleteDatabase(dbName)
        }
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun testMigrationFromVersion1ToVersion2PreservesAttemptHistoryAndEnablesBookmarks() = runBlocking {
        // Step 1: Create a Version 1 database manually with Version 1 schema and user_version = 1
        val dbPath = context.getDatabasePath(dbName)
        dbPath.parentFile?.mkdirs()

        val v1Sqlite = SQLiteDatabase.openOrCreateDatabase(dbPath, null)
        v1Sqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `attempted_questions` (
                `question_id` TEXT NOT NULL,
                `selected_answer` TEXT NOT NULL,
                `is_correct` INTEGER NOT NULL,
                `attempted_at` INTEGER NOT NULL,
                PRIMARY KEY(`question_id`)
            )
            """.trimIndent()
        )

        // Insert pre-existing user attempt history in Version 1
        v1Sqlite.execSQL(
            "INSERT INTO attempted_questions (question_id, selected_answer, is_correct, attempted_at) VALUES ('POL00001', 'A', 1, 1000)"
        )
        v1Sqlite.execSQL(
            "INSERT INTO attempted_questions (question_id, selected_answer, is_correct, attempted_at) VALUES ('GEO00056', 'D', 0, 2000)"
        )

        // Set SQLite user_version to 1
        v1Sqlite.version = 1
        v1Sqlite.close()

        // Step 2: Open the database using Room version 3 with MIGRATION_1_2 and MIGRATION_2_3
        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        // Step 3: Verify existing attempt records survived the migration completely intact
        val practiceDao = migratedDb.practiceDao()
        assertEquals(2, practiceDao.getAttemptCount())

        val polAttempt = practiceDao.getAttempt("POL00001")
        assertNotNull(polAttempt)
        assertEquals("POL00001", polAttempt?.questionId)
        assertEquals("A", polAttempt?.selectedAnswer)
        assertTrue(polAttempt?.isCorrect == true)
        assertEquals(1000L, polAttempt?.attemptedAt)

        val geoAttempt = practiceDao.getAttempt("GEO00056")
        assertNotNull(geoAttempt)
        assertEquals("GEO00056", geoAttempt?.questionId)
        assertEquals("D", geoAttempt?.selectedAnswer)
        assertFalse(geoAttempt?.isCorrect == true)
        assertEquals(2000L, geoAttempt?.attemptedAt)

        // Step 4: Verify the new bookmarked_questions table is available and operational
        val bookmarkDao = migratedDb.bookmarkDao()
        assertEquals(0, bookmarkDao.getBookmarkCount())
        assertFalse(bookmarkDao.isBookmarked("POL00001"))

        // Add a bookmark into the newly migrated table
        bookmarkDao.insertBookmark(BookmarkedQuestionEntity("POL00001", 3000L))
        bookmarkDao.insertBookmark(BookmarkedQuestionEntity("AMAC00001", 4000L))

        assertEquals(2, bookmarkDao.getBookmarkCount())
        assertTrue(bookmarkDao.isBookmarked("POL00001"))
        assertTrue(bookmarkDao.isBookmarked("AMAC00001"))
        assertEquals(listOf("AMAC00001", "POL00001"), bookmarkDao.getAllBookmarkedQuestionIds())

        // Verify attempt history remains untouched after new bookmark operations on migrated DB
        assertEquals(2, practiceDao.getAttemptCount())

        migratedDb.close()
    }

    @Test
    fun testMigrationFromVersion2ToVersion3PreservesExistingDataAndEnablesTests() = runBlocking {
        // Step 1: Create a Version 2 database manually with Version 2 schema (attempted_questions + bookmarked_questions)
        val dbPath = context.getDatabasePath(dbName)
        dbPath.parentFile?.mkdirs()

        val v2Sqlite = SQLiteDatabase.openOrCreateDatabase(dbPath, null)
        v2Sqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `attempted_questions` (
                `question_id` TEXT NOT NULL,
                `selected_answer` TEXT NOT NULL,
                `is_correct` INTEGER NOT NULL,
                `attempted_at` INTEGER NOT NULL,
                PRIMARY KEY(`question_id`)
            )
            """.trimIndent()
        )
        v2Sqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bookmarked_questions` (
                `question_id` TEXT NOT NULL,
                `bookmarked_at` INTEGER NOT NULL,
                PRIMARY KEY(`question_id`)
            )
            """.trimIndent()
        )

        // Seed Version 2 user data
        v2Sqlite.execSQL("INSERT INTO attempted_questions (question_id, selected_answer, is_correct, attempted_at) VALUES ('POL00010', 'B', 1, 5000)")
        v2Sqlite.execSQL("INSERT INTO bookmarked_questions (question_id, bookmarked_at) VALUES ('POL00010', 5500)")

        v2Sqlite.version = 2
        v2Sqlite.close()

        // Step 2: Open with Room Version 3 and MIGRATION_2_3
        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        // Step 3: Verify existing attempt and bookmark records are 100% preserved
        val practiceDao = migratedDb.practiceDao()
        val bookmarkDao = migratedDb.bookmarkDao()
        val testDao = migratedDb.testDao()

        assertEquals(1, practiceDao.getAttemptCount())
        assertEquals("B", practiceDao.getAttempt("POL00010")?.selectedAnswer)
        assertEquals(1, bookmarkDao.getBookmarkCount())
        assertTrue(bookmarkDao.isBookmarked("POL00010"))

        // Step 4: Verify new test tables are operational
        assertEquals(0, testDao.getCompletedTestCount())

        val testEntity = TestEntity(
            testId = "test_uuid_1",
            testType = "FULL_LENGTH",
            startedAt = 6000L,
            completedAt = 7000L,
            durationSeconds = 3600,
            totalQuestions = 100,
            attemptedCount = 80,
            correctCount = 60,
            incorrectCount = 20,
            unansweredCount = 20,
            score = 106.67,
            maxScore = 200.0,
            accuracy = 75.0,
            status = "COMPLETED"
        )
        testDao.insertTest(testEntity)

        assertEquals(1, testDao.getCompletedTestCount())
        val retrieved = testDao.getTestById("test_uuid_1")
        assertNotNull(retrieved)
        assertEquals("FULL_LENGTH", retrieved?.testType)
        assertEquals(106.67, retrieved?.score ?: 0.0, 0.01)

        migratedDb.close()
    }
}
