package com.csejudge.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a single UPSC practice question from the question bank.
 * Order of items in [question] and [solution] is preserved as in the source JSON.
 */
@Serializable
data class Question(
    val subject: String,
    val microtheme: String,
    val question: List<String>,
    val options: Options,
    @SerialName("correct_answer")
    val correctAnswer: String,
    val solution: List<String>,
    @SerialName("question_id")
    val questionId: String
) {
    /**
     * Validates that all fields comply with domain constraints.
     * Returns a list of validation error descriptions (empty if valid).
     */
    fun validate(contextInfo: String = ""): List<String> {
        val errors = mutableListOf<String>()
        if (questionId.isBlank()) errors.add("Question ID is blank in $contextInfo")
        if (subject.isBlank()) errors.add("Subject is blank for question $questionId in $contextInfo")
        if (microtheme.isBlank()) errors.add("Microtheme is blank for question $questionId in $contextInfo")
        if (question.isEmpty()) errors.add("Question text lines are empty for question $questionId in $contextInfo")
        if (options.a.isBlank() || options.b.isBlank() || options.c.isBlank() || options.d.isBlank()) {
            errors.add("One or more options are blank for question $questionId in $contextInfo")
        }
        if (correctAnswer !in VALID_OPTIONS) {
            errors.add("Invalid correct_answer '$correctAnswer' for question $questionId in $contextInfo (expected A, B, C, or D)")
        }
        if (solution.isEmpty()) errors.add("Solution text lines are empty for question $questionId in $contextInfo")
        return errors
    }

    companion object {
        val VALID_OPTIONS = setOf("A", "B", "C", "D")
    }
}

/**
 * Four vertically stacked options for a question (A, B, C, D).
 */
@Serializable
data class Options(
    @SerialName("A")
    val a: String,
    @SerialName("B")
    val b: String,
    @SerialName("C")
    val c: String,
    @SerialName("D")
    val d: String
) {
    fun get(key: String): String? = when (key.trim().uppercase()) {
        "A" -> a
        "B" -> b
        "C" -> c
        "D" -> d
        else -> null
    }

    fun toMap(): Map<String, String> = mapOf(
        "A" to a,
        "B" to b,
        "C" to c,
        "D" to d
    )
}

/**
 * Top-level structure of each subject-wise JSON file.
 */
@Serializable
data class QuestionBankFile(
    val questions: List<Question>
)
