package com.csejudge.app.ui.analysis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csejudge.app.ui.practice.PracticeColors

/**
 * Stateful Analysis Screen observing [AnalysisViewModel].
 */
@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    AnalysisContent(
        uiState = uiState,
        onSelectSubject = { subject -> viewModel.selectSubject(subject) },
        onBack = onBack,
        modifier = modifier
    )
}

/**
 * Stateless Analysis content composable for testing and presentation.
 */
@Composable
fun AnalysisContent(
    uiState: AnalysisUiState,
    onSelectSubject: (String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("AnalysisScreenSurface"),
        color = PracticeColors.ScreenBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Header with Back Navigation Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("AnalysisHeaderRow")
            ) {
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = onBack)
                        .testTag("AnalysisBackButton"),
                    shape = RoundedCornerShape(6.dp),
                    color = PracticeColors.CardSurface,
                    border = BorderStroke(1.dp, PracticeColors.BorderNeutral)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "←",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PracticeColors.PrimaryNavy
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Analysis",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = PracticeColors.PrimaryNavy,
                        modifier = Modifier.testTag("AnalysisScreenTitle")
                    )
                    Text(
                        text = "Track your preparation and accuracy",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = PracticeColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Body Content based on UI State
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("AnalysisLoadingIndicator"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = PracticeColors.PrimaryNavy,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Calculating analysis...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PracticeColors.TextSecondary
                            )
                        }
                    }
                }

                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("AnalysisErrorState"),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PracticeColors.CardSurface),
                            border = BorderStroke(1.dp, PracticeColors.BorderNeutral),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Error Loading Analysis",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PracticeColors.IncorrectBorder
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uiState.errorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PracticeColors.TextSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onRetry,
                                    colors = ButtonDefaults.buttonColors(containerColor = PracticeColors.PrimaryNavy)
                                ) {
                                    Text("Retry", color = PracticeColors.OnPrimary)
                                }
                            }
                        }
                    }
                }

                uiState.overall != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .testTag("AnalysisContentScrollable")
                    ) {
                        // Section 1: Overall Analysis
                        OverallAnalysisSection(overall = uiState.overall)

                        Spacer(modifier = Modifier.height(20.dp))

                        // Section 2: Subject-wise Performance (dynamically loaded subjects)
                        SubjectAnalysisSection(subjects = uiState.subjects)

                        Spacer(modifier = Modifier.height(20.dp))

                        // Section 3: Microtheme Deep Dive (with dynamic subject selector)
                        MicrothemeAnalysisSection(
                            availableSubjects = uiState.availableSubjects,
                            selectedSubject = uiState.selectedSubject,
                            microthemes = uiState.microthemes,
                            onSelectSubject = onSelectSubject
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
