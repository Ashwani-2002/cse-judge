package com.csejudge.app.ui.analysis

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.csejudge.app.data.local.AttemptedQuestionEntity
import com.csejudge.app.data.model.FullAnalysisData
import com.csejudge.app.data.model.MicrothemeAnalysis
import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.OverallAnalysis
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.model.SubjectAnalysis
import com.csejudge.app.data.repository.AnalysisRepository
import com.csejudge.app.data.repository.AnalysisRepositoryImpl
import com.csejudge.app.data.repository.PracticeHistoryRepository
import com.csejudge.app.data.repository.QuestionBankRepository
import com.csejudge.app.data.repository.QuestionBankStats
import com.csejudge.app.ui.practice.CSEJudgeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnalysisScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var fakeQuestionBankRepo: FakeAnalysisQuestionBankRepo
    private lateinit var fakeHistoryRepo: FakeAnalysisHistoryRepo
    private lateinit var analysisRepository: AnalysisRepository
    private lateinit var viewModel: AnalysisViewModel

    @Before
    fun setUp() {
        fakeQuestionBankRepo = FakeAnalysisQuestionBankRepo()
        fakeHistoryRepo = FakeAnalysisHistoryRepo()
        analysisRepository = AnalysisRepositoryImpl(
            questionBankRepository = fakeQuestionBankRepo,
            practiceHistoryRepository = fakeHistoryRepo,
            ioDispatcher = Dispatchers.Main
        )
        viewModel = AnalysisViewModel(analysisRepository, ioDispatcher = Dispatchers.Main)
    }

    private fun waitForAnalysisLoaded() {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            !viewModel.uiState.value.isLoading
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun testOverallAnalysisDisplaysZeroAttemptNAState() {
        // No attempts in history
        composeTestRule.setContent {
            CSEJudgeTheme(darkTheme = false) {
                AnalysisScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }
        waitForAnalysisLoaded()

        // Header and overall section
        composeTestRule.onNodeWithTag("AnalysisScreenTitle").assertIsDisplayed()
        composeTestRule.onNodeWithTag("OverallSectionTitle").assertIsDisplayed()
        composeTestRule.onNodeWithText("1432 Questions").assertIsDisplayed()

        // Coverage with 0 attempts: 0.00%
        composeTestRule.onAllNodesWithText("0.00%")[0].assertIsDisplayed()
        composeTestRule.onNodeWithText("Attempted: 0 / 1432").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unattempted: 1432").assertIsDisplayed()

        // Zero-attempt accuracy displays N/A (never misleading 0%)
        composeTestRule.onAllNodesWithTag("AccuracyZeroAttempts")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Accuracy: N/A")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("No questions attempted yet. Practice to see accuracy.")[0].assertIsDisplayed()
    }

    @Test
    fun testOverallAnalysisWithAttemptsDisplaysPercentagesAndPieChart() {
        // Record attempts matching fake repository question IDs: 3 correct, 1 incorrect
        runBlocking {
            fakeHistoryRepo.recordAttempt("POL1", "A", true)
            fakeHistoryRepo.recordAttempt("POL2", "B", true)
            fakeHistoryRepo.recordAttempt("ECO1", "C", true)
            fakeHistoryRepo.recordAttempt("MH1", "WRONG", false)
        }

        val testViewModel = AnalysisViewModel(analysisRepository, ioDispatcher = Dispatchers.Main)

        composeTestRule.setContent {
            CSEJudgeTheme(darkTheme = false) {
                AnalysisScreen(
                    viewModel = testViewModel,
                    onBack = {}
                )
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            !testViewModel.uiState.value.isLoading
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("OverallSectionTitle").assertIsDisplayed()

        // Coverage: 4 attempted out of 1432
        composeTestRule.onNodeWithText("Attempted: 4 / 1432").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unattempted: 1428").assertIsDisplayed()

        // Accuracy: 3 / 4 = 75.00%
        composeTestRule.onNodeWithText("Accuracy: 75.00%").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Correct: 3")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Incorrect: 1")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("4 attempted questions")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("AccuracyPieCanvas", useUnmergedTree = true)[0].assertIsDisplayed()
    }

    @Test
    fun testAllSevenSubjectsAppearInSubjectSection() {
        composeTestRule.setContent {
            CSEJudgeTheme(darkTheme = false) {
                AnalysisScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }
        waitForAnalysisLoaded()

        val expectedSubjects = listOf(
            "AMAC" to 149,
            "Economy" to 243,
            "Environment" to 248,
            "Geography" to 233,
            "Modern History" to 127,
            "Polity" to 252,
            "Sci and Tech" to 180
        )

        composeTestRule.onNodeWithTag("SubjectSectionTitle").performScrollTo().assertIsDisplayed()

        for ((subject, totalQuestions) in expectedSubjects) {
            composeTestRule.onNodeWithTag("SubjectCard_$subject").performScrollTo().assertIsDisplayed()
            composeTestRule.onNodeWithTag("SubjectTitle_$subject", useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithTag("SubjectTotalBadge_$subject", useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithText("$totalQuestions Questions").assertIsDisplayed()
        }
    }

    @Test
    fun testSelectingSubjectUpdatesMicrothemeBreakdown() {
        composeTestRule.setContent {
            CSEJudgeTheme(darkTheme = false) {
                AnalysisScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }
        waitForAnalysisLoaded()

        // Default selected subject is AMAC
        composeTestRule.onNodeWithTag("MicrothemeSectionTitle").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("SelectedSubjectMicrothemesHeader").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("AMAC Microthemes").assertIsDisplayed()
        composeTestRule.onNodeWithTag("MicrothemeCard_Buddhist and Jain Art Forms").performScrollTo().assertIsDisplayed()

        // Tap Economy chip
        composeTestRule.onNodeWithTag("SubjectChip_Economy").performScrollTo().performClick()
        waitForAnalysisLoaded()

        // Header updates to Economy Microthemes
        composeTestRule.onNodeWithText("Economy Microthemes").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("MicrothemeCard_Macro economy").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("5 Qs").performScrollTo().assertIsDisplayed()

        // Tap Modern History chip
        composeTestRule.onNodeWithTag("SubjectChip_Modern History").performScrollTo().performClick()
        waitForAnalysisLoaded()

        // Modern History must show "From 1857 to 1884 CE"
        composeTestRule.onNodeWithText("Modern History Microthemes").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("MicrothemeCard_From 1857 to 1884 CE").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun testLightModeStyling() {
        composeTestRule.setContent {
            CSEJudgeTheme(darkTheme = false) {
                AnalysisScreen(viewModel = viewModel, onBack = {})
            }
        }
        waitForAnalysisLoaded()
        composeTestRule.onNodeWithTag("AnalysisScreenSurface").assertIsDisplayed()
    }

    @Test
    fun testDarkModeStyling() {
        composeTestRule.setContent {
            CSEJudgeTheme(darkTheme = true) {
                AnalysisScreen(viewModel = viewModel, onBack = {})
            }
        }
        waitForAnalysisLoaded()
        composeTestRule.onNodeWithTag("AnalysisScreenSurface").assertIsDisplayed()
    }

    @Test
    fun testBackButtonTriggersCallback() {
        var backClicked = false

        composeTestRule.setContent {
            CSEJudgeTheme(darkTheme = false) {
                AnalysisScreen(
                    viewModel = viewModel,
                    onBack = { backClicked = true }
                )
            }
        }
        waitForAnalysisLoaded()

        composeTestRule.onNodeWithTag("AnalysisBackButton").performClick()
        assertTrue(backClicked)
    }

    // ==========================================
    // Test Fakes
    // ==========================================

    private class FakeAnalysisQuestionBankRepo : QuestionBankRepository {
        override var isLoaded: Boolean = true

        private val subjects = listOf("AMAC", "Economy", "Environment", "Geography", "Modern History", "Polity", "Sci and Tech")
        private val subjectTotals = mapOf(
            "AMAC" to 149,
            "Economy" to 243,
            "Environment" to 248,
            "Geography" to 233,
            "Modern History" to 127,
            "Polity" to 252,
            "Sci and Tech" to 180
        )

        private val allQuestions = buildList {
            // AMAC: 149 questions (2 microthemes: 2 questions and 147 questions)
            for (i in 1..2) add(createQ("AMAC$i", "AMAC", "Buddhist and Jain Art Forms"))
            for (i in 3..149) add(createQ("AMAC$i", "AMAC", "Mughal Period"))

            // Economy: 243 questions (includes Macro economy: 5)
            for (i in 1..5) add(createQ("ECO$i", "Economy", "Macro economy"))
            for (i in 6..243) add(createQ("ECO$i", "Economy", "Government Schemes"))

            // Environment: 248
            for (i in 1..248) add(createQ("ENV$i", "Environment", "Biodiversity in India"))

            // Geography: 233
            for (i in 1..233) add(createQ("GEO$i", "Geography", "Geomorphic Processes and Landforms"))

            // Modern History: 127 (includes From 1857 to 1884 CE: 5)
            for (i in 1..5) add(createQ("MH$i", "Modern History", "From 1857 to 1884 CE"))
            for (i in 6..127) add(createQ("MH$i", "Modern History", "From 1919 to 1939 CE"))

            // Polity: 252
            for (i in 1..25) add(createQ("POL$i", "Polity", "Fundamental Rights"))
            for (i in 26..252) add(createQ("POL$i", "Polity", "Historical Background & Colonial Rule"))

            // Sci and Tech: 180
            for (i in 1..180) add(createQ("SCI$i", "Sci and Tech", "Astronomy and Space"))
        }

        private fun createQ(id: String, subject: String, microtheme: String): Question {
            return Question(
                questionId = id,
                subject = subject,
                microtheme = microtheme,
                question = listOf("Statement 1 for $id"),
                options = Options("Option A", "Option B", "Option C", "Option D"),
                correctAnswer = "A",
                solution = listOf("Solution for $id")
            )
        }

        override suspend fun loadQuestionBank(): QuestionBankStats {
            isLoaded = true
            return QuestionBankStats(allQuestions.size, emptyMap(), emptyMap(), 0)
        }

        override fun getAllQuestions(): List<Question> = allQuestions
        override fun getQuestionById(questionId: String): Question? = allQuestions.find { it.questionId == questionId }
        override fun getAvailableSubjects(): List<String> = subjects
        override fun getAvailableMicrothemes(subject: String): List<String> =
            allQuestions.filter { it.subject == subject }.map { it.microtheme }.distinct()
        override fun getQuestionsForSubject(subject: String): List<Question> =
            allQuestions.filter { it.subject == subject }
        override fun getQuestionsForSubjectAndMicrotheme(subject: String, microtheme: String): List<Question> =
            allQuestions.filter { it.subject == subject && it.microtheme == microtheme }
    }

    private class FakeAnalysisHistoryRepo : PracticeHistoryRepository {
        val attempts = mutableMapOf<String, AttemptedQuestionEntity>()
        private val attemptsFlow = MutableStateFlow<List<AttemptedQuestionEntity>>(emptyList())

        override suspend fun recordAttempt(
            questionId: String,
            selectedAnswer: String,
            isCorrect: Boolean
        ): Result<Unit> {
            val entity = AttemptedQuestionEntity(questionId, selectedAnswer, isCorrect)
            attempts[questionId] = entity
            attemptsFlow.value = attempts.values.toList()
            return Result.success(Unit)
        }

        override suspend fun recordOrUpdateAttempt(
            questionId: String,
            selectedAnswer: String,
            isCorrect: Boolean,
            attemptedAt: Long
        ): Result<Unit> {
            val entity = AttemptedQuestionEntity(questionId, selectedAnswer, isCorrect, attemptedAt)
            attempts[questionId] = entity
            attemptsFlow.value = attempts.values.toList()
            return Result.success(Unit)
        }

        override suspend fun isQuestionAttempted(questionId: String) = attempts.containsKey(questionId)
        override suspend fun getAllAttemptedQuestionIds() = attempts.keys.toList()
        override fun observeAllAttemptedQuestionIds(): Flow<List<String>> = flowOf(attempts.keys.toList())
        override suspend fun getAttempt(questionId: String) = attempts[questionId]
        override suspend fun getAttemptCount() = attempts.size
        override fun observeAttemptCount(): Flow<Int> = flowOf(attempts.size)
        override suspend fun getAllAttempts(): List<AttemptedQuestionEntity> = attempts.values.toList()
        override fun observeAllAttempts(): Flow<List<AttemptedQuestionEntity>> = attemptsFlow.asStateFlow()
    }
}
