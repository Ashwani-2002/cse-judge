package com.csejudge.app.data.repository

import com.csejudge.app.data.model.AnalysisRecord
import com.csejudge.app.data.model.FullAnalysisData

/**
 * Validation engine ensuring strict mathematical consistency across all
 * analysis levels according to Stage 10 invariants.
 */
object AnalysisValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>
    )

    /**
     * Validates domain invariants for an individual [AnalysisRecord]:
     * - Attempted = Correct + Incorrect
     * - Total = Attempted + Unattempted
     * - Non-negative counts
     * - Null accuracy when attempted == 0
     * - Correct accuracy bounds [0.0, 100.0]
     */
    fun validateRecord(recordName: String, record: AnalysisRecord): List<String> {
        val errors = mutableListOf<String>()

        if (record.totalQuestions < 0) {
            errors.add("$recordName: totalQuestions (${record.totalQuestions}) cannot be negative")
        }
        if (record.attemptedQuestions < 0) {
            errors.add("$recordName: attemptedQuestions (${record.attemptedQuestions}) cannot be negative")
        }
        if (record.unattemptedQuestions < 0) {
            errors.add("$recordName: unattemptedQuestions (${record.unattemptedQuestions}) cannot be negative")
        }
        if (record.correctAnswers < 0) {
            errors.add("$recordName: correctAnswers (${record.correctAnswers}) cannot be negative")
        }
        if (record.incorrectAnswers < 0) {
            errors.add("$recordName: incorrectAnswers (${record.incorrectAnswers}) cannot be negative")
        }

        // Invariant: Attempted = Correct + Incorrect
        if (record.attemptedQuestions != record.correctAnswers + record.incorrectAnswers) {
            errors.add("$recordName: attempted (${record.attemptedQuestions}) != correct (${record.correctAnswers}) + incorrect (${record.incorrectAnswers})")
        }

        // Invariant: Total = Attempted + Unattempted
        if (record.totalQuestions != record.attemptedQuestions + record.unattemptedQuestions) {
            errors.add("$recordName: total (${record.totalQuestions}) != attempted (${record.attemptedQuestions}) + unattempted (${record.unattemptedQuestions})")
        }

        // Coverage bounds
        if (record.coveragePercentage < 0.0 || record.coveragePercentage > 100.0) {
            errors.add("$recordName: coveragePercentage (${record.coveragePercentage}) out of bounds [0, 100]")
        }

        // Accuracy invariants
        if (record.attemptedQuestions == 0) {
            if (record.accuracyPercentage != null) {
                errors.add("$recordName: accuracyPercentage must be null (N/A) when attemptedQuestions == 0, but was ${record.accuracyPercentage}")
            }
        } else {
            val accuracy = record.accuracyPercentage
            if (accuracy == null) {
                errors.add("$recordName: accuracyPercentage must not be null when attemptedQuestions > 0")
            } else if (accuracy < 0.0 || accuracy > 100.0) {
                errors.add("$recordName: accuracyPercentage ($accuracy) out of bounds [0, 100]")
            }
        }

        return errors
    }

    /**
     * Validates full hierarchical consistency:
     * 1. Individual records.
     * 2. Subject sum vs Overall sum (total, attempted, correct, incorrect).
     * 3. Microtheme sum vs Subject sum (total, attempted, correct, incorrect).
     */
    fun validateFullAnalysis(data: FullAnalysisData): ValidationResult {
        val errors = mutableListOf<String>()

        // 1. Validate overall record
        errors.addAll(validateRecord("Overall", data.overall))

        // 2. Validate each subject record
        for (subject in data.subjects) {
            errors.addAll(validateRecord("Subject '${subject.subject}'", subject))
        }

        // 3. Validate each microtheme record
        for ((subject, microthemes) in data.microthemesBySubject) {
            for (microtheme in microthemes) {
                errors.addAll(validateRecord("Microtheme '${microtheme.microtheme}' in '$subject'", microtheme))
            }
        }

        // 4. Sum of subject totals equals overall total
        val sumSubjectTotal = data.subjects.sumOf { it.totalQuestions }
        if (sumSubjectTotal != data.overall.totalQuestions) {
            errors.add("Sum of subject totals ($sumSubjectTotal) != overall total (${data.overall.totalQuestions})")
        }

        // 5. Sum of subject attempted counts equals overall attempted count
        val sumSubjectAttempted = data.subjects.sumOf { it.attemptedQuestions }
        if (sumSubjectAttempted != data.overall.attemptedQuestions) {
            errors.add("Sum of subject attempted ($sumSubjectAttempted) != overall attempted (${data.overall.attemptedQuestions})")
        }

        // 6. Sum of subject correct counts equals overall correct count
        val sumSubjectCorrect = data.subjects.sumOf { it.correctAnswers }
        if (sumSubjectCorrect != data.overall.correctAnswers) {
            errors.add("Sum of subject correct ($sumSubjectCorrect) != overall correct (${data.overall.correctAnswers})")
        }

        // 7. Sum of subject incorrect counts equals overall incorrect count
        val sumSubjectIncorrect = data.subjects.sumOf { it.incorrectAnswers }
        if (sumSubjectIncorrect != data.overall.incorrectAnswers) {
            errors.add("Sum of subject incorrect ($sumSubjectIncorrect) != overall incorrect (${data.overall.incorrectAnswers})")
        }

        // 8. For each subject: microtheme sums equal subject totals
        for (subject in data.subjects) {
            val microthemes = data.microthemesBySubject[subject.subject].orEmpty()

            val sumMthemeTotal = microthemes.sumOf { it.totalQuestions }
            if (sumMthemeTotal != subject.totalQuestions) {
                errors.add("Subject '${subject.subject}': sum of microtheme totals ($sumMthemeTotal) != subject total (${subject.totalQuestions})")
            }

            val sumMthemeAttempted = microthemes.sumOf { it.attemptedQuestions }
            if (sumMthemeAttempted != subject.attemptedQuestions) {
                errors.add("Subject '${subject.subject}': sum of microtheme attempted ($sumMthemeAttempted) != subject attempted (${subject.attemptedQuestions})")
            }

            val sumMthemeCorrect = microthemes.sumOf { it.correctAnswers }
            if (sumMthemeCorrect != subject.correctAnswers) {
                errors.add("Subject '${subject.subject}': sum of microtheme correct ($sumMthemeCorrect) != subject correct (${subject.correctAnswers})")
            }

            val sumMthemeIncorrect = microthemes.sumOf { it.incorrectAnswers }
            if (sumMthemeIncorrect != subject.incorrectAnswers) {
                errors.add("Subject '${subject.subject}': sum of microtheme incorrect ($sumMthemeIncorrect) != subject incorrect (${subject.incorrectAnswers})")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}
