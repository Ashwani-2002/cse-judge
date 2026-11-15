package com.csejudge.app.data.repository

import android.content.Context
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.model.QuestionBankFile
import com.csejudge.app.data.security.QuestionBankCrypto
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
    private val fileNames: List<String> = DEFAULT_QUESTION_BANK_FILES,
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

        for (fileName in fileNames) {
            val stream = try {
                val rawStream = openAssetStream(fileName)
                if (fileName.endsWith(".enc")) {
                    QuestionBankCrypto.decryptStream(rawStream)
                } else {
                    rawStream
                }
            } catch (e: Exception) {
                throw QuestionBankValidationException("Failed to open question bank asset file '$fileName': ${e.message}", e)
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
        val DEFAULT_QUESTION_BANK_FILES = listOf(
            "AMAC-qid.json",
            "Economy-qid.json",
            "Environment-qid.json",
            "Geography-qid.json",
            "Modern History-qid.json",
            "Polity-qid.json",
            "Sci and Tech-qid.json"
        )

        // Backward-compatible alias for existing tests and references
        val QUESTION_BANK_FILES = DEFAULT_QUESTION_BANK_FILES

        @Volatile
        private var instance: QuestionBankRepository? = null

        /**
         * Gets or creates the shared singleton instance of [QuestionBankRepository].
         * Prevents duplicate multi-megabyte JSON loading and parsing across ViewModels.
         */
        fun getInstance(context: Context): QuestionBankRepository {
            return instance ?: synchronized(this) {
                instance ?: create(context).also { instance = it }
            }
        }

        /**
         * Resets the singleton instance (primarily for testing purposes).
         */
        fun resetInstanceForTesting() {
            synchronized(this) {
                instance = null
            }
        }

        /**
         * Standard Android factory using the application's AssetManager.
         * Dynamically discovers question bank files ending with ".enc" or ".json",
         * falling back to [DEFAULT_QUESTION_BANK_FILES] if listing is unavailable.
         */
        fun create(context: Context): QuestionBankRepository {
            val assetManager = context.applicationContext.assets
            val discoveredFiles = runCatching {
                val list = assetManager.list("")?.toList().orEmpty()
                val encFiles = list.filter { it.endsWith(".enc") }.sorted()
                if (encFiles.isNotEmpty()) {
                    encFiles
                } else {
                    list.filter { it.endsWith(".json") }.sorted()
                }
            }.getOrNull()

            val filesToLoad = if (!discoveredFiles.isNullOrEmpty()) {
                discoveredFiles
            } else {
                DEFAULT_QUESTION_BANK_FILES
            }

            return QuestionBankRepositoryImpl(
                openAssetStream = { fileName -> assetManager.open(fileName) },
                fileNames = filesToLoad
            )
        }

        /**
         * Directory-based factory for unit tests, offline validation tools, and build verification.
         * Dynamically discovers question bank files in [directory] without hardcoded file lists.
         */
        fun createFromDirectory(
            directory: java.io.File,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        ): QuestionBankRepository {
            require(directory.exists() && directory.isDirectory) {
                "Question bank directory not found: ${directory.absolutePath}"
            }
            val encFiles = directory.listFiles { _, name -> name.endsWith(".enc") }
                ?.map { it.name }
                ?.sorted()
                .orEmpty()
            val filesToLoad = if (encFiles.isNotEmpty()) {
                encFiles
            } else {
                directory.listFiles { _, name -> name.endsWith(".json") }
                    ?.map { it.name }
                    ?.sorted()
                    .orEmpty()
                    .ifEmpty { DEFAULT_QUESTION_BANK_FILES }
            }

            return QuestionBankRepositoryImpl(
                openAssetStream = { fileName -> java.io.File(directory, fileName).inputStream() },
                fileNames = filesToLoad,
                ioDispatcher = ioDispatcher
            )
        }
    }
}
