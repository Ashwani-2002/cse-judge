package com.csejudge.app.data

import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.repository.QuestionBankRepository
import com.csejudge.app.data.repository.QuestionBankRepositoryImpl
import com.csejudge.app.data.repository.QuestionBankValidationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

class QuestionBankRepositoryTest {

    private lateinit var repository: QuestionBankRepository

    @Before
    fun setUp() {
        // Points to the project's question-bank directory
        val questionBankDir = File("../question-bank").let {
            if (it.exists()) it else File("question-bank")
        }
        repository = QuestionBankRepositoryImpl(
            openAssetStream = { fileName ->
                val file = File(questionBankDir, fileName)
                require(file.exists()) { "Question bank file not found: ${file.absolutePath}" }
                file.inputStream()
            }
        )
    }

    @Test
    fun testAllSevenFilesLoadSuccessfully() = runBlocking {
        assertFalse(repository.isLoaded)
        val stats = repository.loadQuestionBank()
        assertTrue(repository.isLoaded)
        assertEquals(1432, stats.totalQuestions)
        assertEquals(7, stats.subjectCounts.size)
        println("Question bank loaded in ${stats.loadDurationMs} ms")
    }

    @Test
    fun testTotalQuestionCountIs1432() = runBlocking {
        repository.loadQuestionBank()
        val allQuestions = repository.getAllQuestions()
        assertEquals(1432, allQuestions.size)
    }

    @Test
    fun testSubjectWiseQuestionCounts() = runBlocking {
        repository.loadQuestionBank()

        val expectedCounts = mapOf(
            "AMAC" to 149,
            "Economy" to 243,
            "Environment" to 248,
            "Geography" to 233,
            "Modern History" to 127,
            "Polity" to 252,
            "Sci and Tech" to 180
        )

        val availableSubjects = repository.getAvailableSubjects()
        assertEquals(expectedCounts.keys.sorted(), availableSubjects.sorted())

        for ((subject, expectedCount) in expectedCounts) {
            val questions = repository.getQuestionsForSubject(subject)
            assertEquals("Mismatch in question count for subject '$subject'", expectedCount, questions.size)
        }
    }

    @Test
    fun testModernHistoryMicrothemesAndStrictAbsenceOf1757() = runBlocking {
        repository.loadQuestionBank()

        val mhMicrothemes = repository.getAvailableMicrothemes("Modern History")
        assertEquals(16, mhMicrothemes.size)

        // 1. Verify "From 1857 to 1884 CE" exists and has exactly 5 questions
        assertTrue("Expected 'From 1857 to 1884 CE' in Modern History microthemes",
            mhMicrothemes.contains("From 1857 to 1884 CE")
        )
        val questionsIn1857 = repository.getQuestionsForSubjectAndMicrotheme(
            "Modern History",
            "From 1857 to 1884 CE"
        )
        assertEquals(5, questionsIn1857.size)

        // 2. CRITICAL: Verify "From 1757 to 1884 CE" does NOT exist anywhere in microthemes
        assertFalse("Forbidden 'From 1757 to 1884 CE' must not appear in microtheme list",
            mhMicrothemes.contains("From 1757 to 1884 CE")
        )

        // 3. Verify no question in the entire bank has "1757" in microtheme
        val any1757 = repository.getAllQuestions().any { it.microtheme.contains("1757") }
        assertFalse("No question in the entire bank should reference '1757' in its microtheme", any1757)
    }

    @Test
    fun testInvalidJsonThrowsValidationException() {
        val repo = QuestionBankRepositoryImpl(
            openAssetStream = { fileName ->
                ByteArrayInputStream("invalid json content".toByteArray())
            }
        )
        try {
            runBlocking { repo.loadQuestionBank() }
            fail("Expected QuestionBankValidationException for invalid JSON")
        } catch (e: QuestionBankValidationException) {
            assertTrue(e.message?.contains("JSON parsing error") == true)
        }
    }

    @Test
    fun testMissingAssetFileThrowsValidationException() {
        val repo = QuestionBankRepositoryImpl(
            openAssetStream = { fileName ->
                throw java.io.FileNotFoundException("File not found: $fileName")
            }
        )
        try {
            runBlocking { repo.loadQuestionBank() }
            fail("Expected QuestionBankValidationException for missing asset")
        } catch (e: QuestionBankValidationException) {
            assertTrue(e.message?.contains("Failed to open question bank asset file") == true)
        }
    }

    @Test
    fun testBlankQuestionIdThrowsValidationException() {
        val repo = QuestionBankRepositoryImpl(
            openAssetStream = { fileName ->
                val json = """
                    {
                        "questions": [
                            {
                                "subject": "Polity",
                                "microtheme": "Preamble",
                                "question": ["Statement 1"],
                                "options": {"A":"1","B":"2","C":"3","D":"4"},
                                "correct_answer": "A",
                                "solution": ["Solution 1"],
                                "question_id": ""
                            }
                        ]
                    }
                """.trimIndent()
                ByteArrayInputStream(json.toByteArray())
            }
        )
        try {
            runBlocking { repo.loadQuestionBank() }
            fail("Expected QuestionBankValidationException for blank question_id")
        } catch (e: QuestionBankValidationException) {
            assertTrue(e.message?.contains("Question ID is blank") == true)
        }
    }

    @Test
    fun testInvalidCorrectAnswerThrowsValidationException() {
        val repo = QuestionBankRepositoryImpl(
            openAssetStream = { fileName ->
                val json = """
                    {
                        "questions": [
                            {
                                "subject": "Polity",
                                "microtheme": "Preamble",
                                "question": ["Statement 1"],
                                "options": {"A":"1","B":"2","C":"3","D":"4"},
                                "correct_answer": "E",
                                "solution": ["Solution 1"],
                                "question_id": "TEST0001"
                            }
                        ]
                    }
                """.trimIndent()
                ByteArrayInputStream(json.toByteArray())
            }
        )
        try {
            runBlocking { repo.loadQuestionBank() }
            fail("Expected QuestionBankValidationException for invalid correct_answer 'E'")
        } catch (e: QuestionBankValidationException) {
            assertTrue(e.message?.contains("Invalid correct_answer") == true)
        }
    }

    @Test
    fun testEconomyIncludesMacroEconomy() = runBlocking {
        repository.loadQuestionBank()

        val economyMicrothemes = repository.getAvailableMicrothemes("Economy")
        assertTrue("Economy should include 'Macro economy'", economyMicrothemes.contains("Macro economy"))

        val macroQuestions = repository.getQuestionsForSubjectAndMicrotheme("Economy", "Macro economy")
        assertEquals(5, macroQuestions.size)
        val expectedQids = setOf("ECO00149", "ECO00150", "ECO00151", "ECO00152", "ECO00153")
        assertEquals(expectedQids, macroQuestions.map { it.questionId }.toSet())
    }

    @Test
    fun testNoEmptyMicrothemesExposed() = runBlocking {
        repository.loadQuestionBank()

        for (subject in repository.getAvailableSubjects()) {
            val microthemes = repository.getAvailableMicrothemes(subject)
            assertTrue("Subject '$subject' should have at least 1 microtheme", microthemes.isNotEmpty())

            for (mt in microthemes) {
                val questions = repository.getQuestionsForSubjectAndMicrotheme(subject, mt)
                assertTrue("Microtheme '$mt' under subject '$subject' has 0 questions", questions.isNotEmpty())
            }
        }

        // Specifically check that known empty microthemes from prompt are not present
        val polityMicrothemes = repository.getAvailableMicrothemes("Polity")
        assertFalse(polityMicrothemes.contains("Cooperatives"))
        assertFalse(polityMicrothemes.contains("State Legislature"))

        val amacMicrothemes = repository.getAvailableMicrothemes("AMAC")
        assertFalse(amacMicrothemes.contains("Indo-Saracenic Architecture/Contemporary Architecture"))

        val geoMicrothemes = repository.getAvailableMicrothemes("Geography")
        assertFalse(geoMicrothemes.contains("Humidity and Precipitation"))
        assertFalse(geoMicrothemes.contains("Movement, Relief, Temperature & Salinity of the Oceans"))
        assertFalse(geoMicrothemes.contains("Pressure Belts and Systems"))
        assertFalse(geoMicrothemes.contains("Structure and Composition of Atmosphere"))
        assertFalse(geoMicrothemes.contains("Temperature Distribution and Heat Budget"))
    }

    @Test
    fun testGlobalQuestionIdUniqueness() = runBlocking {
        repository.loadQuestionBank()
        val allQuestions = repository.getAllQuestions()
        val ids = allQuestions.map { it.questionId }
        val uniqueIds = ids.toSet()
        assertEquals(allQuestions.size, uniqueIds.size)
    }

    @Test
    fun testDuplicateQuestionIdRejection() {
        val repoWithDuplicate = QuestionBankRepositoryImpl(
            openAssetStream = { fileName ->
                val json = """
                    {
                        "questions": [
                            {
                                "subject": "Polity",
                                "microtheme": "Preamble",
                                "question": ["Statement 1"],
                                "options": {"A":"1","B":"2","C":"3","D":"4"},
                                "correct_answer": "A",
                                "solution": ["Solution 1"],
                                "question_id": "DUP0001"
                            },
                            {
                                "subject": "Polity",
                                "microtheme": "Preamble",
                                "question": ["Statement 2"],
                                "options": {"A":"1","B":"2","C":"3","D":"4"},
                                "correct_answer": "B",
                                "solution": ["Solution 2"],
                                "question_id": "DUP0001"
                            }
                        ]
                    }
                """.trimIndent()
                ByteArrayInputStream(json.toByteArray())
            }
        )

        try {
            runBlocking { repoWithDuplicate.loadQuestionBank() }
            fail("Expected QuestionBankValidationException for duplicate question_id 'DUP0001'")
        } catch (e: QuestionBankValidationException) {
            assertTrue(e.message?.contains("Duplicate question_id") == true)
        }
    }

    @Test
    fun testQuestionAndSolutionPreserveArrayOrder() = runBlocking {
        repository.loadQuestionBank()

        val q = repository.getQuestionById("POL00001")
        assertNotNull("Question POL00001 should exist", q)
        q!!

        assertEquals("Polity", q.subject)
        assertEquals("Historical Background & Colonial Rule", q.microtheme)
        assertEquals("C", q.correctAnswer)

        // Check question statements order
        assertEquals(6, q.question.size)
        assertEquals("Consider the following statements regarding Morley-Minto Reforms:", q.question[0])
        assertEquals("Statement-I:", q.question[1])
        assertEquals("Elected members of Imperial Legislative Council were elected indirectly by the provincial council.", q.question[2])
        assertEquals("Statement-II:", q.question[3])
        assertEquals("It introduced separate electorate for Muslims and depressed classes.", q.question[4])
        assertEquals("Which one of the following is correct in respect of the above statements?", q.question[5])

        // Check options
        assertEquals(
            "Statement-I is correct but Statement-II is incorrect",
            q.options.c
        )

        // Check solution statements order
        assertTrue(q.solution.size >= 10)
        assertEquals("Correct Option: (c)", q.solution[0])
        assertEquals("Explanation:", q.solution[1])
    }

    @Test
    fun testOptionsAndCorrectAnswerIntegrity() = runBlocking {
        repository.loadQuestionBank()

        for (q in repository.getAllQuestions()) {
            assertTrue("Invalid correct_answer in ${q.questionId}", q.correctAnswer in setOf("A", "B", "C", "D"))
            assertTrue("Option A blank in ${q.questionId}", q.options.a.isNotBlank())
            assertTrue("Option B blank in ${q.questionId}", q.options.b.isNotBlank())
            assertTrue("Option C blank in ${q.questionId}", q.options.c.isNotBlank())
            assertTrue("Option D blank in ${q.questionId}", q.options.d.isNotBlank())
            assertTrue("Question statement lines empty in ${q.questionId}", q.question.isNotEmpty())
            assertTrue("Solution statement lines empty in ${q.questionId}", q.solution.isNotEmpty())
        }
    }

    @Test
    fun testLookupByQuestionId() = runBlocking {
        repository.loadQuestionBank()

        val sampleIds = listOf("AMAC00001", "ECO00001", "ENV00001", "GEO00001", "MH00001", "POL00001", "SNT00001")
        for (id in sampleIds) {
            val q = repository.getQuestionById(id)
            assertNotNull("Question with ID $id should be found", q)
            assertEquals(id, q?.questionId)
        }

        assertNull("Non-existent ID should return null", repository.getQuestionById("INVALID_ID"))
    }
}
