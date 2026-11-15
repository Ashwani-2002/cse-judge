package com.csejudge.app.ui.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csejudge.app.ai.AiExplanationLauncher
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.model.TestQuestionDetail
import com.csejudge.app.ui.practice.PracticeColors

/**
 * Reusable AI Explanation button conforming to Material 3 design guidelines.
 * Displays "AI explanation" with a dual-sparkle AI icon.
 * Dynamically adapts to light and dark themes using [PracticeColors].
 */
@Composable
fun AiExplanationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = PracticeColors.PrimaryNavyLight.copy(alpha = 0.45f),
            contentColor = PracticeColors.PrimaryNavy
        ),
        border = BorderStroke(1.dp, PracticeColors.PrimaryNavy.copy(alpha = 0.35f)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("AiExplanationButton")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AiSparklesIcon(
                tint = PracticeColors.PrimaryNavy,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AI explanation",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = PracticeColors.PrimaryNavy
            )
        }
    }
}

/**
 * Convenience overload for [Question] in Practice or Bookmark screens.
 */
@Composable
fun AiExplanationButton(
    question: Question,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AiExplanationButton(
        onClick = { AiExplanationLauncher.launch(context, question) },
        modifier = modifier
    )
}

/**
 * Convenience overload for [TestQuestionDetail] in Test Report screen.
 */
@Composable
fun AiExplanationButton(
    detail: TestQuestionDetail,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AiExplanationButton(
        onClick = { AiExplanationLauncher.launch(context, detail) },
        modifier = modifier
    )
}

/**
 * Canvas-rendered dual-sparkle AI icon.
 * Features a primary 4-point astroid star and a smaller companion star.
 */
@Composable
fun AiSparklesIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.testTag("AiSparklesIcon")
    ) {
        val w = size.width
        val h = size.height

        // 1. Primary Sparkle Star (Center-Left)
        val cx1 = w * 0.40f
        val cy1 = h * 0.52f
        val r1 = w * 0.38f

        val path1 = Path().apply {
            moveTo(cx1, cy1 - r1)
            quadraticTo(cx1, cy1, cx1 + r1, cy1)
            quadraticTo(cx1, cy1, cx1, cy1 + r1)
            quadraticTo(cx1, cy1, cx1 - r1, cy1)
            quadraticTo(cx1, cy1, cx1, cy1 - r1)
            close()
        }
        drawPath(path = path1, color = tint, style = Fill)

        // 2. Secondary Sparkle Star (Upper-Right)
        val cx2 = w * 0.78f
        val cy2 = h * 0.28f
        val r2 = w * 0.20f

        val path2 = Path().apply {
            moveTo(cx2, cy2 - r2)
            quadraticTo(cx2, cy2, cx2 + r2, cy2)
            quadraticTo(cx2, cy2, cx2, cy2 + r2)
            quadraticTo(cx2, cy2, cx2 - r2, cy2)
            quadraticTo(cx2, cy2, cx2, cy2 - r2)
            close()
        }
        drawPath(path = path2, color = tint, style = Fill)
    }
}
