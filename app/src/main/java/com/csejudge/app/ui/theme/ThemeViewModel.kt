package com.csejudge.app.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.csejudge.app.data.repository.ThemeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing central application-level theme state.
 * Observes theme preference from [ThemeRepository] and exposes a reactive [StateFlow].
 */
class ThemeViewModel(
    private val themeRepository: ThemeRepository,
    initialDarkMode: Boolean = false
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = themeRepository.isDarkMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = initialDarkMode
        )

    fun toggleTheme(currentDarkMode: Boolean) {
        viewModelScope.launch {
            themeRepository.setDarkMode(!currentDarkMode)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            themeRepository.setDarkMode(enabled)
        }
    }
}
