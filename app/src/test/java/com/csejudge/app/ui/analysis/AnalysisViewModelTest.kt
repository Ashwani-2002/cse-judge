package com.csejudge.app.ui.analysis

import com.csejudge.app.data.local.AttemptedQuestionEntity
import com.csejudge.app.data.model.FullAnalysisData
import com.csejudge.app.data.model.MicrothemeAnalysis
import com.csejudge.app.data.model.OverallAnalysis
import com.csejudge.app.data.model.SubjectAnalysis
import com.csejudge.app.data.repository.AnalysisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeTestAnalysisRepository
    private lateinit var viewModel: AnalysisViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTestAnalysisRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadingAndSuccessfulStateEmission() = runTest(testDispatcher) {
        viewModel = AnalysisViewModel(fakeRepository, ioDispatcher = testDispatcher)

        // Before emission settles
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNotNull(state.overall)
        assertEquals(1432, state.overall?.totalQuestions)
        assertEquals(7, state.subjects.size)
        assertEquals(listOf("AMAC", "Economy", "Environment", "Geography", "Modern History", "Polity", "Sci and Tech"), state.availableSubjects)
        assertEquals("AMAC", state.selectedSubject)
        assertEquals(2, state.microthemes.size)
    }

    @Test
    fun testSelectSubjectUpdatesMicrothemesWithoutAffectingTotals() = runTest(testDispatcher) {
        viewModel = AnalysisViewModel(fakeRepository, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        assertEquals("AMAC", viewModel.uiState.value.selectedSubject)
        assertEquals(2, viewModel.uiState.value.microthemes.size)

        viewModel.selectSubject("Economy")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Economy", state.selectedSubject)
        assertEquals(1, state.microthemes.size)
        assertEquals("Macro economy", state.microthemes.first().microtheme)
        // Totals must remain identical
        assertEquals(1432, state.overall?.totalQuestions)
        assertEquals(7, state.subjects.size)
    }

    @Test
    fun testReactiveUpdatesReflectInViewModelState() = runTest(testDispatcher) {
        viewModel = AnalysisViewModel(fakeRepository, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.overall?.attemptedQuestions)

        // Simulate new attempt in repository
        val updatedOverall = OverallAnalysis.create(1432, 1, 0)
        val updatedSubjects = fakeRepository.currentData.subjects.map {
            if (it.subject == "Polity") SubjectAnalysis.create("Polity", 252, 1, 0) else it
        }
        val updatedFull = fakeRepository.currentData.copy(
            overall = updatedOverall,
            subjects = updatedSubjects
        )
        fakeRepository.emit(updatedFull)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.overall?.attemptedQuestions)
        assertEquals(1, state.overall?.correctAnswers)
        val polity = state.subjects.first { it.subject == "Polity" }
        assertEquals(1, polity.attemptedQuestions)
        assertEquals(1, polity.correctAnswers)
    }

    @Test
    fun testRepositoryFailureEmitsErrorMessage() = runTest(testDispatcher) {
        val failingRepo = object : AnalysisRepository {
            override fun observeFullAnalysis(): Flow<FullAnalysisData> = kotlinx.coroutines.flow.flow {
                throw IllegalStateException("Database corruption error")
            }
            override fun observeOverallAnalysis(): Flow<OverallAnalysis> = throw NotImplementedError()
            override fun observeSubjectAnalysisList(): Flow<List<SubjectAnalysis>> = throw NotImplementedError()
            override fun observeMicrothemeAnalysisList(subject: String): Flow<List<MicrothemeAnalysis>> = throw NotImplementedError()
            override suspend fun getFullAnalysis(): FullAnalysisData = throw NotImplementedError()
            override suspend fun getOverallAnalysis(): OverallAnalysis = throw NotImplementedError()
            override suspend fun getSubjectAnalysisList(): List<SubjectAnalysis> = throw NotImplementedError()
            override suspend fun getMicrothemeAnalysisList(subject: String): List<MicrothemeAnalysis> = throw NotImplementedError()
        }

        viewModel = AnalysisViewModel(failingRepo, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Database corruption error", state.errorMessage)
    }

    private class FakeTestAnalysisRepository : AnalysisRepository {
        var currentData = createSampleData()
        private val flow = MutableStateFlow(currentData)

        fun emit(data: FullAnalysisData) {
            currentData = data
            flow.value = data
        }

        override fun observeFullAnalysis(): Flow<FullAnalysisData> = flow.asStateFlow()
        override fun observeOverallAnalysis(): Flow<OverallAnalysis> = throw NotImplementedError()
        override fun observeSubjectAnalysisList(): Flow<List<SubjectAnalysis>> = throw NotImplementedError()
        override fun observeMicrothemeAnalysisList(subject: String): Flow<List<MicrothemeAnalysis>> = throw NotImplementedError()
        override suspend fun getFullAnalysis(): FullAnalysisData = currentData
        override suspend fun getOverallAnalysis(): OverallAnalysis = currentData.overall
        override suspend fun getSubjectAnalysisList(): List<SubjectAnalysis> = currentData.subjects
        override suspend fun getMicrothemeAnalysisList(subject: String): List<MicrothemeAnalysis> = currentData.microthemesBySubject[subject].orEmpty()

        private companion object {
            fun createSampleData(): FullAnalysisData {
                val overall = OverallAnalysis.create(1432, 0, 0)
                val subjects = listOf(
                    SubjectAnalysis.create("AMAC", 149, 0, 0),
                    SubjectAnalysis.create("Economy", 243, 0, 0),
                    SubjectAnalysis.create("Environment", 248, 0, 0),
                    SubjectAnalysis.create("Geography", 233, 0, 0),
                    SubjectAnalysis.create("Modern History", 127, 0, 0),
                    SubjectAnalysis.create("Polity", 252, 0, 0),
                    SubjectAnalysis.create("Sci and Tech", 180, 0, 0)
                )
                val microthemes = mapOf(
                    "AMAC" to listOf(
                        MicrothemeAnalysis.create("AMAC", "M1", 100, 0, 0),
                        MicrothemeAnalysis.create("AMAC", "M2", 49, 0, 0)
                    ),
                    "Economy" to listOf(
                        MicrothemeAnalysis.create("Economy", "Macro economy", 243, 0, 0)
                    )
                )
                return FullAnalysisData(
                    overall = overall,
                    subjects = subjects,
                    microthemesBySubject = microthemes
                )
            }
        }
    }
}
