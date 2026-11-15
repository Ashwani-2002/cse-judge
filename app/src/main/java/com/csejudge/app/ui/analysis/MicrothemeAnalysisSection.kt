package com.csejudge.app.ui.analysis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.csejudge.app.data.model.MicrothemeAnalysis
import com.csejudge.app.ui.practice.PracticeColors

/**
 * Section displaying microtheme-wise coverage and accuracy for a selected subject.
 * Includes a dynamic horizontal subject selector to switch between subjects.
 */
@Composable
fun MicrothemeAnalysisSection(
    availableSubjects: List<String>,
    selectedSubject: String?,
    microthemes: List<MicrothemeAnalysis>,
    onSelectSubject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("MicrothemeAnalysisSection")
    ) {
        // Section Title
        Text(
            text = "Microtheme Performance",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = PracticeColors.PrimaryNavy,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .testTag("MicrothemeSectionTitle")
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select a subject to inspect topic-level accuracy and question coverage:",
            style = MaterialTheme.typography.bodySmall,
            color = PracticeColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Subject Selector Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("SubjectSelectorRow")
        ) {
            availableSubjects.forEach { subject ->
                val isSelected = subject == selectedSubject
                SubjectFilterChip(
                    subject = subject,
                    isSelected = isSelected,
                    onClick = { onSelectSubject(subject) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Subject Subheading
        if (selectedSubject != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedSubject Microthemes",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = PracticeColors.PrimaryNavy,
                    modifier = Modifier.testTag("SelectedSubjectMicrothemesHeader")
                )

                Text(
                    text = "${microthemes.size} topics",
                    style = MaterialTheme.typography.bodySmall,
                    color = PracticeColors.TextSecondary,
                    modifier = Modifier.testTag("MicrothemesCountBadge")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // List of Microtheme Cards for the selected subject
        if (microthemes.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("EmptyMicrothemesState"),
                shape = RoundedCornerShape(8.dp),
                color = PracticeColors.CardSurface,
                border = BorderStroke(1.dp, PracticeColors.BorderNeutral)
            ) {
                Text(
                    text = "No microthemes available for this subject.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PracticeColors.TextSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            microthemes.forEach { mthemeAnalysis ->
                MicrothemeAnalysisCard(
                    microthemeAnalysis = mthemeAnalysis,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}

/**
 * Filter chip for subject selector.
 */
@Composable
fun SubjectFilterChip(
    subject: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) PracticeColors.PrimaryNavy else PracticeColors.CardSurface
    val textColor = if (isSelected) PracticeColors.OnPrimary else PracticeColors.TextPrimary
    val borderColor = if (isSelected) PracticeColors.PrimaryNavy else PracticeColors.BorderNeutral

    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("SubjectChip_$subject"),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = subject,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/**
 * Card displaying metrics for a specific microtheme.
 */
@Composable
fun MicrothemeAnalysisCard(
    microthemeAnalysis: MicrothemeAnalysis,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("MicrothemeCard_${microthemeAnalysis.microtheme}"),
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
            // Microtheme Name and Questions Count Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = microthemeAnalysis.microtheme,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = PracticeColors.PrimaryNavy,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .testTag("MicrothemeTitle_${microthemeAnalysis.microtheme}")
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PracticeColors.PrimaryNavyLight,
                    border = BorderStroke(1.dp, PracticeColors.BorderNeutral)
                ) {
                    Text(
                        text = "${microthemeAnalysis.totalQuestions} Qs",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = PracticeColors.PrimaryNavy,
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("MicrothemeTotalBadge_${microthemeAnalysis.microtheme}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Coverage Progress Bar
            CoverageProgressBar(
                attempted = microthemeAnalysis.attemptedQuestions,
                total = microthemeAnalysis.totalQuestions,
                coveragePercentage = microthemeAnalysis.coveragePercentage,
                unattempted = microthemeAnalysis.unattemptedQuestions
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = PracticeColors.BorderNeutral.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Accuracy Pie Chart
            AccuracyPieChart(
                correct = microthemeAnalysis.correctAnswers,
                incorrect = microthemeAnalysis.incorrectAnswers,
                accuracyPercentage = microthemeAnalysis.accuracyPercentage,
                chartSize = 72.dp,
                compact = true
            )
        }
    }
}
