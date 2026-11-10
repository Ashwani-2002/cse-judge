package com.csejudge.app.data.repository

import android.content.Context
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.model.QuestionBankFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream

/**
 * Thrown when the question bank fails validation or parsing.
 */
class QuestionBankValidationException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

/**
 * Statistics regarding loaded question bank.
 */
data class QuestionBankStats(
    val totalQuestions: Int,
    val subjectCounts: Map<String, Int>,
    val microthemeCounts: Map<String, Map<String, Int>>,
    val loadDurationMs: Long
)

/**
 * Repository interface for static question bank access.
 */
interface QuestionBankRepository {
    val isLoaded: Boolean
    suspend fun loadQuestionBank(): QuestionBankStats

    fun getAllQuestions(): List<Question>
    fun getQuestionById(questionId: String): Question?
    fun getAvailableSubjects(): List<String>
    fun getAvailableMicrothemes(subject: String): List<String>
    fun getQuestionsForSubject(subject: String): List<Question>
    fun getQuestionsForSubjectAndMicrotheme(subject: String, microtheme: String): List<Question>
}

/**
 * Default implementation of [QuestionBankRepository].
 * Loads JSON files once on a background dispatcher, performs strict validation,
 * and maintains immutable in-memory lookup indexes.
 */
class QuestionBankRepositoryImpl(
    private val openAssetStream: (fileName: String) -> InputStream,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : QuestionBankRepository {

    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
    }

    @Volatile
    override var isLoaded: Boolean = false
        private set

    // Immutable in-memory indexes built once upon loading
    private var allQuestionsList: List<Question> = emptyList()
    private var questionsByIdMap: Map<String, Question> = emptyMap()
    private var subjectIndexMap: Map<String, List<Question>> = emptyMap()
    private var subjectMicrothemeIndexMap: Map<String, Map<String, List<Question>>> = emptyMap()
    private var availableSubjectsList: List<String> = emptyList()

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun loadQuestionBank(): QuestionBankStats = withContext(ioDispatcher) {
        if (isLoaded) {
            return@withContext computeCurrentStats(0)
        }

        val startTime = System.currentTimeMillis()
        val loadedQuestions = mutableListOf<Question>()
        val seenQuestionIds = mutableMapOf<String, String>() // questionId -> fileName

        for (fileName in QUESTION_BANK_FILES) {
            val stream = try {
                openAssetStream(fileName)
            } catch (e: Exception) {
                throw QuestionBankValidationException("Failed to open question bank asset file '$fileName'", e)
            }

            val bankFile: QuestionBankFile = stream.use { input ->
                try {
                    json.decodeFromStream(input)
                } catch (e: Exception) {
                    throw QuestionBankValidationException("JSON parsing error in file '$fileName': ${e.message}", e)
                }
            }

            if (bankFile.questions.isEmpty()) {
                throw QuestionBankValidationException("File '$fileName' contains no questions")
            }

            for ((index, question) in bankFile.questions.withIndex()) {
                val validationContext = "file '$fileName' at index $index"
                val errors = question.validate(validationContext)
                if (errors.isNotEmpty()) {
                    throw QuestionBankValidationException(
                        "Validation failed for question in $validationContext: ${errors.joinToString("; ")}"
                    )
                }

                // Check global ID uniqueness
                val previousFile = seenQuestionIds.put(question.questionId, fileName)
                if (previousFile != null) {
                    throw QuestionBankValidationException(
                        "Duplicate question_id '${question.questionId}' found in '$fileName' (previously seen in '$previousFile')"
                    )
                }

                loadedQuestions.add(question)
            }
        }

        // Build immutable indexes
        val questionsById = loadedQuestions.associateBy { it.questionId }

        // Group by subject, preserving natural appearance order
        val bySubject = loadedQuestions.groupBy { it.subject }

        // Group by subject then microtheme, strictly filtering out any microtheme with 0 questions
        val bySubjectAndMicrotheme = bySubject.mapValues { (_, questionsInSubject) ->
            questionsInSubject
                .groupBy { it.microtheme }
                .filterValues { it.isNotEmpty() }
        }

        val availableSubjects = bySubject.keys.toList()

        // Assign to volatile / immutable properties
        allQuestionsList = loadedQuestions.toList()
        questionsByIdMap = questionsById
        subjectIndexMap = bySubject
        subjectMicrothemeIndexMap = bySubjectAndMicrotheme
        availableSubjectsList = availableSubjects
        isLoaded = true

        val duration = System.currentTimeMillis() - startTime
        return@withContext computeCurrentStats(duration)
    }

    private fun computeCurrentStats(duration: Long): QuestionBankStats {
        val subjectCounts = subjectIndexMap.mapValues { it.value.size }
        val microthemeCounts = subjectMicrothemeIndexMap.mapValues { (_, mthemes) ->
            mthemes.mapValues { it.value.size }
        }
        return QuestionBankStats(
            totalQuestions = allQuestionsList.size,
            subjectCounts = subjectCounts,
            microthemeCounts = microthemeCounts,
            loadDurationMs = duration
        )
    }

    private fun checkLoaded() {
        check(isLoaded) { "QuestionBankRepository is not loaded yet. Call loadQuestionBank() first." }
    }

    override fun getAllQuestions(): List<Question> {
        checkLoaded()
        return allQuestionsList
    }

    override fun getQuestionById(questionId: String): Question? {
        checkLoaded()
        return questionsByIdMap[questionId]
    }

    override fun getAvailableSubjects(): List<String> {
        checkLoaded()
        return availableSubjectsList
    }

    override fun getAvailableMicrothemes(subject: String): List<String> {
        checkLoaded()
        return subjectMicrothemeIndexMap[subject]?.keys?.toList().orEmpty()
    }

    override fun getQuestionsForSubject(subject: String): List<Question> {
        checkLoaded()
        return subjectIndexMap[subject].orEmpty()
    }

    override fun getQuestionsForSubjectAndMicrotheme(subject: String, microtheme: String): List<Question> {
        checkLoaded()
        return subjectMicrothemeIndexMap[subject]?.get(microtheme).orEmpty()
    }

    companion object {
        val QUESTION_BANK_FILES = listOf(
            "AMAC-qid.json",
            "Economy-qid.json",
            "Environment-qid.json",
            "Geography-qid.json",
            "Modern History-qid.json",
            "Polity-qid.json",
            "Sci and Tech-qid.json"
        )

        /**
         * Standard Android factory using the application's AssetManager.
         */
        fun create(context: Context): QuestionBankRepository {
            val assetManager = context.applicationContext.assets
            return QuestionBankRepositoryImpl(
                openAssetStream = { fileName -> assetManager.open(fileName) }
            )
        }
    }
}
