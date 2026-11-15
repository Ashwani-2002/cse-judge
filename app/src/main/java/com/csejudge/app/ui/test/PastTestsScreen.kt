package com.csejudge.app.ui.test

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.csejudge.app.data.local.TestEntity
import com.csejudge.app.data.model.TestType
import com.csejudge.app.ui.practice.PracticeColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen displaying the history of all completed tests.
 * Allows selecting any completed test to view its full report.
 */
@Composable
fun PastTestsScreen(
    viewModel: TestViewModel,
    onSelectTest: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pastTests by viewModel.pastTests.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = PracticeColors.ScreenBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PracticeColors.CardSurface,
                border = BorderStroke(1.dp, PracticeColors.BorderNeutral),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clickable(onClick = onBack)
                            .testTag("PastTestsBackButton"),
                        shape = RoundedCornerShape(8.dp),
                        color = PracticeColors.PrimaryNavyLight
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
                            text = "Past Tests",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = PracticeColors.PrimaryNavy
                        )
                        Text(
                            text = "${pastTests.size} completed ${if (pastTests.size == 1) "test" else "tests"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PracticeColors.TextSecondary
                        )
                    }
                }
            }

            if (pastTests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .testTag("PastTestsEmptyState"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = PracticeColors.PrimaryNavyLight,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "📋",
                                    fontSize = 28.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Completed Tests Yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PracticeColors.PrimaryNavy
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Take a Full Length or Half Length test to generate detailed performance reports and track your UPSC Prelims readiness.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PracticeColors.TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .testTag("PastTestsList"),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(pastTests, key = { it.testId }) { test ->
                        PastTestCard(
                            test = test,
                            onClick = { onSelectTest(test.testId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PastTestCard(
    test: TestEntity,
    onClick: () -> Unit
) {
    val testType = TestType.fromString(test.testType)
    val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(test.completedAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("PastTestCard_${test.testId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PracticeColors.CardSurface),
        border = BorderStroke(1.dp, PracticeColors.BorderNeutral),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PracticeColors.PrimaryNavyLight
                ) {
                    Text(
                        text = testType.title,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PracticeColors.PrimaryNavy,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = PracticeColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Score",
                        style = MaterialTheme.typography.labelSmall,
                        color = PracticeColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.2f", test.score)} / ${test.maxScore.toInt()}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = PracticeColors.PrimaryNavy
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Accuracy",
                        style = MaterialTheme.typography.labelSmall,
                        color = PracticeColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (test.attemptedCount > 0) {
                            String.format(Locale.US, "%.1f%%", test.accuracy)
                        } else {
                            "N/A"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = if (test.accuracy >= 60.0) PaletteGreen else PracticeColors.PrimaryNavy
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "✓ ${test.correctCount} Correct",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = PaletteGreen
                )
                Text(
                    text = "✗ ${test.incorrectCount} Wrong",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = PaletteRed
                )
                Text(
                    text = "• ${test.unansweredCount} Unanswered",
                    style = MaterialTheme.typography.bodySmall,
                    color = PracticeColors.TextSecondary
                )
            }
        }
    }
}
