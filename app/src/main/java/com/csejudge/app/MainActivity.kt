package com.csejudge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.csejudge.app.data.local.AppDatabase
import com.csejudge.app.data.repository.PracticeHistoryRepositoryImpl
import com.csejudge.app.data.repository.QuestionBankRepositoryImpl
import com.csejudge.app.navigation.AppNavHost
import com.csejudge.app.ui.practice.PracticeColors
import com.csejudge.app.ui.practice.PracticeViewModel

class MainActivity : ComponentActivity() {

    private val practiceViewModel: PracticeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val questionBankRepository = QuestionBankRepositoryImpl.create(applicationContext)
                val database = AppDatabase.getInstance(applicationContext)
                val practiceHistoryRepository = PracticeHistoryRepositoryImpl(database.practiceDao())
                return PracticeViewModel(
                    questionBankRepository = questionBankRepository,
                    practiceHistoryRepository = practiceHistoryRepository
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CSEJudgeTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = PracticeColors.ScreenBackground
                ) { innerPadding ->
                    AppNavHost(
                        practiceViewModel = practiceViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun CSEJudgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            background = PracticeColors.ScreenBackground,
            onBackground = PracticeColors.TextPrimary,
            surface = PracticeColors.CardSurface,
            onSurface = PracticeColors.TextPrimary,
            primary = PracticeColors.PrimaryNavy,
            onPrimary = PracticeColors.OnPrimary
        ),
        content = content
    )
}
