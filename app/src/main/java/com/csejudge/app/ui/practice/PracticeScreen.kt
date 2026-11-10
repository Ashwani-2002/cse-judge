package com.csejudge.app.ui.practice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Top-level Practice destination integrating the Filter screen and Question practice screen.
 * Tracks screen transition between filter setup and active question practice.
 * Designed for direct integration with the app navigation graph in Stage 5.
 */
@Composable
fun PracticeScreen(
    viewModel: PracticeViewModel,
    modifier: Modifier = Modifier
) {
    var inActivePractice by rememberSaveable { mutableStateOf(false) }

    if (!inActivePractice) {
        PracticeFilterScreen(
            viewModel = viewModel,
            onStartPractice = { inActivePractice = true },
            modifier = modifier
        )
    } else {
        PracticeQuestionScreen(
            viewModel = viewModel,
            onBackToFilter = { inActivePractice = false },
            modifier = modifier
        )
    }
}
