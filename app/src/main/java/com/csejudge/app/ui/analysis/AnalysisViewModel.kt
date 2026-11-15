package com.csejudge.app.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.csejudge.app.data.repository.AnalysisRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the Analysis calculations and state.
 *
 * Exposes:
 * - Overall question coverage and accuracy
 * - Seven subject analysis records
 * - Available subjects list
 * - Selected subject
 * - Microtheme analysis records for selected subject
 * - Loading and error states
 */
class AnalysisViewModel(
    private val analysisRepository: AnalysisRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _selectedSubject = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(AnalysisUiState(isLoading = true))
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    init {
        observeAnalysisData()
    }

    private fun observeAnalysisData() {
        viewModelScope.launch(ioDispatcher) {
            combine(
                analysisRepository.observeFullAnalysis(),
                _selectedSubject
            ) { fullData, selectedSubject ->
                val availableSubjects = fullData.subjects.map { it.subject }
                val activeSubject = when {
                    selectedSubject != null && availableSubjects.contains(selectedSubject) -> selectedSubject
                    else -> availableSubjects.firstOrNull()
                }

                val microthemes = activeSubject?.let {
                    fullData.microthemesBySubject[it]
                }.orEmpty()

                AnalysisUiState(
                    isLoading = false,
                    errorMessage = null,
                    overall = fullData.overall,
                    subjects = fullData.subjects,
                    availableSubjects = availableSubjects,
                    selectedSubject = activeSubject,
                    microthemes = microthemes
                )
            }.catch { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load analysis data"
                    )
                }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    /**
     * Updates the selected subject for microtheme analysis breakdown.
     * Preserves existing attempt history and does not trigger question-bank reloads.
     */
    fun selectSubject(subject: String) {
        _selectedSubject.value = subject
    }
}
