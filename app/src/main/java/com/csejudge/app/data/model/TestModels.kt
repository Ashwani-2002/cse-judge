package com.csejudge.app.data.model

import kotlinx.serialization.Serializable

/**
 * Types of tests supported by CSE Judge.
 */
enum class TestType(
    val title: String,
    val totalQuestions: Int,
    val durationMinutes: Int,
    val maxScore: Double,
    val durationSeconds: Int
) {
    FULL_LENGTH(
        title = "Full Length Test",
        totalQuestions = 100,
        durationMinutes = 120,
        maxScore = 200.0,
        durationSeconds = 120 * 60
    ),
    HALF_LENGTH(
        title = "Half Length Test",
        totalQuestions = 50,
        durationMinutes = 60,
        maxScore = 100.0,
        durationSeconds = 60 * 60
    );

    companion object {
        fun fromString(value: String): TestType = when (value.trim().uppercase()) {
            "FULL_LENGTH", "FLT" -> FULL_LENGTH
            "HALF_LENGTH", "HLT" -> HALF_LENGTH
            else -> FULL_LENGTH
        }
    }
}

/**
 * Subject performance metrics for the Overall tab of a test report.
 */
data class SubjectTestPerformance(
    val subject: String,
    val totalQuestions: Int,
    val attemptedQuestions: Int,
    val correctAnswers: Int,
    val incorrectAnswers: Int,
    val accuracyPercentage: Double? // null if 0 attempted
)

/**
 * Full details of a completed test question for the Question Wise tab.
 */
data class TestQuestionDetail(
    val questionOrder: Int,
    val questionId: String,
    val subject: String,
    val microtheme: String,
    val questionStatements: List<String>,
    val options: Options,
    val correctAnswer: String,
    val solutionStatements: List<String>,
    val selectedAnswer: String?,
    val isMarkedForReview: Boolean,
    val status: String // "CORRECT", "INCORRECT", "UNANSWERED"
) {
    val isCorrect: Boolean get() = status == "CORRECT"
    val isIncorrect: Boolean get() = status == "INCORRECT"
    val isUnanswered: Boolean get() = status == "UNANSWERED"
}

/**
 * Complete test report data containing both overall performance and question-wise details.
 */
data class TestReport(
    val testId: String,
    val testType: TestType,
    val startedAt: Long,
    val completedAt: Long,
    val durationSeconds: Int,
    val totalQuestions: Int,
    val attemptedCount: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val unansweredCount: Int,
    val score: Double,
    val maxScore: Double,
    val accuracyPercentage: Double?,
    val subjectPerformanceList: List<SubjectTestPerformance>,
    val questions: List<TestQuestionDetail>
)
