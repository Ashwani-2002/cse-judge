package com.csejudge.app.data

import com.csejudge.app.data.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory test double for [ThemeRepository].
 */
class FakeThemeRepository(
    initialDarkMode: Boolean = false
) : ThemeRepository {

    private val _isDarkMode = MutableStateFlow(initialDarkMode)
    override val isDarkMode: Flow<Boolean> = _isDarkMode.asStateFlow()

    override suspend fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    override suspend fun getInitialDarkMode(): Boolean = _isDarkMode.value
}
