package com.csejudge.app.data.repository

import com.csejudge.app.data.model.Question
import com.csejudge.app.data.model.QuestionBankFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.security.MessageDigest

/**
 * Details of a material content change detected between baseline and updated questions.
 */
data class MaterialChangeDetail(
    val questionId: String,
    val field: String,
    val originalSnippet: String,
    val updatedSnippet: String
)

/**
 * Standalone validation report for checking question-bank JSON files directly.
 */
data class DirectoryValidationReport(
    val scannedFiles: List<String>,
    val totalQuestions: Int,
    val subjectCounts: Map<String, Int>,
    val microthemeCounts: Map<String, Map<String, Int>>,
    val duplicateQuestionIds: List<String>,
    val syntaxErrors: List<String>,
    val schemaErrors: List<String>
) {
    val isValid: Boolean
        get() = duplicateQuestionIds.isEmpty() && syntaxErrors.isEmpty() && schemaErrors.isEmpty()
}

/**
 * Result of comparing an updated question bank against a baseline question bank.
 *
 * Rules enforced:
 * - Permanent question IDs: All baseline question IDs must exist in the updated bank.
 * - Material change flagging: Changes to question wording, options, or correct answer must be flagged for manual review.
 * - Global uniqueness: No duplicate question IDs.
 * - Reordering invariance: Array position must not affect question identity.
 * - Dynamic discovery: Newly added questions, microthemes, and subjects are detected and reported.
 */
data class UpdateValidationResult(
    val totalBaselineQuestions: Int,
    val totalUpdatedQuestions: Int,
    val preservedQuestionsCount: Int,
    val newlyAddedQuestions: List<Question>,
    val newlyAddedSubjects: Set<String>,
    val newlyAddedMicrothemes: Map<String, Set<String>>,
    val missingQuestionIds: List<String>,
    val duplicateQuestionIds: List<String>,
    val materialContentChanges: List<MaterialChangeDetail>,
    val validationErrors: List<String>
) {
    val hasFatalErrors: Boolean
        get() = missingQuestionIds.isNotEmpty() || duplicateQuestionIds.isNotEmpty() || validationErrors.isNotEmpty()

    val hasMaterialChanges: Boolean
        get() = materialContentChanges.isNotEmpty()

    val isValidForRelease: Boolean
        get() = !hasFatalErrors && !hasMaterialChanges
}

/**
 * Validator responsible for verifying question bank updates between application versions.
 * Ensures update compatibility, preserves user data integrity, and flags unintended content alterations.
 */
object QuestionBankUpdateValidator {

    /**
     * Compares an [updated] question bank against a [baseline] question bank.
     *
     * @param baseline The question bank from the prior release (e.g. Version 1).
     * @param updated The candidate question bank for the next release (e.g. Version 2).
     * @return [UpdateValidationResult] detailing compatibility, added/missing items, and flagged material changes.
     */
    fun validateUpdate(
        baseline: List<Question>,
        updated: List<Question>
    ): UpdateValidationResult {
        val baselineById = baseline.associateBy { it.questionId }
        val updatedById = mutableMapOf<String, Question>()
        val duplicates = mutableListOf<String>()
        val errors = mutableListOf<String>()

        // 1. Check for duplicates in updated questions and validate basic structure
        for ((index, q) in updated.withIndex()) {
            val qErrors = q.validate("updated question at index $index (id: ${q.questionId})")
            if (qErrors.isNotEmpty()) {
                errors.addAll(qErrors)
            }
            if (updatedById.containsKey(q.questionId)) {
                duplicates.add(q.questionId)
            } else {
                updatedById[q.questionId] = q
            }
        }

        // 2. Verify all baseline question IDs exist in updated bank (never delete or drop existing IDs)
        val missingIds = mutableListOf<String>()
        val materialChanges = mutableListOf<MaterialChangeDetail>()
        var preservedCount = 0

        for ((id, baseQ) in baselineById) {
            val newQ = updatedById[id]
            if (newQ == null) {
                missingIds.add(id)
            } else {
                preservedCount++
                // Detect material changes
                detectMaterialChanges(baseQ, newQ, materialChanges)
            }
        }

        // 3. Identify newly added questions
        val newlyAddedQuestions = updated.filter { !baselineById.containsKey(it.questionId) }

        // 4. Discover newly added subjects and microthemes
        val baselineSubjects = baseline.map { it.subject }.toSet()
        val updatedSubjects = updated.map { it.subject }.toSet()
        val newlyAddedSubjects = updatedSubjects - baselineSubjects

        val baselineMicrothemesBySubject = baseline.groupBy({ it.subject }, { it.microtheme })
            .mapValues { it.value.toSet() }
        val updatedMicrothemesBySubject = updated.groupBy({ it.subject }, { it.microtheme })
            .mapValues { it.value.toSet() }

        val newlyAddedMicrothemes = mutableMapOf<String, Set<String>>()
        for ((subject, newMthemes) in updatedMicrothemesBySubject) {
            val baseMthemes = baselineMicrothemesBySubject[subject].orEmpty()
            val added = newMthemes - baseMthemes
            if (added.isNotEmpty()) {
                newlyAddedMicrothemes[subject] = added
            }
        }

        return UpdateValidationResult(
            totalBaselineQuestions = baseline.size,
            totalUpdatedQuestions = updated.size,
            preservedQuestionsCount = preservedCount,
            newlyAddedQuestions = newlyAddedQuestions,
            newlyAddedSubjects = newlyAddedSubjects,
            newlyAddedMicrothemes = newlyAddedMicrothemes,
            missingQuestionIds = missingIds,
            duplicateQuestionIds = duplicates.distinct(),
            materialContentChanges = materialChanges,
            validationErrors = errors
        )
    }

    /**
     * Checks if a question's wording, options, or correct answer has changed materially.
     */
    private fun detectMaterialChanges(
        baseQ: Question,
        newQ: Question,
        changes: MutableList<MaterialChangeDetail>
    ) {
        val baseText = baseQ.question.joinToString("\n").trim()
        val newText = newQ.question.joinToString("\n").trim()
        if (baseText != newText) {
            changes.add(
                MaterialChangeDetail(
                    questionId = baseQ.questionId,
                    field = "question_text",
                    originalSnippet = baseText.take(120),
                    updatedSnippet = newText.take(120)
                )
            )
        }

        // Check options
        if (baseQ.options.a.trim() != newQ.options.a.trim()) {
            changes.add(
                MaterialChangeDetail(
                    questionId = baseQ.questionId,
                    field = "option_A",
                    originalSnippet = baseQ.options.a,
                    updatedSnippet = newQ.options.a
                )
            )
        }
        if (baseQ.options.b.trim() != newQ.options.b.trim()) {
            changes.add(
                MaterialChangeDetail(
                    questionId = baseQ.questionId,
                    field = "option_B",
                    originalSnippet = baseQ.options.b,
                    updatedSnippet = newQ.options.b
                )
            )
        }
        if (baseQ.options.c.trim() != newQ.options.c.trim()) {
            changes.add(
                MaterialChangeDetail(
                    questionId = baseQ.questionId,
                    field = "option_C",
                    originalSnippet = baseQ.options.c,
                    updatedSnippet = newQ.options.c
                )
            )
        }
        if (baseQ.options.d.trim() != newQ.options.d.trim()) {
            changes.add(
                MaterialChangeDetail(
                    questionId = baseQ.questionId,
                    field = "option_D",
                    originalSnippet = baseQ.options.d,
                    updatedSnippet = newQ.options.d
                )
            )
        }

        // Check correct answer
        if (baseQ.correctAnswer.trim().uppercase() != newQ.correctAnswer.trim().uppercase()) {
            changes.add(
                MaterialChangeDetail(
                    questionId = baseQ.questionId,
                    field = "correct_answer",
                    originalSnippet = baseQ.correctAnswer,
                    updatedSnippet = newQ.correctAnswer
                )
            )
        }
    }

    /**
     * Computes a cryptographic SHA-256 fingerprint of a question's core content.
     */
    fun computeContentHash(question: Question): String {
        val canonicalRepresentation = buildString {
            append(question.questionId).append("|")
            append(question.subject.trim()).append("|")
            append(question.microtheme.trim()).append("|")
            append(question.question.joinToString("\n") { it.trim() }).append("|")
            append(question.options.a.trim()).append("|")
            append(question.options.b.trim()).append("|")
            append(question.options.c.trim()).append("|")
            append(question.options.d.trim()).append("|")
            append(question.correctAnswer.trim().uppercase()).append("|")
            append(question.solution.joinToString("\n") { it.trim() })
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(canonicalRepresentation.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Validates all question bank JSON files in [directory] standalone.
     * Detects:
     * - Syntax errors in JSON
     * - Missing/blank question IDs
     * - Duplicate question IDs across all files
     * - Missing/blank options
     * - Invalid correct answers
     * - Empty question text or solution
     * - Blank subjects or microthemes
     * - Files with 0 questions
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun validateDirectory(
        directory: File,
        json: Json = Json { ignoreUnknownKeys = false }
    ): DirectoryValidationReport {
        val files = directory.listFiles { _, name -> name.endsWith(".json") }?.sortedBy { it.name }.orEmpty()
        val scannedFiles = mutableListOf<String>()
        val allQuestions = mutableListOf<Question>()
        val seenIds = mutableMapOf<String, String>()
        val duplicateIds = mutableListOf<String>()
        val syntaxErrors = mutableListOf<String>()
        val schemaErrors = mutableListOf<String>()

        for (file in files) {
            scannedFiles.add(file.name)
            val bankFile = try {
                file.inputStream().use { input ->
                    json.decodeFromStream<QuestionBankFile>(input)
                }
            } catch (e: Exception) {
                syntaxErrors.add("File '${file.name}': JSON parsing error: ${e.message}")
                continue
            }

            if (bankFile.questions.isEmpty()) {
                schemaErrors.add("File '${file.name}': contains 0 questions")
            }

            for ((index, q) in bankFile.questions.withIndex()) {
                val context = "File '${file.name}', index $index (id: ${q.questionId})"
                val errors = q.validate(context)
                if (errors.isNotEmpty()) {
                    schemaErrors.addAll(errors)
                }
                val prev = seenIds.put(q.questionId, file.name)
                if (prev != null) {
                    duplicateIds.add("${q.questionId} (in '${file.name}' and '$prev')")
                }
                allQuestions.add(q)
            }
        }

        val subjectCounts = allQuestions.groupBy { it.subject }.mapValues { it.value.size }
        val microthemeCounts = allQuestions.groupBy { it.subject }.mapValues { (_, qs) ->
            qs.groupBy { it.microtheme }.mapValues { it.value.size }
        }

        return DirectoryValidationReport(
            scannedFiles = scannedFiles,
            totalQuestions = allQuestions.size,
            subjectCounts = subjectCounts,
            microthemeCounts = microthemeCounts,
            duplicateQuestionIds = duplicateIds.distinct(),
            syntaxErrors = syntaxErrors,
            schemaErrors = schemaErrors
        )
    }
}
