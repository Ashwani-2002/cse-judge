package com.csejudge.app.data

import com.csejudge.app.data.model.FullAnalysisData
import com.csejudge.app.data.model.MicrothemeAnalysis
import com.csejudge.app.data.model.OverallAnalysis
import com.csejudge.app.data.model.SubjectAnalysis
import com.csejudge.app.data.repository.AnalysisValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisValidatorTest {

    @Test
    fun testValidRecordPassesValidation() {
        val overall = OverallAnalysis.create(
            totalQuestions = 100,
            correctAnswers = 40,
            incorrectAnswers = 20
        )
        val errors = AnalysisValidator.validateRecord("Overall", overall)
        assertTrue("Expected no errors, got: $errors", errors.isEmpty())
        assertEquals(60, overall.attemptedQuestions)
        assertEquals(40, overall.unattemptedQuestions)
        assertEquals(60.0, overall.coveragePercentage, 0.001)
        assertEquals((40.0 / 60.0) * 100.0, overall.accuracyPercentage!!, 0.001)
    }

    @Test
    fun testZeroAttemptRecordHasNullAccuracyAndZeroCoverage() {
        val overall = OverallAnalysis.create(
            totalQuestions = 50,
            correctAnswers = 0,
            incorrectAnswers = 0
        )
        val errors = AnalysisValidator.validateRecord("Overall", overall)
        assertTrue(errors.isEmpty())
        assertEquals(0, overall.attemptedQuestions)
        assertEquals(50, overall.unattemptedQuestions)
        assertEquals(0.0, overall.coveragePercentage, 0.001)
        assertEquals(null, overall.accuracyPercentage)
    }

    @Test
    fun testRecordWithCorruptedAttemptedSumFailsValidation() {
        // Manually construct record where attempted != correct + incorrect
        val corrupted = OverallAnalysis(
            totalQuestions = 100,
            attemptedQuestions = 50, // Should be 30 + 10 = 40
            unattemptedQuestions = 50,
            coveragePercentage = 50.0,
            correctAnswers = 30,
            incorrectAnswers = 10,
            accuracyPercentage = 75.0
        )
        val errors = AnalysisValidator.validateRecord("Corrupted", corrupted)
        assertFalse(errors.isEmpty())
        assertTrue(errors.any { it.contains("attempted (50) != correct (30) + incorrect (10)") })
    }

    @Test
    fun testRecordWithCorruptedTotalSumFailsValidation() {
        // Manually construct record where total != attempted + unattempted
        val corrupted = OverallAnalysis(
            totalQuestions = 100,
            attemptedQuestions = 40,
            unattemptedQuestions = 50, // 40 + 50 = 90 != 100
            coveragePercentage = 40.0,
            correctAnswers = 30,
            incorrectAnswers = 10,
            accuracyPercentage = 75.0
        )
        val errors = AnalysisValidator.validateRecord("Corrupted", corrupted)
        assertFalse(errors.isEmpty())
        assertTrue(errors.any { it.contains("total (100) != attempted (40) + unattempted (50)") })
    }

    @Test
    fun testZeroAttemptWithNonNullAccuracyFailsValidation() {
        val corrupted = OverallAnalysis(
            totalQuestions = 10,
            attemptedQuestions = 0,
            unattemptedQuestions = 10,
            coveragePercentage = 0.0,
            correctAnswers = 0,
            incorrectAnswers = 0,
            accuracyPercentage = 0.0 // Must be null (N/A)
        )
        val errors = AnalysisValidator.validateRecord("Corrupted", corrupted)
        assertFalse(errors.isEmpty())
        assertTrue(errors.any { it.contains("accuracyPercentage must be null (N/A) when attemptedQuestions == 0") })
    }

    @Test
    fun testValidFullAnalysisPassesValidation() {
        val subject1M1 = MicrothemeAnalysis.create("Subject1", "M1", 30, 10, 5)
        val subject1M2 = MicrothemeAnalysis.create("Subject1", "M2", 20, 5, 5)
        val subject1 = SubjectAnalysis.create("Subject1", 50, 15, 10)

        val subject2M1 = MicrothemeAnalysis.create("Subject2", "M1", 50, 20, 10)
        val subject2 = SubjectAnalysis.create("Subject2", 50, 20, 10)

        val overall = OverallAnalysis.create(100, 35, 20)

        val fullData = FullAnalysisData(
            overall = overall,
            subjects = listOf(subject1, subject2),
            microthemesBySubject = mapOf(
                "Subject1" to listOf(subject1M1, subject1M2),
                "Subject2" to listOf(subject2M1)
            )
        )

        val result = AnalysisValidator.validateFullAnalysis(fullData)
        assertTrue("Expected full analysis to be valid, got: ${result.errors}", result.isValid)
    }

    @Test
    fun testFullAnalysisWithSubjectTotalMismatchFailsValidation() {
        val subject1 = SubjectAnalysis.create("Subject1", 40, 10, 5) // Sum = 90
        val subject2 = SubjectAnalysis.create("Subject2", 50, 10, 5)
        val overall = OverallAnalysis.create(100, 20, 10) // Overall total = 100 != 90

        val fullData = FullAnalysisData(
            overall = overall,
            subjects = listOf(subject1, subject2),
            microthemesBySubject = mapOf(
                "Subject1" to emptyList(),
                "Subject2" to emptyList()
            )
        )

        val result = AnalysisValidator.validateFullAnalysis(fullData)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Sum of subject totals (90) != overall total (100)") })
    }

    @Test
    fun testFullAnalysisWithMicrothemeTotalMismatchFailsValidation() {
        val subject1M1 = MicrothemeAnalysis.create("Subject1", "M1", 20, 5, 5) // Mtheme total = 20 != 50
        val subject1 = SubjectAnalysis.create("Subject1", 50, 15, 10)
        val overall = OverallAnalysis.create(50, 15, 10)

        val fullData = FullAnalysisData(
            overall = overall,
            subjects = listOf(subject1),
            microthemesBySubject = mapOf(
                "Subject1" to listOf(subject1M1)
            )
        )

        val result = AnalysisValidator.validateFullAnalysis(fullData)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("sum of microtheme totals (20) != subject total (50)") })
    }
}
