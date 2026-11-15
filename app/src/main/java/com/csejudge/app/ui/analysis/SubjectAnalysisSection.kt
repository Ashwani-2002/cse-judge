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
import com.csejudge.app.data.model.SubjectAnalysis
import com.csejudge.app.ui.practice.PracticeColors

/**
 * Section displaying question coverage and answer accuracy for all available subjects.
 */
@Composable
fun SubjectAnalysisSection(
    subjects: List<SubjectAnalysis>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("SubjectAnalysisSection")
    ) {
        Text(
            text = "Subject-wise Performance",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = PracticeColors.PrimaryNavy,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .testTag("SubjectSectionTitle")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Render card for each of the seven subjects
        subjects.forEach { subjectAnalysis ->
            SubjectAnalysisCard(
                subjectAnalysis = subjectAnalysis,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

/**
 * Individual card displaying coverage and accuracy for a specific subject.
 */
@Composable
fun SubjectAnalysisCard(
    subjectAnalysis: SubjectAnalysis,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("SubjectCard_${subjectAnalysis.subject}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = PracticeColors.CardSurface),
        border = BorderStroke(1.dp, PracticeColors.BorderNeutral),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Subject Title and Questions Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subjectAnalysis.subject,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = PracticeColors.PrimaryNavy,
                    modifier = Modifier.testTag("SubjectTitle_${subjectAnalysis.subject}")
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PracticeColors.PrimaryNavyLight,
                    border = BorderStroke(1.dp, PracticeColors.BorderNeutral)
                ) {
                    Text(
                        text = "${subjectAnalysis.totalQuestions} Questions",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = PracticeColors.PrimaryNavy,
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("SubjectTotalBadge_${subjectAnalysis.subject}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Coverage Progress Bar
            CoverageProgressBar(
                attempted = subjectAnalysis.attemptedQuestions,
                total = subjectAnalysis.totalQuestions,
                coveragePercentage = subjectAnalysis.coveragePercentage,
                unattempted = subjectAnalysis.unattemptedQuestions
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = PracticeColors.BorderNeutral.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Accuracy Pie Chart
            AccuracyPieChart(
                correct = subjectAnalysis.correctAnswers,
                incorrect = subjectAnalysis.incorrectAnswers,
                accuracyPercentage = subjectAnalysis.accuracyPercentage,
                chartSize = 76.dp,
                compact = true
            )
        }
    }
}
