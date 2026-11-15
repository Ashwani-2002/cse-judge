package com.csejudge.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Snapshot of a question presented in a specific test session.
 * Stores question statement, options, solution, selected answer, and evaluation status
 * so that historical test reports can be reconstructed faithfully even if question-bank JSON files change.
 */
@Entity(
    tableName = "test_questions",
    primaryKeys = ["test_id", "question_order"],
    foreignKeys = [
        ForeignKey(
            entity = TestEntity::class,
            parentColumns = ["test_id"],
            childColumns = ["test_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["test_id"]),
        Index(value = ["test_id", "question_id"], unique = true)
    ]
)
data class TestQuestionEntity(
    @ColumnInfo(name = "test_id")
    val testId: String,

    @ColumnInfo(name = "question_order")
    val questionOrder: Int, // 1-based index (1 to 100, or 1 to 50)

    @ColumnInfo(name = "question_id")
    val questionId: String,

    @ColumnInfo(name = "subject")
    val subject: String,

    @ColumnInfo(name = "microtheme")
    val microtheme: String,

    @ColumnInfo(name = "question_statement_json")
    val questionStatementJson: String, // Serialized List<String>

    @ColumnInfo(name = "options_json")
    val optionsJson: String, // Serialized Options

    @ColumnInfo(name = "correct_answer")
    val correctAnswer: String,

    @ColumnInfo(name = "solution_json")
    val solutionJson: String, // Serialized List<String>

    @ColumnInfo(name = "selected_answer")
    val selectedAnswer: String?, // "A", "B", "C", "D" or null if unanswered

    @ColumnInfo(name = "is_marked_for_review")
    val isMarkedForReview: Boolean,

    @ColumnInfo(name = "status")
    val status: String // "CORRECT", "INCORRECT", "UNANSWERED"
)
