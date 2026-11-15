package com.csejudge.app.ai

import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.model.TestQuestionDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Automated tests for Stage 14: Google AI Mode Explanation Feature.
 * Verifies prompt construction, query formatting, single-time percent encoding,
 * Unicode handling, options preservation, and historical snapshot compatibility.
 */
class AiExplanationTest {

    private val sampleQuestion = Question(
        subject = "Indian Polity",
        microtheme = "Fundamental Rights",
        question = listOf(
            "Consider the following statements regarding Article 21:",
            "1. It guarantees the right to life and personal liberty.",
            "2. It is available only to Indian citizens.",
            "Which of the statements given above is/are correct?"
        ),
        options = Options(
            a = "1 only",
            b = "2 only",
            c = "Both 1 and 2",
            d = "Neither 1 nor 2"
        ),
        correctAnswer = "A",
        solution = listOf(
            "Correct Option: A",
            "Explanation: Article 21 is available to both citizens and non-citizens (foreigners)."
        ),
        questionId = "POL00042"
    )

    private val sampleSnapshotDetail = TestQuestionDetail(
        questionOrder = 1,
        questionId = "HIST00099",
        subject = "Modern History",
        microtheme = "Freedom Struggle",
        questionStatements = listOf(
            "With reference to the Indian National Congress session of 1929, consider the following:",
            "1. It was presided over by Jawaharlal Nehru.",
            "2. The resolution of 'Poorna Swaraj' was passed."
        ),
        options = Options(
            a = "1 only",
            b = "2 only",
            c = "Both 1 and 2",
            d = "Neither 1 nor 2"
        ),
        correctAnswer = "C",
        solutionStatements = listOf(
            "Correct Option: C",
            "Lahore session 1929 was historic where Poorna Swaraj resolution was passed under Nehru's presidency."
        ),
        selectedAnswer = "C",
        isMarkedForReview = false,
        status = "CORRECT"
    )

    // 1. Correct URL generation
    @Test
    fun test1_CorrectUrlGeneration() {
        val prompt = "Explain question in detail"
        val url = AiExplanationUrlGenerator.generateUrl(prompt)

        assertTrue("URL must start with Google AI search endpoint", url.startsWith("https://www.google.com/search?udm=50&q="))
        assertEquals("https://www.google.com/search?udm=50&q=Explain%20question%20in%20detail", url)
    }

    // 2. Correct encoding of spaces
    @Test
    fun test2_CorrectEncodingOfSpaces() {
        val textWithSpaces = "UPSC Civil Services Preliminary Examination"
        val encoded = AiExplanationUrlGenerator.encodeQuery(textWithSpaces)

        // Spaces must be encoded as %20 rather than unencoded spaces or literal '+'
        assertTrue("Encoded query must contain %20", encoded.contains("%20"))
        assertFalse("Encoded query must not contain raw whitespace", encoded.contains(" "))
        assertFalse("Encoded query spaces must not be left as '+'", encoded.contains("+"))
        assertEquals("UPSC%20Civil%20Services%20Preliminary%20Examination", encoded)

        // Verify decoding roundtrip
        val decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
        assertEquals(textWithSpaces, decoded)
    }

    // 3. Correct encoding of multiple question statements and newlines
    @Test
    fun test3_CorrectEncodingOfMultipleQuestionStatementsAndNewlines() {
        val multiLineText = "Statement 1\nStatement 2\nWhich is correct?"
        val encoded = AiExplanationUrlGenerator.encodeQuery(multiLineText)

        // Newline characters (\n) must be encoded as %0A
        assertTrue("Encoded query must contain %0A for newlines", encoded.contains("%0A"))
        assertFalse("Encoded query must not contain literal newlines", encoded.contains("\n"))

        val decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
        assertEquals(multiLineText, decoded)
    }

    // 4. Correct encoding of special characters
    @Test
    fun test4_CorrectEncodingOfSpecialCharacters() {
        val textWithSpecialChars = "Rate: 50% & Ratio = 1/2? Is it (A) or (B) #test & \"quoted\""
        val encoded = AiExplanationUrlGenerator.encodeQuery(textWithSpecialChars)

        // Reserved characters in query parameters must be percent-encoded
        assertTrue("Percent symbol must be encoded as %25", encoded.contains("%25"))
        assertTrue("Ampersand must be encoded as %26", encoded.contains("%26"))
        assertTrue("Equals sign must be encoded as %3D", encoded.contains("%3D"))
        assertTrue("Question mark must be encoded as %3F", encoded.contains("%3F"))
        assertTrue("Slash must be encoded as %2F", encoded.contains("%2F"))
        assertTrue("Hash must be encoded as %23", encoded.contains("%23"))
        assertTrue("Quotes must be encoded as %22", encoded.contains("%22"))

        // Decoding must recover the exact original text faithfully
        val decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
        assertEquals(textWithSpecialChars, decoded)
    }

    // 5. Correct handling of Unicode
    @Test
    fun test5_CorrectHandlingOfUnicode() {
        val hindiText = "निम्नलिखित कथनों पर विचार कीजिए: भारत एक संप्रभु राष्ट्र है।"
        val encoded = AiExplanationUrlGenerator.encodeQuery(hindiText)

        // Unicode multi-byte UTF-8 sequences must be percent-encoded
        assertTrue("Hindi text must be percent-encoded", encoded.contains("%E0%A4"))
        assertFalse("Encoded string must not contain raw non-ASCII characters", encoded.any { it.code > 127 })

        val decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
        assertEquals(hindiText, decoded)
    }

    // 6. Inclusion of all four options
    @Test
    fun test6_InclusionOfAllFourOptions() {
        val prompt = AiExplanationPromptBuilder.buildPrompt(sampleQuestion)

        assertTrue("Must include OPTIONS section header", prompt.contains("OPTIONS:"))
        assertTrue("Must include Option A with label and content", prompt.contains("A. 1 only"))
        assertTrue("Must include Option B with label and content", prompt.contains("B. 2 only"))
        assertTrue("Must include Option C with label and content", prompt.contains("C. Both 1 and 2"))
        assertTrue("Must include Option D with label and content", prompt.contains("D. Neither 1 nor 2"))
    }

    // 7. Inclusion of the correct answer
    @Test
    fun test7_InclusionOfCorrectAnswer() {
        val prompt = AiExplanationPromptBuilder.buildPrompt(sampleQuestion)

        assertTrue(
            "Must include OFFICIAL/RECORDED CORRECT ANSWER section",
            prompt.contains("OFFICIAL/RECORDED CORRECT ANSWER:\nA")
        )
    }

    // 8. Inclusion of subject and microtheme
    @Test
    fun test8_InclusionOfSubjectAndMicrotheme() {
        val prompt = AiExplanationPromptBuilder.buildPrompt(sampleQuestion)

        assertTrue("Must include Subject line", prompt.contains("Subject: Indian Polity"))
        assertTrue("Must include Microtheme line", prompt.contains("Microtheme: Fundamental Rights"))
    }

    // 9. Correct query construction for Practice questions
    @Test
    fun test9_CorrectQueryConstructionForPracticeQuestions() {
        val prompt = AiExplanationPromptBuilder.buildPrompt(sampleQuestion)

        // Verify preamble and instructions
        assertTrue(prompt.startsWith("Explain the following UPSC Civil Services Preliminary Examination multiple-choice question in detail."))
        assertTrue(prompt.contains("QUESTION:\nConsider the following statements regarding Article 21:\n1. It guarantees the right to life and personal liberty.\n2. It is available only to Indian citizens.\nWhich of the statements given above is/are correct?"))
        assertTrue(prompt.contains("INSTRUCTIONS:"))
        assertTrue(prompt.contains("1. Explain the question in a detailed and conceptually clear manner."))
        assertTrue(prompt.contains("2. Explain the underlying concepts required to solve the question."))
        assertTrue(prompt.contains("3. Evaluate all four options individually."))
        assertTrue(prompt.contains("4. Explain why the correct option is correct."))
        assertTrue(prompt.contains("5. Explain why each incorrect option is incorrect."))
        assertTrue(prompt.contains("6. Explain relevant facts, provisions, examples, or conceptual distinctions wherever applicable."))
        assertTrue(prompt.contains("7. Highlight important UPSC examination takeaways."))
        assertTrue(prompt.contains("8. Do not invent facts. Clearly mention any ambiguity or factual uncertainty."))
        assertTrue(prompt.contains("9. Structure the explanation using headings and bullet points for readability."))

        // Verify existing stored solution is NOT in the query
        assertFalse(
            "Existing stored solution must not be included in the AI explanation query",
            prompt.contains("Article 21 is available to both citizens and non-citizens")
        )
    }

    // 10. Correct query construction using historical Test snapshots
    @Test
    fun test10_CorrectQueryConstructionUsingHistoricalTestSnapshots() {
        val prompt = AiExplanationPromptBuilder.buildPrompt(sampleSnapshotDetail)

        assertTrue("Must include Subject from snapshot", prompt.contains("Subject: Modern History"))
        assertTrue("Must include Microtheme from snapshot", prompt.contains("Microtheme: Freedom Struggle"))
        assertTrue("Must include full question statements from snapshot", prompt.contains("Lahore session 1929 was historic") == false)
        assertTrue("Must include question statements", prompt.contains("With reference to the Indian National Congress session of 1929"))
        assertTrue("Must include options from snapshot", prompt.contains("A. 1 only"))
        assertTrue("Must include correct answer from snapshot", prompt.contains("OFFICIAL/RECORDED CORRECT ANSWER:\nC"))

        val url = AiExplanationUrlGenerator.generateUrl(prompt)
        assertTrue("Snapshot URL must be valid Google AI search URL", url.startsWith("https://www.google.com/search?udm=50&q="))
    }

    // 11. URL generation exactly once without double encoding
    @Test
    fun test11_UrlGenerationExactlyOnceWithoutDoubleEncoding() {
        val prompt = AiExplanationPromptBuilder.buildPrompt(sampleQuestion)
        val url = AiExplanationUrlGenerator.generateUrl(prompt)

        // If double-encoded, '%20' would become '%2520' or '%0A' would become '%250A'
        assertFalse("Must not have double-encoded spaces (%2520)", url.contains("%2520"))
        assertFalse("Must not have double-encoded newlines (%250A)", url.contains("%250A"))

        // Extract query parameter and decode once
        val prefix = "https://www.google.com/search?udm=50&q="
        val encodedQuery = url.substring(prefix.length)
        val decodedOnce = URLDecoder.decode(encodedQuery, StandardCharsets.UTF_8.name())

        assertEquals("Single URL decoding must yield the exact original prompt text", prompt, decodedOnce)
    }

    // Validation tests
    @Test
    fun testValidation_EmptyQuestionStatementReturnsError() {
        val error = AiExplanationPromptBuilder.validateQuestionData(
            questionStatements = emptyList(),
            options = sampleQuestion.options
        )
        assertNotNull("Empty question statement must produce validation error", error)
        assertTrue(error!!.contains("empty", ignoreCase = true))

        val errorBlank = AiExplanationPromptBuilder.validateQuestionData(
            questionStatements = listOf("   ", ""),
            options = sampleQuestion.options
        )
        assertNotNull("Blank question statement must produce validation error", errorBlank)
    }

    @Test
    fun testValidation_BlankOptionReturnsError() {
        val invalidOptions = Options(
            a = "Option A",
            b = "",
            c = "Option C",
            d = "Option D"
        )
        val error = AiExplanationPromptBuilder.validateQuestionData(
            questionStatements = listOf("Valid statement"),
            options = invalidOptions
        )
        assertNotNull("Blank option must produce validation error", error)
        assertTrue(error!!.contains("missing", ignoreCase = true))
    }

    @Test
    fun testValidation_ValidDataReturnsNull() {
        val error = AiExplanationPromptBuilder.validateQuestionData(
            questionStatements = sampleQuestion.question,
            options = sampleQuestion.options
        )
        assertNull("Valid question data must return null", error)
    }
}
