package com.csejudge.app.data.repository

import androidx.room.withTransaction
import com.csejudge.app.data.local.AppDatabase
import com.csejudge.app.data.local.AttemptedQuestionEntity
import com.csejudge.app.data.local.PracticeDao
import com.csejudge.app.data.local.TestDao
import com.csejudge.app.data.local.TestEntity
import com.csejudge.app.data.local.TestQuestionEntity
import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.SubjectTestPerformance
import com.csejudge.app.data.model.TestQuestionDetail
import com.csejudge.app.data.model.TestReport
import com.csejudge.app.data.model.TestType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

interface TestRepository {
    suspend fun saveCompletedTest(
        test: TestEntity,
        testQuestions: List<TestQuestionEntity>
    ): Result<Unit>

    fun observeCompletedTests(): Flow<List<TestEntity>>
    suspend fun getAllCompletedTests(): List<TestEntity>
    suspend fun getTestById(testId: String): TestEntity?
    suspend fun getTestReport(testId: String): TestReport?
}

class TestRepositoryImpl(
    private val database: AppDatabase,
    private val testDao: TestDao = database.testDao(),
    private val practiceDao: PracticeDao = database.practiceDao(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TestRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun saveCompletedTest(
        test: TestEntity,
        testQuestions: List<TestQuestionEntity>
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            database.withTransaction {
                // 1. Insert test summary metadata
                testDao.insertTest(test)

                // 2. Insert all test question snapshots
                testDao.insertTestQuestions(testQuestions)

                // 3. Persist answered questions into unified Practice attempt history
                for (q in testQuestions) {
                    val selected = q.selectedAnswer?.trim()?.uppercase()
                    if (!selected.isNullOrBlank()) {
                        val isCorrect = q.status == "CORRECT"
                        val attempt = AttemptedQuestionEntity(
                            questionId = q.questionId,
                            selectedAnswer = selected,
                            isCorrect = isCorrect,
                            attemptedAt = test.completedAt
                        )
                        practiceDao.upsertAttempt(attempt)
                    }
                }
            }
        }
    }

    override fun observeCompletedTests(): Flow<List<TestEntity>> {
        return testDao.observeCompletedTests().flowOn(ioDispatcher)
    }

    override suspend fun getAllCompletedTests(): List<TestEntity> = withContext(ioDispatcher) {
        testDao.getAllCompletedTests()
    }

    override suspend fun getTestById(testId: String): TestEntity? = withContext(ioDispatcher) {
        testDao.getTestById(testId)
    }

    override suspend fun getTestReport(testId: String): TestReport? = withContext(ioDispatcher) {
        val testEntity = testDao.getTestById(testId) ?: return@withContext null
        val questionEntities = testDao.getTestQuestions(testId)

        val questionDetails = questionEntities.map { entity ->
            val statements: List<String> = runCatching {
                json.decodeFromString<List<String>>(entity.questionStatementJson)
            }.getOrElse { listOf(entity.questionStatementJson) }

            val options: Options = runCatching {
                json.decodeFromString<Options>(entity.optionsJson)
            }.getOrElse { Options("", "", "", "") }

            val solutions: List<String> = runCatching {
                json.decodeFromString<List<String>>(entity.solutionJson)
            }.getOrElse { listOf(entity.solutionJson) }

            TestQuestionDetail(
                questionOrder = entity.questionOrder,
                questionId = entity.questionId,
                subject = entity.subject,
                microtheme = entity.microtheme,
                questionStatements = statements,
                options = options,
                correctAnswer = entity.correctAnswer,
                solutionStatements = solutions,
                selectedAnswer = entity.selectedAnswer,
                isMarkedForReview = entity.isMarkedForReview,
                status = entity.status
            )
        }

        // Calculate subject-wise performance
        val subjectMap = questionDetails.groupBy { it.subject }
        val subjectPerformanceList = subjectMap.map { (subject, questions) ->
            val total = questions.size
            val attempted = questions.count { !it.selectedAnswer.isNullOrBlank() }
            val correct = questions.count { it.status == "CORRECT" }
            val incorrect = questions.count { it.status == "INCORRECT" }
            val accuracy = if (attempted > 0) (correct.toDouble() / attempted.toDouble()) * 100.0 else null

            SubjectTestPerformance(
                subject = subject,
                totalQuestions = total,
                attemptedQuestions = attempted,
                correctAnswers = correct,
                incorrectAnswers = incorrect,
                accuracyPercentage = accuracy
            )
        }.sortedBy { it.subject }

        TestReport(
            testId = testEntity.testId,
            testType = TestType.fromString(testEntity.testType),
            startedAt = testEntity.startedAt,
            completedAt = testEntity.completedAt,
            durationSeconds = testEntity.durationSeconds,
            totalQuestions = testEntity.totalQuestions,
            attemptedCount = testEntity.attemptedCount,
            correctCount = testEntity.correctCount,
            incorrectCount = testEntity.incorrectCount,
            unansweredCount = testEntity.unansweredCount,
            score = testEntity.score,
            maxScore = testEntity.maxScore,
            accuracyPercentage = if (testEntity.attemptedCount > 0) testEntity.accuracy else null,
            subjectPerformanceList = subjectPerformanceList,
            questions = questionDetails
        )
    }
}
