package com.csejudge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.csejudge.app.data.local.AppDatabase
import com.csejudge.app.data.repository.AnalysisRepositoryImpl
import com.csejudge.app.data.repository.BookmarkRepositoryImpl
import com.csejudge.app.data.repository.PracticeHistoryRepositoryImpl
import com.csejudge.app.data.repository.QuestionBankRepositoryImpl
import com.csejudge.app.data.repository.ThemeRepositoryImpl
import com.csejudge.app.navigation.AppNavHost
import com.csejudge.app.ui.analysis.AnalysisViewModel
import com.csejudge.app.ui.bookmark.BookmarkViewModel
import com.csejudge.app.ui.practice.CSEJudgeTheme
import com.csejudge.app.ui.practice.PracticeColors
import com.csejudge.app.ui.practice.PracticeViewModel
import com.csejudge.app.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val themeRepository = ThemeRepositoryImpl.create(applicationContext)
                val initialValue = runCatching {
                    kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                        kotlinx.coroutines.withTimeoutOrNull(500L) {
                            themeRepository.getInitialDarkMode()
                        }
                    }
                }.getOrNull() ?: false
                return ThemeViewModel(themeRepository, initialDarkMode = initialValue) as T
            }
        }
    }

    private val practiceViewModel: PracticeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val questionBankRepository = QuestionBankRepositoryImpl.getInstance(applicationContext)
                val database = AppDatabase.getInstance(applicationContext)
                val practiceHistoryRepository = PracticeHistoryRepositoryImpl(database.practiceDao())
                val bookmarkRepository = BookmarkRepositoryImpl(
                    bookmarkDao = database.bookmarkDao(),
                    questionBankRepository = questionBankRepository
                )
                return PracticeViewModel(
                    questionBankRepository = questionBankRepository,
                    practiceHistoryRepository = practiceHistoryRepository,
                    bookmarkRepository = bookmarkRepository
                ) as T
            }
        }
    }

    private val bookmarkViewModel: BookmarkViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val questionBankRepository = QuestionBankRepositoryImpl.getInstance(applicationContext)
                val database = AppDatabase.getInstance(applicationContext)
                val practiceHistoryRepository = PracticeHistoryRepositoryImpl(database.practiceDao())
                val bookmarkRepository = BookmarkRepositoryImpl(
                    bookmarkDao = database.bookmarkDao(),
                    questionBankRepository = questionBankRepository
                )
                return BookmarkViewModel(
                    questionBankRepository = questionBankRepository,
                    bookmarkRepository = bookmarkRepository,
                    practiceHistoryRepository = practiceHistoryRepository
                ) as T
            }
        }
    }

    private val analysisViewModel: AnalysisViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val questionBankRepository = QuestionBankRepositoryImpl.getInstance(applicationContext)
                val database = AppDatabase.getInstance(applicationContext)
                val practiceHistoryRepository = PracticeHistoryRepositoryImpl(database.practiceDao())
                val analysisRepository = AnalysisRepositoryImpl(
                    questionBankRepository = questionBankRepository,
                    practiceHistoryRepository = practiceHistoryRepository
                )
                return AnalysisViewModel(
                    analysisRepository = analysisRepository
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()

            CSEJudgeTheme(darkTheme = isDarkMode) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = PracticeColors.ScreenBackground
                ) { innerPadding ->
                    AppNavHost(
                        practiceViewModel = practiceViewModel,
                        bookmarkViewModel = bookmarkViewModel,
                        analysisViewModel = analysisViewModel,
                        isDarkMode = isDarkMode,
                        onToggleTheme = { themeViewModel.toggleTheme(isDarkMode) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
