package com.csejudge.app.ui.bookmark

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.csejudge.app.ui.practice.PracticeColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Bookmark star toggle button for questions.
 * - Displays an unselected star when question is not bookmarked.
 * - Displays a filled yellow star when question is bookmarked.
 * - Prevents multiple rapid taps while an operation is in progress (isLoading/debounce).
 * - Clickable touch target adheres to Material guidelines with minimum 36dp surface.
 */
@Composable
fun BookmarkStarButton(
    isBookmarked: Boolean,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = if (isBookmarked) "Bookmarked" else "Bookmark question"

    Surface(
        modifier = modifier
            .size(36.dp)
            .clickable(
                enabled = !isLoading,
                onClick = onClick
            )
            .semantics { contentDescription = description }
            .testTag("BookmarkStarButton"),
        shape = RoundedCornerShape(6.dp),
        color = if (isBookmarked) PracticeColors.BookmarkYellowLight else PracticeColors.CardSurface,
        border = BorderStroke(
            1.dp,
            if (isBookmarked) PracticeColors.BookmarkYellowBorder else PracticeColors.BorderNeutral
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            StarIcon(
                isBookmarked = isBookmarked,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Renders a crisp 5-pointed star icon.
 * Filled with gold/yellow when bookmarked; outlined with neutral color when unbookmarked.
 */
@Composable
fun StarIcon(
    isBookmarked: Boolean,
    modifier: Modifier = Modifier
) {
    val bookmarkYellow = PracticeColors.BookmarkYellow
    val bookmarkYellowBorder = PracticeColors.BookmarkYellowBorder
    val textSecondary = PracticeColors.TextSecondary

    Canvas(
        modifier = modifier.testTag("StarIconCanvas")
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val outerRadius = size.width / 2f * 0.95f
        val innerRadius = outerRadius * 0.42f

        val path = Path()
        for (i in 0 until 10) {
            val angle = (i * Math.PI / 5.0 - Math.PI / 2.0).toFloat()
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()

        if (isBookmarked) {
            drawPath(
                path = path,
                color = bookmarkYellow,
                style = Fill
            )
            drawPath(
                path = path,
                color = bookmarkYellowBorder,
                style = Stroke(width = 1.dp.toPx())
            )
        } else {
            drawPath(
                path = path,
                color = textSecondary,
                style = Stroke(width = 1.8.dp.toPx())
            )
        }
    }
}
