package com.csejudge.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTest(test: TestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestQuestions(questions: List<TestQuestionEntity>)

    @Query("SELECT * FROM tests WHERE status = 'COMPLETED' ORDER BY completed_at DESC")
    fun observeCompletedTests(): Flow<List<TestEntity>>

    @Query("SELECT * FROM tests WHERE status = 'COMPLETED' ORDER BY completed_at DESC")
    suspend fun getAllCompletedTests(): List<TestEntity>

    @Query("SELECT * FROM tests WHERE test_id = :testId LIMIT 1")
    suspend fun getTestById(testId: String): TestEntity?

    @Query("SELECT * FROM test_questions WHERE test_id = :testId ORDER BY question_order ASC")
    suspend fun getTestQuestions(testId: String): List<TestQuestionEntity>

    @Query("SELECT COUNT(*) FROM tests WHERE status = 'COMPLETED'")
    suspend fun getCompletedTestCount(): Int
}
