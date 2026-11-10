package com.csejudge.app.ui.practice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Visual state for an option card.
 */
enum class OptionVisualState {
    DEFAULT,
    SELECTED_CORRECT,
    SELECTED_INCORRECT,
    UNSELECTED_CORRECT,
    DISABLED_NEUTRAL
}

/**
 * Rectangular option card for practice questions.
 * Handles letter badge (A/B/C/D), natural text wrapping, minimum 48dp touch target,
 * and distinct answer evaluation colors (green for correct, red for incorrect).
 */
@Composable
fun PracticeOptionCard(
    letter: String,
    text: String,
    visualState: OptionVisualState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor: Color
    val borderColor: Color
    val badgeBackgroundColor: Color
    val badgeTextColor: Color
    val textColor: Color

    when (visualState) {
        OptionVisualState.SELECTED_CORRECT -> {
            backgroundColor = PracticeColors.CorrectBackground
            borderColor = PracticeColors.CorrectBorder
            badgeBackgroundColor = PracticeColors.CorrectBorder
            badgeTextColor = Color.White
            textColor = PracticeColors.CorrectText
        }
        OptionVisualState.SELECTED_INCORRECT -> {
            backgroundColor = PracticeColors.IncorrectBackground
            borderColor = PracticeColors.IncorrectBorder
            badgeBackgroundColor = PracticeColors.IncorrectBorder
            badgeTextColor = Color.White
            textColor = PracticeColors.IncorrectText
        }
        OptionVisualState.UNSELECTED_CORRECT -> {
            backgroundColor = PracticeColors.CorrectBackground
            borderColor = PracticeColors.CorrectBorder
            badgeBackgroundColor = PracticeColors.CorrectBorder
            badgeTextColor = Color.White
            textColor = PracticeColors.CorrectText
        }
        OptionVisualState.DISABLED_NEUTRAL -> {
            backgroundColor = PracticeColors.CardSurface
            borderColor = PracticeColors.BorderNeutral
            badgeBackgroundColor = PracticeColors.BorderNeutral.copy(alpha = 0.5f)
            badgeTextColor = PracticeColors.TextSecondary
            textColor = PracticeColors.TextSecondary
        }
        OptionVisualState.DEFAULT -> {
            backgroundColor = PracticeColors.CardSurface
            borderColor = PracticeColors.BorderNeutral
            badgeBackgroundColor = PracticeColors.PrimaryNavyLight
            badgeTextColor = PracticeColors.PrimaryNavy
            textColor = PracticeColors.TextPrimary
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("OptionCard_$letter"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Letter Badge (A, B, C, D)
            Surface(
                modifier = Modifier.size(30.dp),
                shape = RoundedCornerShape(6.dp),
                color = badgeBackgroundColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = letter,
                        color = badgeTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Option Text (Natural wrapping, no clipping, no horizontal overflow)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = if (visualState == OptionVisualState.SELECTED_CORRECT ||
                        visualState == OptionVisualState.UNSELECTED_CORRECT ||
                        visualState == OptionVisualState.SELECTED_INCORRECT) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    }
                ),
                color = textColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
