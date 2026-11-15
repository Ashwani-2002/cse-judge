package com.csejudge.app.ui.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csejudge.app.ui.practice.PracticeColors
import java.util.Locale

/**
 * Answer Accuracy visualisation using a responsive 2-slice Pie/Donut Chart.
 *
 * Formula: Accuracy % = (Correct Answers / Attempted Questions) * 100
 *
 * Segments:
 * - Green: Correct answers
 * - Red: Incorrect answers
 *
 * If attempted == 0:
 * - Displays "Accuracy: N/A" with an empty-state indicator.
 * - Never displays a misleading 0% accuracy.
 */
@Composable
fun AccuracyPieChart(
    correct: Int,
    incorrect: Int,
    accuracyPercentage: Double?,
    modifier: Modifier = Modifier,
    chartSize: Dp = 88.dp,
    compact: Boolean = false
) {
    val totalAttempted = correct + incorrect
    val correctColor = PracticeColors.CorrectBorder
    val incorrectColor = PracticeColors.IncorrectBorder
    val neutralTrack = PracticeColors.BorderNeutral

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("AccuracyPieChart")
    ) {
        if (totalAttempted == 0 || accuracyPercentage == null) {
            // Zero-attempt empty state: Never display 0%, display N/A clearly
            AccuracyZeroAttemptState(compact = compact)
        } else {
            // Active attempts state with pie chart and legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Pie Chart Canvas with donut center
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(chartSize)
                        .testTag("AccuracyPieCanvasContainer")
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(chartSize)
                            .testTag("AccuracyPieCanvas")
                    ) {
                        val strokeWidth = size.minDimension * 0.22f
                        val arcSize = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                        val correctFraction = correct.toFloat() / totalAttempted.toFloat()
                        val correctSweep = correctFraction * 360f
                        val incorrectSweep = 360f - correctSweep

                        // Draw background track
                        drawArc(
                            color = neutralTrack,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )

                        // Draw Green segment (Correct)
                        if (correct > 0) {
                            drawArc(
                                color = correctColor,
                                startAngle = -90f,
                                sweepAngle = correctSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(arcSize, arcSize),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                        }

                        // Draw Red segment (Incorrect)
                        if (incorrect > 0) {
                            drawArc(
                                color = incorrectColor,
                                startAngle = -90f + correctSweep,
                                sweepAngle = incorrectSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(arcSize, arcSize),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                        }
                    }

                    // Prominent Accuracy Percentage in center
                    val formattedCenter = String.format(Locale.US, "%.1f%%", accuracyPercentage)
                    Text(
                        text = formattedCenter,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (compact) 11.sp else 13.sp
                        ),
                        color = PracticeColors.TextPrimary,
                        modifier = Modifier.testTag("AccuracyCenterPercentage")
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Accuracy Legend and Statistics
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    val fullAccuracyText = String.format(Locale.US, "Accuracy: %.2f%%", accuracyPercentage)
                    Text(
                        text = fullAccuracyText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (compact) 14.sp else 16.sp
                        ),
                        color = PracticeColors.PrimaryNavy,
                        modifier = Modifier.testTag("AccuracyProminentText")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Correct answers legend item
                    AccuracyLegendItem(
                        color = correctColor,
                        label = "Correct",
                        count = correct,
                        testTag = "AccuracyCorrectLegend"
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Incorrect answers legend item
                    AccuracyLegendItem(
                        color = incorrectColor,
                        label = "Incorrect",
                        count = incorrect,
                        testTag = "AccuracyIncorrectLegend"
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Total attempted denominator note
                    Text(
                        text = "$totalAttempted attempted questions",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = PracticeColors.TextSecondary,
                        modifier = Modifier.testTag("AccuracyDenominatorText")
                    )
                }
            }
        }
    }
}

/**
 * Legend item for Correct or Incorrect segment.
 */
@Composable
private fun AccuracyLegendItem(
    color: Color,
    label: String,
    count: Int,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: $count",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            ),
            color = PracticeColors.TextPrimary
        )
    }
}

/**
 * Empty state rendered when there are zero attempts.
 * Explicitly displays "Accuracy: N/A" and prevents misleading 0% figures.
 */
@Composable
private fun AccuracyZeroAttemptState(
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("AccuracyZeroAttempts"),
        shape = RoundedCornerShape(8.dp),
        color = PracticeColors.PrimaryNavyLight.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, PracticeColors.BorderNeutral)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Neutral Placeholder Ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(if (compact) 36.dp else 44.dp)
                    .border(
                        width = 2.dp,
                        color = PracticeColors.BorderNeutral,
                        shape = CircleShape
                    )
                    .testTag("AccuracyZeroAttemptsPlaceholder")
            ) {
                Text(
                    text = "N/A",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = PracticeColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Accuracy: N/A",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 13.sp else 15.sp
                    ),
                    color = PracticeColors.TextSecondary,
                    modifier = Modifier.testTag("AccuracyZeroAttemptsTitle")
                )
                Text(
                    text = "No questions attempted yet. Practice to see accuracy.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = PracticeColors.TextSecondary
                )
            }
        }
    }
}
