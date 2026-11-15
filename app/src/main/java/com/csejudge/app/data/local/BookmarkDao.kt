package com.csejudge.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    /**
     * Adds a bookmark. If the bookmark already exists, replaces it with updated timestamp,
     * ensuring duplicate entries are never created for the same question ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkedQuestionEntity)

    /**
     * Removes a bookmark by question ID.
     * Deletes only from bookmarked_questions table without modifying attempt history.
     */
    @Query("DELETE FROM bookmarked_questions WHERE question_id = :questionId")
    suspend fun deleteBookmark(questionId: String): Int

    /**
     * Checks if a question is currently bookmarked.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_questions WHERE question_id = :questionId)")
    suspend fun isBookmarked(questionId: String): Boolean

    /**
     * Retrieves all bookmarked question IDs in predictable order (newest first).
     */
    @Query("SELECT question_id FROM bookmarked_questions ORDER BY bookmarked_at DESC")
    suspend fun getAllBookmarkedQuestionIds(): List<String>

    /**
     * Observes all bookmarked question IDs reactively.
     */
    @Query("SELECT question_id FROM bookmarked_questions ORDER BY bookmarked_at DESC")
    fun observeAllBookmarkedQuestionIds(): Flow<List<String>>

    /**
     * Retrieves all bookmark entity records, newest first.
     */
    @Query("SELECT * FROM bookmarked_questions ORDER BY bookmarked_at DESC")
    suspend fun getAllBookmarks(): List<BookmarkedQuestionEntity>

    /**
     * Observes all bookmark records reactively.
     */
    @Query("SELECT * FROM bookmarked_questions ORDER BY bookmarked_at DESC")
    fun observeAllBookmarks(): Flow<List<BookmarkedQuestionEntity>>

    /**
     * Retrieves total bookmark count.
     */
    @Query("SELECT COUNT(*) FROM bookmarked_questions")
    suspend fun getBookmarkCount(): Int

    /**
     * Observes total bookmark count reactively.
     */
    @Query("SELECT COUNT(*) FROM bookmarked_questions")
    fun observeBookmarkCount(): Flow<Int>
}
