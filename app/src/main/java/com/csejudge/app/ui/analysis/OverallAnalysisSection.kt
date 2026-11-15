package com.csejudge.app.ui.analysis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csejudge.app.data.model.OverallAnalysis
import com.csejudge.app.ui.practice.PracticeColors

/**
 * Top section of the Analysis screen displaying application-wide metrics:
 * - Total questions across all subjects dynamically derived from question bank.
 * - Attempted and Unattempted counts.
 * - Question Coverage Progress Bar.
 * - Answer Accuracy Pie Chart (or N/A state).
 */
@Composable
fun OverallAnalysisSection(
    overall: OverallAnalysis,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("OverallAnalysisSection"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PracticeColors.CardSurface),
        border = BorderStroke(1.dp, PracticeColors.BorderNeutral),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Overall Performance",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = PracticeColors.PrimaryNavy,
                    modifier = Modifier.testTag("OverallSectionTitle")
                )

                // Total questions badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PracticeColors.PrimaryNavyLight,
                    border = BorderStroke(1.dp, PracticeColors.BorderNeutral)
                ) {
                    Text(
                        text = "${overall.totalQuestions} Questions",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = PracticeColors.PrimaryNavy,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("OverallTotalQuestionsBadge")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Question Coverage Visualisation
            CoverageProgressBar(
                attempted = overall.attemptedQuestions,
                total = overall.totalQuestions,
                coveragePercentage = overall.coveragePercentage,
                unattempted = overall.unattemptedQuestions
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = PracticeColors.BorderNeutral.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // 2. Answer Accuracy Visualisation
            AccuracyPieChart(
                correct = overall.correctAnswers,
                incorrect = overall.incorrectAnswers,
                accuracyPercentage = overall.accuracyPercentage,
                chartSize = 92.dp
            )
        }
    }
}
