package com.csejudge.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a completed test session (Full Length or Half Length).
 * Persisted in local Room database, independent of static question-bank updates.
 */
@Entity(tableName = "tests")
data class TestEntity(
    @PrimaryKey
    @ColumnInfo(name = "test_id")
    val testId: String,

    @ColumnInfo(name = "test_type")
    val testType: String, // "FULL_LENGTH" or "HALF_LENGTH"

    @ColumnInfo(name = "started_at")
    val startedAt: Long,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long,

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int,

    @ColumnInfo(name = "total_questions")
    val totalQuestions: Int,

    @ColumnInfo(name = "attempted_count")
    val attemptedCount: Int,

    @ColumnInfo(name = "correct_count")
    val correctCount: Int,

    @ColumnInfo(name = "incorrect_count")
    val incorrectCount: Int,

    @ColumnInfo(name = "unanswered_count")
    val unansweredCount: Int,

    @ColumnInfo(name = "score")
    val score: Double,

    @ColumnInfo(name = "max_score")
    val maxScore: Double,

    @ColumnInfo(name = "accuracy")
    val accuracy: Double, // Percentage e.g. 75.5 or -1.0 if 0 attempted

    @ColumnInfo(name = "status")
    val status: String = "COMPLETED"
)
