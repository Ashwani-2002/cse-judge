package com.csejudge.app.ui.test

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.window.Dialog
import com.csejudge.app.ui.practice.PracticeColors

val PalettePurple = Color(0xFF7C3AED)
val PaletteRed = Color(0xFFDC2626)
val PaletteGreen = Color(0xFF16A34A)

/**
 * Question palette dialog displaying a 5-column grid of numbered circular indicators.
 * FLT: 100 questions (20 rows).
 * HLT: 50 questions (10 rows).
 *
 * Color Semantics:
 * - Red: Unanswered and not marked for review.
 * - Purple: Marked for review.
 * - Green: Answered and not marked for review.
 */
@Composable
fun TestPaletteDialog(
    totalQuestions: Int,
    currentQuestionIndex: Int,
    getPaletteState: (Int) -> PaletteQuestionState,
    onSelectQuestion: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .testTag("QuestionPaletteDialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PracticeColors.CardSurface),
            border = BorderStroke(1.dp, PracticeColors.BorderNeutral),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question Palette",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = PracticeColors.PrimaryNavy
                    )

                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(onClick = onDismiss)
                            .testTag("PaletteCloseButton"),
                        shape = RoundedCornerShape(8.dp),
                        color = PracticeColors.PrimaryNavyLight
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "✕",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PracticeColors.PrimaryNavy
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Legend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PracticeColors.SolutionBackground, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PaletteLegendItem(color = PaletteRed, label = "Unanswered")
                    PaletteLegendItem(color = PalettePurple, label = "Review")
                    PaletteLegendItem(color = PaletteGreen, label = "Answered")
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5-column grid of numbered circles
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("PaletteGrid"),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(totalQuestions) { index ->
                        val questionNumber = index + 1
                        val state = getPaletteState(index)
                        val isCurrent = index == currentQuestionIndex

                        val circleColor = when (state) {
                            PaletteQuestionState.MARKED_FOR_REVIEW_PURPLE -> PalettePurple
                            PaletteQuestionState.ANSWERED_GREEN -> PaletteGreen
                            PaletteQuestionState.UNANSWERED_RED -> PaletteRed
                        }

                        val borderStroke = if (isCurrent) {
                            BorderStroke(3.dp, PracticeColors.PrimaryNavy)
                        } else null

                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { onSelectQuestion(index) }
                                .testTag("PaletteItem_$questionNumber"),
                            shape = CircleShape,
                            color = circleColor,
                            border = borderStroke,
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$questionNumber",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaletteLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = PracticeColors.TextPrimary
        )
    }
}
