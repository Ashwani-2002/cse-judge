package com.csejudge.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user bookmark on a practice question.
 * Stored in local Room database, completely independent of attempt history.
 */
@Entity(tableName = "bookmarked_questions")
data class BookmarkedQuestionEntity(
    @PrimaryKey
    @ColumnInfo(name = "question_id")
    val questionId: String,

    @ColumnInfo(name = "bookmarked_at")
    val bookmarkedAt: Long = System.currentTimeMillis()
)
