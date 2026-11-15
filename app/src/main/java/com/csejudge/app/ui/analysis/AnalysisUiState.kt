package com.csejudge.app.ui.analysis

import com.csejudge.app.data.model.MicrothemeAnalysis
import com.csejudge.app.data.model.OverallAnalysis
import com.csejudge.app.data.model.SubjectAnalysis

/**
 * UI State for the Analysis data layer and screens.
 * Exposes overall, subject-wise, and microtheme-wise metrics.
 */
data class AnalysisUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val overall: OverallAnalysis? = null,
    val subjects: List<SubjectAnalysis> = emptyList(),
    val availableSubjects: List<String> = emptyList(),
    val selectedSubject: String? = null,
    val microthemes: List<MicrothemeAnalysis> = emptyList()
)
