package com.csejudge.app.ui.bookmark

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.csejudge.app.data.local.AttemptedQuestionEntity
import com.csejudge.app.data.local.BookmarkedQuestionEntity
import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.repository.BookmarkRepository
import com.csejudge.app.data.repository.PracticeHistoryRepository
import com.csejudge.app.data.repository.QuestionBankRepository
import com.csejudge.app.data.repository.QuestionBankStats
import com.csejudge.app.navigation.AppNavHost
import com.csejudge.app.navigation.Screen
import com.csejudge.app.ui.practice.PracticeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BookmarkFlowInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController
    private lateinit var fakeQuestionBankRepo: FakeQuestionBankRepository
    private lateinit var fakeBookmarkRepo: FakeBookmarkRepository
    private lateinit var fakeHistoryRepo: FakePracticeHistoryRepository
    private lateinit var practiceViewModel: PracticeViewModel
    private lateinit var bookmarkViewModel: BookmarkViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        navController = TestNavHostController(context)
        navController.navigatorProvider.addNavigator(ComposeNavigator())

        fakeQuestionBankRepo = FakeQuestionBankRepository()
        fakeBookmarkRepo = FakeBookmarkRepository()
        fakeHistoryRepo = FakePracticeHistoryRepository()

        practiceViewModel = PracticeViewModel(
            questionBankRepository = fakeQuestionBankRepo,
            practiceHistoryRepository = fakeHistoryRepo,
            bookmarkRepository = fakeBookmarkRepo,
            ioDispatcher = Dispatchers.Main
        )

        bookmarkViewModel = BookmarkViewModel(
            questionBankRepository = fakeQuestionBankRepo,
            bookmarkRepository = fakeBookmarkRepo,
            practiceHistoryRepository = fakeHistoryRepo,
            ioDispatcher = Dispatchers.Main
        )
    }

    @Test
    fun testEmptyBookmarkFlowShowsEmptyStateAndNavigatesToPractice() {
        composeTestRule.setContent {
            AppNavHost(
                practiceViewModel = practiceViewModel,
                bookmarkViewModel = bookmarkViewModel,
                navController = navController
            )
        }
        composeTestRule.waitForIdle()

        // 1. Home screen displays 0 bookmarks
        composeTestRule.onNodeWithTag("BookmarkedQuestionsTile").performScrollTo().assertIsDisplayed().performClick()

        // 2. Navigates to BookmarkSubjects
        assertEquals(Screen.BookmarkSubjects.route, navController.currentBackStackEntry?.destination?.route)
        composeTestRule.onNodeWithTag("BookmarkEmptyStateView").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Bookmarked Questions").assertIsDisplayed()

        // 3. Tapping "Go to Practice" navigates to PracticeFilter
        composeTestRule.onNodeWithTag("GoToPracticeButton").performClick()
        assertEquals(Screen.PracticeFilter.route, navController.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun testCompleteBookmarkNavigationHierarchyAndBackNavigation() {
        runBlocking {
            fakeBookmarkRepo.addBookmark("POL00001")
            fakeBookmarkRepo.addBookmark("POL00002")
        }

        composeTestRule.setContent {
            AppNavHost(
                practiceViewModel = practiceViewModel,
                bookmarkViewModel = bookmarkViewModel,
                navController = navController
            )
        }
        composeTestRule.waitForIdle()

        // 1. Home screen displays tile with count 2
        composeTestRule.onNodeWithTag("BookmarkedQuestionsTile").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("BookmarkCountBadge", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("2 questions saved", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("BookmarkedQuestionsTile").performClick()

        // 2. BookmarkSubjects screen shows Polity (2 questions)
        assertEquals(Screen.BookmarkSubjects.route, navController.currentBackStackEntry?.destination?.route)
        composeTestRule.onNodeWithTag("BookmarkSubjectItem_Polity").assertIsDisplayed().performClick()

        // 3. BookmarkMicrothemes screen shows Preamble (1 question) and Fundamental Rights (1 question)
        assertTrue(navController.currentBackStackEntry?.destination?.route?.startsWith("bookmark_microthemes/") == true)
        composeTestRule.onNodeWithTag("BookmarkMicrothemeItem_Preamble").assertIsDisplayed()
        composeTestRule.onNodeWithTag("BookmarkMicrothemeItem_Fundamental Rights").assertIsDisplayed()

        // Select Preamble
        composeTestRule.onNodeWithTag("BookmarkMicrothemeItem_Preamble").performClick()

        // 4. BookmarkQuestions screen shows POL00001
        assertTrue(navController.currentBackStackEntry?.destination?.route?.startsWith("bookmark_questions/") == true)
        composeTestRule.onNodeWithTag("BookmarkHeaderSection").assertIsDisplayed()
        composeTestRule.onNodeWithTag("QuestionStatementsSection").assertIsDisplayed()
        composeTestRule.onNodeWithTag("BookmarkStarButton").assertIsDisplayed()

        // 5. Back navigation from Questions to Microthemes
        composeTestRule.onNodeWithTag("BookmarkBackButton").performClick()
        assertTrue(navController.currentBackStackEntry?.destination?.route?.startsWith("bookmark_microthemes/") == true)

        // 6. Back navigation from Microthemes to Subjects
        composeTestRule.onNodeWithTag("BookmarkMicrothemesBackButton").performClick()
        assertEquals(Screen.BookmarkSubjects.route, navController.currentBackStackEntry?.destination?.route)

        // 7. Back navigation from Subjects to Home
        composeTestRule.onNodeWithTag("BookmarkSubjectsBackButton").performClick()
        assertEquals(Screen.Home.route, navController.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun testPreviouslyAttemptedQuestionShowsAttemptAndSupportsReattempt() {
        // Pre-attempt POL00001 with answer "B" (incorrect; correct is "A") and bookmark it
        runBlocking {
            fakeHistoryRepo.recordAttempt("POL00001", "B", false)
            fakeBookmarkRepo.addBookmark("POL00001")
        }

        composeTestRule.setContent {
            AppNavHost(
                practiceViewModel = practiceViewModel,
                bookmarkViewModel = bookmarkViewModel,
                navController = navController
            )
        }
        composeTestRule.waitForIdle()

        // Navigate to POL00001 in Preamble
        composeTestRule.onNodeWithTag("BookmarkedQuestionsTile").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("BookmarkSubjectItem_Polity").performClick()
        composeTestRule.onNodeWithTag("BookmarkMicrothemeItem_Preamble").performClick()

        // 1. Verify previous attempt: Solution is displayed
        composeTestRule.onNodeWithTag("SolutionSection").performScrollTo().assertIsDisplayed()

        // 2. Re-attempt: Click option "A" (the correct answer)
        composeTestRule.onNodeWithTag("OptionCard_A").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // 3. Verify attempt is updated in repository (upserted without duplicates)
        val attempt = fakeHistoryRepo.attempts["POL00001"]
        assertNotNull(attempt)
        assertEquals("A", attempt?.selectedAnswer)
        assertEquals(true, attempt?.isCorrect)
        assertEquals(1, fakeHistoryRepo.attempts.size)
    }

    @Test
    fun testBookmarkToggleOnQuestionScreenPreservesAttempt() {
        runBlocking {
            fakeHistoryRepo.recordAttempt("POL00001", "A", true)
            fakeBookmarkRepo.addBookmark("POL00001")
        }

        composeTestRule.setContent {
            AppNavHost(
                practiceViewModel = practiceViewModel,
                bookmarkViewModel = bookmarkViewModel,
                navController = navController
            )
        }
        composeTestRule.waitForIdle()

        // Open bookmarked question
        composeTestRule.onNodeWithTag("BookmarkedQuestionsTile").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("BookmarkSubjectItem_Polity").performClick()
        composeTestRule.onNodeWithTag("BookmarkMicrothemeItem_Preamble").performClick()

        // Unbookmark via star button
        composeTestRule.onNodeWithTag("BookmarkStarButton").performClick()
        composeTestRule.waitForIdle()

        // Verify bookmark removed in repo
        assertEquals(false, runBlocking { fakeBookmarkRepo.isBookmarked("POL00001") })

        // Verify attempt STILL EXISTS in repo!
        val attempt = fakeHistoryRepo.attempts["POL00001"]
        assertNotNull(attempt)
        assertEquals("A", attempt?.selectedAnswer)
    }

    // ==========================================
    // Test Fakes
    // ==========================================

    private class FakeQuestionBankRepository : QuestionBankRepository {
        override var isLoaded: Boolean = true

        private val allQuestions = listOf(
            createTestQuestion("POL00001", "Polity", "Preamble", "A"),
            createTestQuestion("POL00002", "Polity", "Fundamental Rights", "B"),
            createTestQuestion("ECO00001", "Economy", "Banking", "C")
        )

        private fun createTestQuestion(id: String, subject: String, microtheme: String, correct: String): Question {
            return Question(
                questionId = id,
                subject = subject,
                microtheme = microtheme,
                question = listOf("Statement 1 for $id", "Statement 2 for $id"),
                options = Options("Option A", "Option B", "Option C", "Option D"),
                correctAnswer = correct,
                solution = listOf("Solution for $id")
            )
        }

        override suspend fun loadQuestionBank(): QuestionBankStats {
            isLoaded = true
            return QuestionBankStats(allQuestions.size, emptyMap(), emptyMap(), 0)
        }

        override fun getAllQuestions(): List<Question> = allQuestions
        override fun getQuestionById(questionId: String): Question? = allQuestions.find { it.questionId == questionId }
        override fun getAvailableSubjects(): List<String> = allQuestions.map { it.subject }.distinct()
        override fun getAvailableMicrothemes(subject: String): List<String> =
            allQuestions.filter { it.subject == subject }.map { it.microtheme }.distinct()
        override fun getQuestionsForSubject(subject: String): List<Question> =
            allQuestions.filter { it.subject == subject }
        override fun getQuestionsForSubjectAndMicrotheme(subject: String, microtheme: String): List<Question> =
            allQuestions.filter { it.subject == subject && it.microtheme == microtheme }
    }

    private class FakeBookmarkRepository : BookmarkRepository {
        private val bookmarks = mutableMapOf<String, Long>()
        private val countFlow = MutableStateFlow(0)
        private val idsFlow = MutableStateFlow<List<String>>(emptyList())

        override suspend fun addBookmark(questionId: String, timestamp: Long): Result<Unit> {
            bookmarks[questionId] = timestamp
            notifyFlows()
            return Result.success(Unit)
        }

        override suspend fun removeBookmark(questionId: String): Result<Unit> {
            bookmarks.remove(questionId)
            notifyFlows()
            return Result.success(Unit)
        }

        override suspend fun isBookmarked(questionId: String): Boolean = bookmarks.containsKey(questionId)
        override suspend fun getAllBookmarkedQuestionIds(): List<String> = bookmarks.keys.toList()
        override fun observeAllBookmarkedQuestionIds(): Flow<List<String>> = idsFlow.asStateFlow()
        override suspend fun getAllBookmarks(): List<BookmarkedQuestionEntity> =
            bookmarks.map { BookmarkedQuestionEntity(it.key, it.value) }
        override fun observeAllBookmarks(): Flow<List<BookmarkedQuestionEntity>> =
            MutableStateFlow(bookmarks.map { BookmarkedQuestionEntity(it.key, it.value) })
        override suspend fun getBookmarkCount(): Int = bookmarks.size
        override fun observeBookmarkCount(): Flow<Int> = countFlow.asStateFlow()

        private fun notifyFlows() {
            countFlow.value = bookmarks.size
            idsFlow.value = bookmarks.keys.toList()
        }
    }

    private class FakePracticeHistoryRepository : PracticeHistoryRepository {
        val attempts = mutableMapOf<String, AttemptedQuestionEntity>()
        private val attemptedIdsFlow = MutableStateFlow<List<String>>(emptyList())
        private val countFlow = MutableStateFlow(0)
        private val attemptsFlow = MutableStateFlow<List<AttemptedQuestionEntity>>(emptyList())

        override suspend fun recordAttempt(
            questionId: String,
            selectedAnswer: String,
            isCorrect: Boolean
        ): Result<Unit> {
            val entity = AttemptedQuestionEntity(
                questionId = questionId,
                selectedAnswer = selectedAnswer,
                isCorrect = isCorrect,
                attemptedAt = System.currentTimeMillis()
            )
            attempts[questionId] = entity
            notifyFlows()
            return Result.success(Unit)
        }

        override suspend fun recordOrUpdateAttempt(
            questionId: String,
            selectedAnswer: String,
            isCorrect: Boolean,
            attemptedAt: Long
        ): Result<Unit> {
            val entity = AttemptedQuestionEntity(
                questionId = questionId,
                selectedAnswer = selectedAnswer,
                isCorrect = isCorrect,
                attemptedAt = attemptedAt
            )
            attempts[questionId] = entity
            notifyFlows()
            return Result.success(Unit)
        }

        override suspend fun isQuestionAttempted(questionId: String): Boolean = attempts.containsKey(questionId)
        override suspend fun getAllAttemptedQuestionIds(): List<String> = attempts.keys.toList()
        override fun observeAllAttemptedQuestionIds(): Flow<List<String>> = attemptedIdsFlow.asStateFlow()
        override suspend fun getAttempt(questionId: String): AttemptedQuestionEntity? = attempts[questionId]
        override suspend fun getAttemptCount(): Int = attempts.size
        override fun observeAttemptCount(): Flow<Int> = countFlow.asStateFlow()
        override suspend fun getAllAttempts(): List<AttemptedQuestionEntity> = attempts.values.toList()
        override fun observeAllAttempts(): Flow<List<AttemptedQuestionEntity>> = attemptsFlow.asStateFlow()

        private fun notifyFlows() {
            attemptedIdsFlow.value = attempts.keys.toList()
            countFlow.value = attempts.size
            attemptsFlow.value = attempts.values.toList()
        }
    }
}
