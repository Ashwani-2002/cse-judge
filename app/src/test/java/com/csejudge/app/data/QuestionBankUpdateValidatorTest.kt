package com.csejudge.app.data

import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.repository.QuestionBankUpdateValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionBankUpdateValidatorTest {

    private fun createQuestion(
        id: String,
        subject: String = "Polity",
        microtheme: String = "Fundamental Rights",
        text: String = "What is Article 21?",
        a: String = "Right to Life",
        b: String = "Right to Equality",
        c: String = "Right to Freedom",
        d: String = "Right to Remedy",
        correct: String = "A",
        solution: String = "Article 21 provides protection of life and personal liberty."
    ): Question {
        return Question(
            questionId = id,
            subject = subject,
            microtheme = microtheme,
            question = listOf(text),
            options = Options(a = a, b = b, c = c, d = d),
            correctAnswer = correct,
            solution = listOf(solution)
        )
    }

    @Test
    fun testValidUpdateWithNewQuestionsAndMicrothemes() {
        val baseline = listOf(
            createQuestion("POL00001", "Polity", "Fundamental Rights"),
            createQuestion("POL00002", "Polity", "Fundamental Rights"),
            createQuestion("ECO00001", "Economy", "Banking System")
        )

        val updated = listOf(
            // Baseline preserved
            createQuestion("POL00001", "Polity", "Fundamental Rights"),
            createQuestion("POL00002", "Polity", "Fundamental Rights"),
            createQuestion("ECO00001", "Economy", "Banking System"),
            // New question in existing microtheme
            createQuestion("POL00003", "Polity", "Fundamental Rights"),
            // New microtheme in existing subject
            createQuestion("ECO00002", "Economy", "Fiscal Policy"),
            // New subject and microtheme
            createQuestion("ETH00001", "Ethics", "Human Values")
        )

        val result = QuestionBankUpdateValidator.validateUpdate(baseline, updated)

        assertTrue(result.isValidForRelease)
        assertFalse(result.hasFatalErrors)
        assertFalse(result.hasMaterialChanges)
        assertEquals(3, result.preservedQuestionsCount)
        assertEquals(3, result.newlyAddedQuestions.size)
        assertEquals(setOf("Ethics"), result.newlyAddedSubjects)
        assertEquals(setOf("Fiscal Policy"), result.newlyAddedMicrothemes["Economy"])
        assertEquals(setOf("Human Values"), result.newlyAddedMicrothemes["Ethics"])
        assertEquals(0, result.missingQuestionIds.size)
        assertEquals(0, result.duplicateQuestionIds.size)
    }

    @Test
    fun testReorderingDoesNotAffectIdentityOrValidity() {
        val q1 = createQuestion("POL00001", text = "Question One")
        val q2 = createQuestion("POL00002", text = "Question Two")
        val q3 = createQuestion("POL00003", text = "Question Three")

        val baseline = listOf(q1, q2, q3)
        // Reverse order in updated JSON
        val updated = listOf(q3, q1, q2)

        val result = QuestionBankUpdateValidator.validateUpdate(baseline, updated)

        assertTrue(result.isValidForRelease)
        assertEquals(3, result.preservedQuestionsCount)
        assertEquals(0, result.newlyAddedQuestions.size)
        assertEquals(0, result.missingQuestionIds.size)
        assertEquals(0, result.materialContentChanges.size)
    }

    @Test
    fun testMissingBaselineQuestionIdIsDetectedAsFatalError() {
        val baseline = listOf(
            createQuestion("POL00001"),
            createQuestion("POL00002"),
            createQuestion("POL00003")
        )

        // Candidate update dropped POL00002
        val updated = listOf(
            createQuestion("POL00001"),
            createQuestion("POL00003")
        )

        val result = QuestionBankUpdateValidator.validateUpdate(baseline, updated)

        assertFalse(result.isValidForRelease)
        assertTrue(result.hasFatalErrors)
        assertEquals(listOf("POL00002"), result.missingQuestionIds)
    }

    @Test
    fun testDuplicateQuestionIdsInUpdatedBankDetectedAsFatalError() {
        val baseline = listOf(createQuestion("POL00001"))

        val updated = listOf(
            createQuestion("POL00001"),
            createQuestion("POL00002"),
            createQuestion("POL00002") // duplicate ID!
        )

        val result = QuestionBankUpdateValidator.validateUpdate(baseline, updated)

        assertFalse(result.isValidForRelease)
        assertTrue(result.hasFatalErrors)
        assertEquals(listOf("POL00002"), result.duplicateQuestionIds)
    }

    @Test
    fun testMaterialChangeInWordingFlaggedForReview() {
        val baseline = listOf(createQuestion("POL00001", text = "Original wording of question"))
        val updated = listOf(createQuestion("POL00001", text = "Completely altered wording of question"))

        val result = QuestionBankUpdateValidator.validateUpdate(baseline, updated)

        assertFalse(result.isValidForRelease)
        assertTrue(result.hasMaterialChanges)
        assertEquals(1, result.materialContentChanges.size)
        assertEquals("question_text", result.materialContentChanges[0].field)
        assertEquals("POL00001", result.materialContentChanges[0].questionId)
    }

    @Test
    fun testMaterialChangeInOptionsFlaggedForReview() {
        val baseline = listOf(createQuestion("POL00001", a = "Original Option A"))
        val updated = listOf(createQuestion("POL00001", a = "Modified Option A"))

        val result = QuestionBankUpdateValidator.validateUpdate(baseline, updated)

        assertFalse(result.isValidForRelease)
        assertTrue(result.hasMaterialChanges)
        assertEquals(1, result.materialContentChanges.size)
        assertEquals("option_A", result.materialContentChanges[0].field)
    }

    @Test
    fun testMaterialChangeInCorrectAnswerFlaggedForReview() {
        val baseline = listOf(createQuestion("POL00001", correct = "A"))
        val updated = listOf(createQuestion("POL00001", correct = "B"))

        val result = QuestionBankUpdateValidator.validateUpdate(baseline, updated)

        assertFalse(result.isValidForRelease)
        assertTrue(result.hasMaterialChanges)
        assertEquals(1, result.materialContentChanges.size)
        assertEquals("correct_answer", result.materialContentChanges[0].field)
    }

    @Test
    fun testContentHashComputationIsDeterministic() {
        val q1 = createQuestion("POL00001")
        val q2 = createQuestion("POL00001")
        val qDifferent = createQuestion("POL00001", text = "Different text")

        val hash1 = QuestionBankUpdateValidator.computeContentHash(q1)
        val hash2 = QuestionBankUpdateValidator.computeContentHash(q2)
        val hashDifferent = QuestionBankUpdateValidator.computeContentHash(qDifferent)

        assertEquals(hash1, hash2)
        org.junit.Assert.assertNotEquals(hash1, hashDifferent)
    }

    @Test
    fun testValidateDirectoryOnProductionQuestionBank() {
        val questionBankDir = java.io.File("../question-bank").let {
            if (it.exists()) it else java.io.File("question-bank")
        }
        assertTrue("Question bank directory must exist", questionBankDir.exists())

        val report = QuestionBankUpdateValidator.validateDirectory(questionBankDir)

        assertTrue("Production question bank must be completely valid: ${report.schemaErrors.take(3)}", report.isValid)
        assertEquals(0, report.syntaxErrors.size)
        assertEquals(0, report.schemaErrors.size)
        assertEquals(0, report.duplicateQuestionIds.size)
        assertEquals(7, report.scannedFiles.size)
        assertEquals(1432, report.totalQuestions)
        assertEquals(7, report.subjectCounts.size)
    }

    @Test
    fun testValidateDirectoryDetectsInvalidSchemaAndDuplicates() {
        val tempDir = java.io.File.createTempFile("qb_test", "").also { it.delete(); it.mkdir() }
        try {
            val jsonText = """
                {
                    "questions": [
                        {
                            "subject": "Polity",
                            "microtheme": "Fundamental Rights",
                            "question": ["Valid question"],
                            "options": { "A": "A", "B": "B", "C": "C", "D": "D" },
                            "correct_answer": "A",
                            "solution": ["Solution"],
                            "question_id": "TEST001"
                        },
                        {
                            "subject": "Polity",
                            "microtheme": "Fundamental Rights",
                            "question": ["Duplicate ID question"],
                            "options": { "A": "A", "B": "B", "C": "C", "D": "D" },
                            "correct_answer": "A",
                            "solution": ["Solution"],
                            "question_id": "TEST001"
                        },
                        {
                            "subject": "",
                            "microtheme": "",
                            "question": [],
                            "options": { "A": "", "B": "", "C": "", "D": "" },
                            "correct_answer": "Z",
                            "solution": [],
                            "question_id": ""
                        }
                    ]
                }
            """.trimIndent()
            java.io.File(tempDir, "test.json").writeText(jsonText)

            val report = QuestionBankUpdateValidator.validateDirectory(tempDir)
            assertFalse(report.isValid)
            assertEquals(1, report.duplicateQuestionIds.size)
            assertTrue("Must report schema errors for blank/invalid question", report.schemaErrors.isNotEmpty())
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
