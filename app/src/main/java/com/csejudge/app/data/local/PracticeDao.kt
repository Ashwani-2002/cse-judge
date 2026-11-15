package com.csejudge.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeDao {

    /**
     * Inserts a new question attempt record.
     * Uses ABORT strategy to ensure that an existing attempt is never overwritten silently
     * during normal practice sessions.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAttempt(attempt: AttemptedQuestionEntity)

    /**
     * Inserts or updates an attempt record (upsert).
     * Replaces previous attempt results for re-answering bookmarked questions in Stage 8,
     * preserving the primary key and preventing duplicate records.
     */
    @Upsert
    suspend fun upsertAttempt(attempt: AttemptedQuestionEntity)

    /**
     * Checks if a question has been attempted previously.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM attempted_questions WHERE question_id = :questionId)")
    suspend fun isQuestionAttempted(questionId: String): Boolean

    /**
     * Retrieves all attempted question IDs as a one-shot query.
     */
    @Query("SELECT question_id FROM attempted_questions")
    suspend fun getAllAttemptedQuestionIds(): List<String>

    /**
     * Observes all attempted question IDs reactively.
     */
    @Query("SELECT question_id FROM attempted_questions")
    fun observeAllAttemptedQuestionIds(): Flow<List<String>>

    /**
     * Retrieves the attempt details for a specific question ID.
     */
    @Query("SELECT * FROM attempted_questions WHERE question_id = :questionId LIMIT 1")
    suspend fun getAttempt(questionId: String): AttemptedQuestionEntity?

    /**
     * Retrieves the total count of attempted questions.
     */
    @Query("SELECT COUNT(*) FROM attempted_questions")
    suspend fun getAttemptCount(): Int

    /**
     * Observes the total count of attempted questions reactively.
     */
    @Query("SELECT COUNT(*) FROM attempted_questions")
    fun observeAttemptCount(): Flow<Int>

    /**
     * Retrieves all attempt records as a one-shot query.
     */
    @Query("SELECT * FROM attempted_questions")
    suspend fun getAllAttempts(): List<AttemptedQuestionEntity>

    /**
     * Observes all attempt records reactively.
     */
    @Query("SELECT * FROM attempted_questions")
    fun observeAllAttempts(): Flow<List<AttemptedQuestionEntity>>
}
