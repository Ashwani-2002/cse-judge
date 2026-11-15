package com.csejudge.app.data.repository

import android.content.Context
import com.csejudge.app.data.local.AppDatabase
import com.csejudge.app.data.local.AttemptedQuestionEntity
import com.csejudge.app.data.model.FullAnalysisData
import com.csejudge.app.data.model.MicrothemeAnalysis
import com.csejudge.app.data.model.OverallAnalysis
import com.csejudge.app.data.model.SubjectAnalysis
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository interface for question coverage and answer accuracy analysis.
 */
interface AnalysisRepository {
    /**
     * Observes the complete analysis snapshot reactively across all levels.
     */
    fun observeFullAnalysis(): Flow<FullAnalysisData>

    /**
     * Observes Level 1 overall analysis.
     */
    fun observeOverallAnalysis(): Flow<OverallAnalysis>

    /**
     * Observes Level 2 subject-wise analysis list.
     */
    fun observeSubjectAnalysisList(): Flow<List<SubjectAnalysis>>

    /**
     * Observes Level 3 microtheme analysis list for the given [subject].
     */
    fun observeMicrothemeAnalysisList(subject: String): Flow<List<MicrothemeAnalysis>>

    /**
     * One-shot retrieval of the complete analysis snapshot.
     */
    suspend fun getFullAnalysis(): FullAnalysisData

    /**
     * One-shot retrieval of Level 1 overall analysis.
     */
    suspend fun getOverallAnalysis(): OverallAnalysis

    /**
     * One-shot retrieval of Level 2 subject-wise analysis list.
     */
    suspend fun getSubjectAnalysisList(): List<SubjectAnalysis>

    /**
     * One-shot retrieval of Level 3 microtheme analysis list for the given [subject].
     */
    suspend fun getMicrothemeAnalysisList(subject: String): List<MicrothemeAnalysis>
}

/**
 * Default implementation of [AnalysisRepository].
 *
 * Derives coverage and accuracy dynamically from immutable [QuestionBankRepository]
 * indexes and reactive Room attempt records from [PracticeHistoryRepository].
 */
class AnalysisRepositoryImpl(
    private val questionBankRepository: QuestionBankRepository,
    private val practiceHistoryRepository: PracticeHistoryRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val onUnmatchedAttempt: ((questionId: String) -> Unit)? = null
) : AnalysisRepository {

    private suspend fun ensureQuestionBankLoaded() {
        if (!questionBankRepository.isLoaded) {
            questionBankRepository.loadQuestionBank()
        }
    }

    override fun observeFullAnalysis(): Flow<FullAnalysisData> {
        return practiceHistoryRepository.observeAllAttempts()
            .map { attempts ->
                ensureQuestionBankLoaded()
                computeFullAnalysis(attempts)
            }
            .flowOn(ioDispatcher)
    }

    override fun observeOverallAnalysis(): Flow<OverallAnalysis> {
        return observeFullAnalysis().map { it.overall }
    }

    override fun observeSubjectAnalysisList(): Flow<List<SubjectAnalysis>> {
        return observeFullAnalysis().map { it.subjects }
    }

    override fun observeMicrothemeAnalysisList(subject: String): Flow<List<MicrothemeAnalysis>> {
        return observeFullAnalysis().map { it.microthemesBySubject[subject].orEmpty() }
    }

    override suspend fun getFullAnalysis(): FullAnalysisData = withContext(ioDispatcher) {
        ensureQuestionBankLoaded()
        val attempts = practiceHistoryRepository.getAllAttempts()
        computeFullAnalysis(attempts)
    }

    override suspend fun getOverallAnalysis(): OverallAnalysis = withContext(ioDispatcher) {
        getFullAnalysis().overall
    }

    override suspend fun getSubjectAnalysisList(): List<SubjectAnalysis> = withContext(ioDispatcher) {
        getFullAnalysis().subjects
    }

    override suspend fun getMicrothemeAnalysisList(subject: String): List<MicrothemeAnalysis> = withContext(ioDispatcher) {
        getFullAnalysis().microthemesBySubject[subject].orEmpty()
    }

    /**
     * Pure, deterministic aggregation of attempts against question bank data.
     */
    internal fun computeFullAnalysis(attempts: List<AttemptedQuestionEntity>): FullAnalysisData {
        val allQuestions = questionBankRepository.getAllQuestions()
        val availableSubjects = questionBankRepository.getAvailableSubjects()
        val questionMap = allQuestions.associateBy { it.questionId }

        val unmatchedAttemptIds = mutableListOf<String>()
        val matchedAttempts = mutableMapOf<String, AttemptedQuestionEntity>()

        for (attempt in attempts) {
            if (questionMap.containsKey(attempt.questionId)) {
                matchedAttempts[attempt.questionId] = attempt
            } else {
                unmatchedAttemptIds.add(attempt.questionId)
                onUnmatchedAttempt?.invoke(attempt.questionId)
            }
        }

        // 1. Overall analysis
        val totalQuestions = allQuestions.size
        val correctAnswers = matchedAttempts.values.count { it.isCorrect }
        val incorrectAnswers = matchedAttempts.values.count { !it.isCorrect }
        val overall = OverallAnalysis.create(
            totalQuestions = totalQuestions,
            correctAnswers = correctAnswers,
            incorrectAnswers = incorrectAnswers
        )

        // 2. Subject-wise analysis
        val subjects = availableSubjects.map { subject ->
            val subjectQuestions = questionBankRepository.getQuestionsForSubject(subject)
            val sTotal = subjectQuestions.size
            val sCorrect = subjectQuestions.count { q -> matchedAttempts[q.questionId]?.isCorrect == true }
            val sIncorrect = subjectQuestions.count { q -> matchedAttempts[q.questionId]?.let { !it.isCorrect } == true }

            SubjectAnalysis.create(
                subject = subject,
                totalQuestions = sTotal,
                correctAnswers = sCorrect,
                incorrectAnswers = sIncorrect
            )
        }

        // 3. Microtheme-wise analysis by subject
        val microthemesBySubject = mutableMapOf<String, List<MicrothemeAnalysis>>()
        for (subject in availableSubjects) {
            val microthemeNames = questionBankRepository.getAvailableMicrothemes(subject)
            val microthemeList = mutableListOf<MicrothemeAnalysis>()

            for (mtheme in microthemeNames) {
                val mthemeQuestions = questionBankRepository.getQuestionsForSubjectAndMicrotheme(subject, mtheme)
                // Strictly exclude microthemes with zero questions
                if (mthemeQuestions.isNotEmpty()) {
                    val mTotal = mthemeQuestions.size
                    val mCorrect = mthemeQuestions.count { q -> matchedAttempts[q.questionId]?.isCorrect == true }
                    val mIncorrect = mthemeQuestions.count { q -> matchedAttempts[q.questionId]?.let { !it.isCorrect } == true }

                    microthemeList.add(
                        MicrothemeAnalysis.create(
                            subject = subject,
                            microtheme = mtheme,
                            totalQuestions = mTotal,
                            correctAnswers = mCorrect,
                            incorrectAnswers = mIncorrect
                        )
                    )
                }
            }
            microthemesBySubject[subject] = microthemeList
        }

        val fullData = FullAnalysisData(
            overall = overall,
            subjects = subjects,
            microthemesBySubject = microthemesBySubject,
            unmatchedAttemptIds = unmatchedAttemptIds
        )

        // Strict mathematical consistency validation
        val validation = AnalysisValidator.validateFullAnalysis(fullData)
        check(validation.isValid) {
            "Analysis consistency validation failed: ${validation.errors.joinToString("; ")}"
        }

        return fullData
    }

    companion object {
        fun create(
            context: Context,
            questionBankRepository: QuestionBankRepository? = null,
            practiceHistoryRepository: PracticeHistoryRepository? = null
        ): AnalysisRepository {
            val qbRepo = questionBankRepository ?: QuestionBankRepositoryImpl.create(context)
            val phRepo = practiceHistoryRepository ?: PracticeHistoryRepositoryImpl(
                AppDatabase.getInstance(context).practiceDao()
            )
            return AnalysisRepositoryImpl(
                questionBankRepository = qbRepo,
                practiceHistoryRepository = phRepo
            )
        }
    }
}
