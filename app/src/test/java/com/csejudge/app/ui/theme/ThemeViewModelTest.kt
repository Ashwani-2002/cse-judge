package com.csejudge.app.ui.theme

import com.csejudge.app.data.FakeThemeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeThemeRepository: FakeThemeRepository
    private lateinit var viewModel: ThemeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeThemeRepository = FakeThemeRepository(initialDarkMode = false)
        viewModel = ThemeViewModel(fakeThemeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultThemeIsLightMode() = runTest {
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.isDarkMode.collect() }
        advanceUntilIdle()
        assertFalse(viewModel.isDarkMode.value)
        job.cancel()
    }

    @Test
    fun toggleThemeSwitchesLightToDark() = runTest {
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.isDarkMode.collect() }
        advanceUntilIdle()
        assertFalse(viewModel.isDarkMode.value)

        viewModel.toggleTheme(currentDarkMode = false)
        advanceUntilIdle()

        assertTrue(viewModel.isDarkMode.value)
        job.cancel()
    }

    @Test
    fun toggleThemeSwitchesDarkToLight() = runTest {
        fakeThemeRepository.setDarkMode(true)
        val vm = ThemeViewModel(fakeThemeRepository)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.isDarkMode.collect() }
        advanceUntilIdle()
        assertTrue(vm.isDarkMode.value)

        vm.toggleTheme(currentDarkMode = true)
        advanceUntilIdle()

        assertFalse(vm.isDarkMode.value)
        job.cancel()
    }

    @Test
    fun setDarkModeExplicitlyUpdatesState() = runTest {
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.isDarkMode.collect() }
        advanceUntilIdle()
        assertFalse(viewModel.isDarkMode.value)

        viewModel.setDarkMode(true)
        advanceUntilIdle()
        assertTrue(viewModel.isDarkMode.value)

        viewModel.setDarkMode(false)
        advanceUntilIdle()
        assertFalse(viewModel.isDarkMode.value)
        job.cancel()
    }
}
