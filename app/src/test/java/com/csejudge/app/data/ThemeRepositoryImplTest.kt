package com.csejudge.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.csejudge.app.data.repository.ThemeRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun freshPreferenceStoreDefaultsToLightMode() = runTest {
        val testFile = tempFolder.newFile("test_theme_fresh.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        val repository = ThemeRepositoryImpl(dataStore)

        assertFalse(repository.isDarkMode.first())
        assertFalse(repository.getInitialDarkMode())
    }

    @Test
    fun setDarkModeTrueUpdatesFlowAndInitialValue() = runTest {
        val testFile = tempFolder.newFile("test_theme_dark.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        val repository = ThemeRepositoryImpl(dataStore)

        assertFalse(repository.isDarkMode.first())

        repository.setDarkMode(true)

        assertTrue(repository.isDarkMode.first())
        assertTrue(repository.getInitialDarkMode())
    }

    @Test
    fun setDarkModeFalseRestoresLightMode() = runTest {
        val testFile = tempFolder.newFile("test_theme_toggle.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        val repository = ThemeRepositoryImpl(dataStore)

        repository.setDarkMode(true)
        assertTrue(repository.isDarkMode.first())

        repository.setDarkMode(false)
        assertFalse(repository.isDarkMode.first())
        assertFalse(repository.getInitialDarkMode())
    }

    @Test
    fun preferenceSurvivesDataStoreRecreation() = runTest {
        val testFile = tempFolder.newFile("test_theme_persistence.preferences_pb")

        // First session: enable dark mode and persist
        val job1 = Job()
        val scope1 = CoroutineScope(Dispatchers.IO + job1)
        val dataStore1 = PreferenceDataStoreFactory.create(
            scope = scope1,
            produceFile = { testFile }
        )
        val repository1 = ThemeRepositoryImpl(dataStore1)
        repository1.setDarkMode(true)
        assertTrue(repository1.isDarkMode.first())

        // Wait for first session job to cancel and finish completely
        job1.cancelAndJoin()

        // Simulate app restart / new process: reopen same file with fresh scope
        val job2 = Job()
        val scope2 = CoroutineScope(Dispatchers.IO + job2)
        val dataStore2 = PreferenceDataStoreFactory.create(
            scope = scope2,
            produceFile = { testFile }
        )
        val repository2 = ThemeRepositoryImpl(dataStore2)

        assertTrue(repository2.isDarkMode.first())
        assertTrue(repository2.getInitialDarkMode())
        job2.cancelAndJoin()
    }
}
