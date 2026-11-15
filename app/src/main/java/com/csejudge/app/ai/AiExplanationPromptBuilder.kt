package com.csejudge.app.ai

import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.model.TestQuestionDetail

/**
 * Builds the structured Google AI Mode prompt for UPSC Prelims multiple-choice questions.
 * Preserves complete question statements, numbered items, order, all four options,
 * and official/recorded correct answers without truncating.
 */
object AiExplanationPromptBuilder {

    /**
     * Constructs a detailed, structured prompt from raw question parameters.
     */
    fun buildPrompt(
        subject: String,
        microtheme: String,
        questionStatements: List<String>,
        options: Options,
        correctAnswer: String
    ): String {
        val questionText = questionStatements.joinToString("\n") { it.trim() }
        val sb = StringBuilder()
        sb.append("Explain the following UPSC Civil Services Preliminary Examination multiple-choice question in detail.\n\n")
        sb.append("Subject: ").append(subject.trim()).append("\n")
        sb.append("Microtheme: ").append(microtheme.trim()).append("\n\n")
        sb.append("QUESTION:\n").append(questionText).append("\n\n")
        sb.append("OPTIONS:\n")
        sb.append("A. ").append(options.a.trim()).append("\n")
        sb.append("B. ").append(options.b.trim()).append("\n")
        sb.append("C. ").append(options.c.trim()).append("\n")
        sb.append("D. ").append(options.d.trim()).append("\n\n")
        sb.append("OFFICIAL/RECORDED CORRECT ANSWER:\n").append(correctAnswer.trim()).append("\n\n")
        sb.append("INSTRUCTIONS:\n")
        sb.append("1. Explain the question in a detailed and conceptually clear manner.\n")
        sb.append("2. Explain the underlying concepts required to solve the question.\n")
        sb.append("3. Evaluate all four options individually.\n")
        sb.append("4. Explain why the correct option is correct.\n")
        sb.append("5. Explain why each incorrect option is incorrect.\n")
        sb.append("6. Explain relevant facts, provisions, examples, or conceptual distinctions wherever applicable.\n")
        sb.append("7. Highlight important UPSC examination takeaways.\n")
        sb.append("8. Do not invent facts. Clearly mention any ambiguity or factual uncertainty.\n")
        sb.append("9. Structure the explanation using headings and bullet points for readability.")
        return sb.toString()
    }

    /**
     * Convenience builder for [Question] domain models (Practice and Bookmark flows).
     */
    fun buildPrompt(question: Question): String = buildPrompt(
        subject = question.subject,
        microtheme = question.microtheme,
        questionStatements = question.question,
        options = question.options,
        correctAnswer = question.correctAnswer
    )

    /**
     * Convenience builder for [TestQuestionDetail] models (Test Report flows).
     */
    fun buildPrompt(detail: TestQuestionDetail): String = buildPrompt(
        subject = detail.subject,
        microtheme = detail.microtheme,
        questionStatements = detail.questionStatements,
        options = detail.options,
        correctAnswer = detail.correctAnswer
    )

    /**
     * Validates that critical question data fields are non-empty and well-formed.
     * Returns an error description if invalid, or null if valid.
     */
    fun validateQuestionData(
        questionStatements: List<String>,
        options: Options
    ): String? {
        if (questionStatements.isEmpty() || questionStatements.all { it.isBlank() }) {
            return "Question statement is empty"
        }
        if (options.a.isBlank() || options.b.isBlank() || options.c.isBlank() || options.d.isBlank()) {
            return "One or more options are missing"
        }
        return null
    }
}
