package com.csejudge.app.data.model

/**
 * Common contract for any level of performance and coverage analysis.
 *
 * Distinguishes strictly between question coverage (attempted / total)
 * and answer accuracy (correct / attempted).
 */
interface AnalysisRecord {
    val totalQuestions: Int
    val attemptedQuestions: Int
    val unattemptedQuestions: Int
    val coveragePercentage: Double
    val correctAnswers: Int
    val incorrectAnswers: Int
    val accuracyPercentage: Double?
}

/**
 * Pure calculation helper functions for coverage and accuracy metrics.
 */
object AnalysisCalculations {

    /**
     * Calculates question coverage percentage:
     * Coverage % = (Attempted Questions / Total Questions) * 100
     * Returns 0.0 when [total] is 0.
     */
    fun calculateCoveragePercentage(attempted: Int, total: Int): Double {
        return if (total > 0) (attempted.toDouble() / total.toDouble()) * 100.0 else 0.0
    }

    /**
     * Calculates answer accuracy percentage:
     * Accuracy % = (Correct Answers / Attempted Questions) * 100
     * Returns null (N/A) when [attempted] is 0, never 0.0%.
     */
    fun calculateAccuracyPercentage(correct: Int, attempted: Int): Double? {
        return if (attempted > 0) (correct.toDouble() / attempted.toDouble()) * 100.0 else null
    }
}

/**
 * Level 1: Overall question coverage and accuracy across all subjects.
 */
data class OverallAnalysis(
    override val totalQuestions: Int,
    override val attemptedQuestions: Int,
    override val unattemptedQuestions: Int,
    override val coveragePercentage: Double,
    override val correctAnswers: Int,
    override val incorrectAnswers: Int,
    override val accuracyPercentage: Double?
) : AnalysisRecord {

    companion object {
        fun create(
            totalQuestions: Int,
            correctAnswers: Int,
            incorrectAnswers: Int
        ): OverallAnalysis {
            val attempted = correctAnswers + incorrectAnswers
            val unattempted = (totalQuestions - attempted).coerceAtLeast(0)
            return OverallAnalysis(
                totalQuestions = totalQuestions,
                attemptedQuestions = attempted,
                unattemptedQuestions = unattempted,
                coveragePercentage = AnalysisCalculations.calculateCoveragePercentage(attempted, totalQuestions),
                correctAnswers = correctAnswers,
                incorrectAnswers = incorrectAnswers,
                accuracyPercentage = AnalysisCalculations.calculateAccuracyPercentage(correctAnswers, attempted)
            )
        }
    }
}

/**
 * Level 2: Subject-wise question coverage and accuracy.
 */
data class SubjectAnalysis(
    val subject: String,
    override val totalQuestions: Int,
    override val attemptedQuestions: Int,
    override val unattemptedQuestions: Int,
    override val coveragePercentage: Double,
    override val correctAnswers: Int,
    override val incorrectAnswers: Int,
    override val accuracyPercentage: Double?
) : AnalysisRecord {

    companion object {
        fun create(
            subject: String,
            totalQuestions: Int,
            correctAnswers: Int,
            incorrectAnswers: Int
        ): SubjectAnalysis {
            val attempted = correctAnswers + incorrectAnswers
            val unattempted = (totalQuestions - attempted).coerceAtLeast(0)
            return SubjectAnalysis(
                subject = subject,
                totalQuestions = totalQuestions,
                attemptedQuestions = attempted,
                unattemptedQuestions = unattempted,
                coveragePercentage = AnalysisCalculations.calculateCoveragePercentage(attempted, totalQuestions),
                correctAnswers = correctAnswers,
                incorrectAnswers = incorrectAnswers,
                accuracyPercentage = AnalysisCalculations.calculateAccuracyPercentage(correctAnswers, attempted)
            )
        }
    }
}

/**
 * Level 3: Microtheme-wise question coverage and accuracy within a subject.
 */
data class MicrothemeAnalysis(
    val subject: String,
    val microtheme: String,
    override val totalQuestions: Int,
    override val attemptedQuestions: Int,
    override val unattemptedQuestions: Int,
    override val coveragePercentage: Double,
    override val correctAnswers: Int,
    override val incorrectAnswers: Int,
    override val accuracyPercentage: Double?
) : AnalysisRecord {

    companion object {
        fun create(
            subject: String,
            microtheme: String,
            totalQuestions: Int,
            correctAnswers: Int,
            incorrectAnswers: Int
        ): MicrothemeAnalysis {
            val attempted = correctAnswers + incorrectAnswers
            val unattempted = (totalQuestions - attempted).coerceAtLeast(0)
            return MicrothemeAnalysis(
                subject = subject,
                microtheme = microtheme,
                totalQuestions = totalQuestions,
                attemptedQuestions = attempted,
                unattemptedQuestions = unattempted,
                coveragePercentage = AnalysisCalculations.calculateCoveragePercentage(attempted, totalQuestions),
                correctAnswers = correctAnswers,
                incorrectAnswers = incorrectAnswers,
                accuracyPercentage = AnalysisCalculations.calculateAccuracyPercentage(correctAnswers, attempted)
            )
        }
    }
}

/**
 * Complete immutable snapshot of analysis across all three hierarchy levels.
 */
data class FullAnalysisData(
    val overall: OverallAnalysis,
    val subjects: List<SubjectAnalysis>,
    val microthemesBySubject: Map<String, List<MicrothemeAnalysis>>,
    val unmatchedAttemptIds: List<String> = emptyList()
)
