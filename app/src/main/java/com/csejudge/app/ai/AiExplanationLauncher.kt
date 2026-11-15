package com.csejudge.app.ai

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.model.TestQuestionDetail

/**
 * Reusable launcher for opening Google AI Mode explanations in an external browser.
 * Validates question payload, builds URL, fires Android ACTION_VIEW intent,
 * and handles browser missing or intent resolution errors gracefully.
 */
object AiExplanationLauncher {

    fun launch(
        context: Context,
        subject: String,
        microtheme: String,
        questionStatements: List<String>,
        options: Options,
        correctAnswer: String
    ): Boolean {
        val validationError = AiExplanationPromptBuilder.validateQuestionData(questionStatements, options)
        if (validationError != null) {
            Toast.makeText(
                context,
                "Cannot open AI explanation: $validationError",
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        return try {
            val prompt = AiExplanationPromptBuilder.buildPrompt(
                subject = subject,
                microtheme = microtheme,
                questionStatements = questionStatements,
                options = options,
                correctAnswer = correctAnswer
            )
            val url = AiExplanationUrlGenerator.generateUrl(prompt)
            val uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "No web browser found to open Google AI explanation.",
                Toast.LENGTH_LONG
            ).show()
            false
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to open browser: ${e.localizedMessage ?: "Unknown error"}",
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }

    fun launch(context: Context, question: Question): Boolean = launch(
        context = context,
        subject = question.subject,
        microtheme = question.microtheme,
        questionStatements = question.question,
        options = question.options,
        correctAnswer = question.correctAnswer
    )

    fun launch(context: Context, detail: TestQuestionDetail): Boolean = launch(
        context = context,
        subject = detail.subject,
        microtheme = detail.microtheme,
        questionStatements = detail.questionStatements,
        options = detail.options,
        correctAnswer = detail.correctAnswer
    )
}
