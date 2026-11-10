package com.csejudge.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user attempt on a practice question.
 * Stored in local Room database, completely separated from static question-bank data.
 */
@Entity(tableName = "attempted_questions")
data class AttemptedQuestionEntity(
    @PrimaryKey
    @ColumnInfo(name = "question_id")
    val questionId: String,

    @ColumnInfo(name = "selected_answer")
    val selectedAnswer: String,

    @ColumnInfo(name = "is_correct")
    val isCorrect: Boolean,

    @ColumnInfo(name = "attempted_at")
    val attemptedAt: Long = System.currentTimeMillis()
)
