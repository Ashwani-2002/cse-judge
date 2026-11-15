package com.csejudge.app.ui.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csejudge.app.ui.practice.PracticeColors
import java.util.Locale

/**
 * Question Coverage visualisation using a horizontal progress bar.
 *
 * Formula: Coverage % = (Attempted Questions / Total Questions) * 100
 *
 * Displays:
 * - Horizontal progress bar.
 * - Coverage percentage formatted to 2 decimal places (e.g. "41.90%").
 * - Attempted count / total count (e.g. "600 / 1,432").
 * - Unattempted count (e.g. "Unattempted: 832").
 */
@Composable
fun CoverageProgressBar(
    attempted: Int,
    total: Int,
    coveragePercentage: Double,
    unattempted: Int = (total - attempted).coerceAtLeast(0),
    modifier: Modifier = Modifier
) {
    val progressFraction = if (total > 0) {
        (attempted.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val formattedPercentage = String.format(Locale.US, "%.2f%%", coveragePercentage)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("CoverageProgressBar")
    ) {
        // Label and Percentage Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Coverage",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                ),
                color = PracticeColors.TextSecondary
            )
            Text(
                text = formattedPercentage,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = PracticeColors.PrimaryNavy,
                modifier = Modifier.testTag("CoveragePercentageText")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Horizontal Progress Bar
        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .testTag("CoverageProgressIndicator"),
            color = PracticeColors.PrimaryNavy,
            trackColor = PracticeColors.PrimaryNavyLight
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Counts Row: Attempted vs Unattempted
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Attempted: $attempted / $total",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = PracticeColors.TextSecondary,
                modifier = Modifier.testTag("CoverageAttemptedText")
            )
            Text(
                text = "Unattempted: $unattempted",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = PracticeColors.TextSecondary,
                modifier = Modifier.testTag("CoverageUnattemptedText")
            )
        }
    }
}
