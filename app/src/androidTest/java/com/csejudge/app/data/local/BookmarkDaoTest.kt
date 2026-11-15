package com.csejudge.app.data.local

import android.content.Context
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BookmarkDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var practiceDao: PracticeDao
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        bookmarkDao = database.bookmarkDao()
        practiceDao = database.practiceDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAddAndRemoveBookmark() = runBlocking {
        assertFalse(bookmarkDao.isBookmarked("POL00001"))
        assertEquals(0, bookmarkDao.getBookmarkCount())

        // Add bookmark
        bookmarkDao.insertBookmark(BookmarkedQuestionEntity("POL00001", 1000L))
        assertTrue(bookmarkDao.isBookmarked("POL00001"))
        assertEquals(1, bookmarkDao.getBookmarkCount())

        // Remove bookmark
        val deletedRows = bookmarkDao.deleteBookmark("POL00001")
        assertEquals(1, deletedRows)
        assertFalse(bookmarkDao.isBookmarked("POL00001"))
        assertEquals(0, bookmarkDao.getBookmarkCount())
    }

    @Test
    fun testAddSameBookmarkTwiceDoesNotCreateDuplicates() = runBlocking {
        bookmarkDao.insertBookmark(BookmarkedQuestionEntity("POL00001", 1000L))
        bookmarkDao.insertBookmark(BookmarkedQuestionEntity("POL00001", 2000L))

        assertEquals(1, bookmarkDao.getBookmarkCount())
        assertEquals(listOf("POL00001"), bookmarkDao.getAllBookmarkedQuestionIds())
    }

    @Test
    fun testRetrieveAllBookmarksOrderedNewestFirst() = runBlocking {
        bookmarkDao.insertBookmark(BookmarkedQuestionEntity("POL00001", 1000L))
        bookmarkDao.insertBookmark(BookmarkedQuestionEntity("ECO00001", 3000L))
        bookmarkDao.insertBookmark(BookmarkedQuestionEntity("GEO00001", 2000L))

        val ids = bookmarkDao.getAllBookmarkedQuestionIds()
        assertEquals(listOf("ECO00001", "GEO00001", "POL00001"), ids)

        val entities = bookmarkDao.getAllBookmarks()
        assertEquals("ECO00001", entities[0].questionId)
        assertEquals(3000L, entities[0].bookmarkedAt)
        assertEquals("GEO00001", entities[1].questionId)
        assertEquals(2000L, entities[1].bookmarkedAt)
        assertEquals("POL00001", entities[2].questionId)
        assertEquals(1000L, entities[2].bookmarkedAt)
    }

    @Test
    fun testBookmarkingUnattemptedAndAttemptedQuestions() = runBlocking {
        // Record an attempt for POL00001
        practiceDao.insertAttempt(
            AttemptedQuestionEntity("POL00001", "A", true, 500L)
        )
        // POL00002 has NO attempt record (unattempted)

        // Bookmark unattempted question
        bookmarkDao.insertBookmark(BookmarkedQuestionEntity("POL00002", 1000L))
        assertTrue(bookmarkDao.isBookmarked("POL00002"))
        assertFalse(practiceDao.isQuestionAttempted("POL00002"))

        // Bookmark attempted question
        bookmarkDao.insertBookmark(BookmarkedQuestionEntity("POL00001", 2000L))
        assertTrue(bookmarkDao.isBookmarked("POL00001"))
        assertTrue(practiceDao.isQuestionAttempted("POL00001"))
    }

    @Test
    fun testBookmarkPersistenceAfterDatabaseReopening() = runBlocking {
        val dbFile = File(context.filesDir, "test_bookmark_persistence.db")
        if (dbFile.exists()) dbFile.delete()

        // 1. Open persistent DB, insert bookmark, and close
        var persistentDb = Room.databaseBuilder(context, AppDatabase::class.java, "test_bookmark_persistence.db")
            .allowMainThreadQueries()
            .build()
        persistentDb.bookmarkDao().insertBookmark(
            BookmarkedQuestionEntity("POL00005", 99999L)
        )
        persistentDb.close()

        // 2. Reopen DB from same file and verify bookmark is intact
        persistentDb = Room.databaseBuilder(context, AppDatabase::class.java, "test_bookmark_persistence.db")
            .allowMainThreadQueries()
            .build()
        assertTrue(persistentDb.bookmarkDao().isBookmarked("POL00005"))
        assertEquals(listOf("POL00005"), persistentDb.bookmarkDao().getAllBookmarkedQuestionIds())
        persistentDb.close()

        // Cleanup
        dbFile.delete()
        Unit
    }

    @Test
    fun testRemovingBookmarkDoesNotDeleteOrModifyAttemptHistory() = runBlocking {
        // Create attempt for POL00001
        practiceDao.insertAttempt(
            AttemptedQuestionEntity("POL00001", "B", true, 1000L)
        )
        // Create bookmark for POL00001
        bookmarkDao.insertBookmark(BookmarkedQuestionEntity("POL00001", 2000L))

        assertEquals(1, practiceDao.getAttemptCount())
        assertEquals(1, bookmarkDao.getBookmarkCount())

        // Delete bookmark
        bookmarkDao.deleteBookmark("POL00001")
        assertEquals(0, bookmarkDao.getBookmarkCount())
        assertFalse(bookmarkDao.isBookmarked("POL00001"))

        // Attempt history must be completely untouched
        assertEquals(1, practiceDao.getAttemptCount())
        val attempt = practiceDao.getAttempt("POL00001")
        assertNotNull(attempt)
        assertEquals("POL00001", attempt?.questionId)
        assertEquals("B", attempt?.selectedAnswer)
        assertTrue(attempt?.isCorrect == true)
        assertEquals(1000L, attempt?.attemptedAt)
    }

    @Test
    fun testRapidRepeatedBookmarkOperationsDoNotCreateDuplicates() = runBlocking {
        for (i in 1..20) {
            bookmarkDao.insertBookmark(BookmarkedQuestionEntity("POL00001", 1000L + i))
        }
        assertEquals(1, bookmarkDao.getBookmarkCount())
        assertEquals(listOf("POL00001"), bookmarkDao.getAllBookmarkedQuestionIds())
    }
}
