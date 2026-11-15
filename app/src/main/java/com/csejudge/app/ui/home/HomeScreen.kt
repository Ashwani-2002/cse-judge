package com.csejudge.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csejudge.app.ui.practice.PracticeColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Home screen for CSE Judge.
 * Presents preparation study modules, featuring the prominent "Practice problems" rectangular tile,
 * the "Bookmarked Questions" tile with reactive count and empty state, and a top-right Theme Toggle.
 */
@Composable
fun HomeScreen(
    onNavigateToPractice: () -> Unit,
    onNavigateToTests: () -> Unit = {},
    onNavigateToBookmarks: () -> Unit = {},
    onNavigateToAnalysis: () -> Unit = {},
    bookmarkCount: Int = 0,
    isDarkMode: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = PracticeColors.ScreenBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // App Branding Header with Theme Toggle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CSE Judge",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        color = PracticeColors.PrimaryNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "UPSC Civil Services Prelims Preparation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PracticeColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                ThemeToggleButton(
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Study Modules",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = PracticeColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Practice problems
            PracticeProblemsTile(
                onClick = onNavigateToPractice
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Tests
            TestsTile(
                onClick = onNavigateToTests
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Bookmarked Questions
            BookmarkedQuestionsTile(
                bookmarkCount = bookmarkCount,
                onClick = onNavigateToBookmarks
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Analysis
            AnalysisTile(
                onClick = onNavigateToAnalysis
            )
        }
    }
}

/**
 * Top-right theme switch toggle button with Sun / Moon icon.
 * - In Light Mode: Shows Moon icon indicating Dark Mode can be enabled.
 * - In Dark Mode: Shows Sun icon indicating Light Mode can be enabled.
 */
@Composable
fun ThemeToggleButton(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = if (isDarkMode) "Switch to light mode" else "Switch to dark mode"

    Surface(
        modifier = modifier
            .size(44.dp)
            .clickable(onClick = onToggleTheme)
            .semantics { contentDescription = description }
            .testTag("ThemeToggleButton"),
        shape = RoundedCornerShape(10.dp),
        color = PracticeColors.CardSurface,
        border = BorderStroke(1.dp, PracticeColors.BorderNeutral),
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isDarkMode) {
                SunIcon(
                    modifier = Modifier.size(22.dp)
                )
            } else {
                MoonIcon(
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Luminous Sun icon rendered on Canvas.
 * Displayed in Dark Mode to switch back to Light Mode.
 */
@Composable
fun SunIcon(
    modifier: Modifier = Modifier
) {
    val sunColor = PracticeColors.BookmarkYellow

    Canvas(
        modifier = modifier.testTag("SunIconCanvas")
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val centerRadius = size.minDimension * 0.23f

        // Central sun disc
        drawCircle(
            color = sunColor,
            radius = centerRadius,
            center = Offset(cx, cy)
        )

        // 8 radiating rays
        val rayInner = centerRadius + size.minDimension * 0.10f
        val rayOuter = rayInner + size.minDimension * 0.14f
        val strokeWidth = 1.8.dp.toPx()

        for (i in 0 until 8) {
            val angle = (i * Math.PI / 4.0).toFloat()
            val startX = cx + rayInner * cos(angle)
            val startY = cy + rayInner * sin(angle)
            val endX = cx + rayOuter * cos(angle)
            val endY = cy + rayOuter * sin(angle)

            drawLine(
                color = sunColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Crescent Moon icon rendered on Canvas.
 * Displayed in Light Mode to switch to Dark Mode.
 */
@Composable
fun MoonIcon(
    modifier: Modifier = Modifier
) {
    val moonColor = PracticeColors.PrimaryNavy

    Canvas(
        modifier = modifier.testTag("MoonIconCanvas")
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension / 2f

        val path = Path().apply {
            moveTo(cx + r * 0.25f, cy - r * 0.85f)
            // Outer round boundary
            cubicTo(
                cx - r * 0.95f, cy - r * 0.65f,
                cx - r * 0.95f, cy + r * 0.65f,
                cx + r * 0.25f, cy + r * 0.85f
            )
            // Inner crescent cutout
            cubicTo(
                cx - r * 0.35f, cy + r * 0.50f,
                cx - r * 0.35f, cy - r * 0.50f,
                cx + r * 0.25f, cy - r * 0.85f
            )
            close()
        }

        drawPath(
            path = path,
            color = moonColor,
            style = Fill
        )
    }
}

/**
 * Prominent rectangular tile for the Practice Problems feature.
 * The entire card surface is clickable, providing a comfortable touch target.
 */
@Composable
fun PracticeProblemsTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("PracticeProblemsTile"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PracticeColors.CardSurface),
        border = BorderStroke(1.dp, PracticeColors.BorderNeutral),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Practice problems",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = PracticeColors.PrimaryNavy
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Practice prelims questions by topic, microtheme, or across all subjects at random.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = PracticeColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Navigation indicator chevron
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = PracticeColors.PrimaryNavyLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "→",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PracticeColors.PrimaryNavy
                    )
                }
            }
        }
    }
}

/**
 * Prominent rectangular tile for the Tests feature.
 * Positioned as Tile 2 on the Home screen.
 */
@Composable
fun TestsTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("TestsTile"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PracticeColors.CardSurface),
        border = BorderStroke(1.dp, PracticeColors.BorderNeutral),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tests",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = PracticeColors.PrimaryNavy
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Take timed Full Length (100 Qs) or Half Length (50 Qs) tests and review past reports.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = PracticeColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Navigation indicator chevron
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = PracticeColors.PrimaryNavyLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "→",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PracticeColors.PrimaryNavy
                    )
                }
            }
        }
    }
}

/**
 * Prominent rectangular tile for the Bookmarked Questions feature.
 * Visually consistent with PracticeProblemsTile, displaying the reactive bookmark count and empty state.
 */
@Composable
fun BookmarkedQuestionsTile(
    bookmarkCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("BookmarkedQuestionsTile"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PracticeColors.CardSurface),
        border = BorderStroke(1.dp, PracticeColors.BorderNeutral),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (bookmarkCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PracticeColors.BookmarkYellowLight,
                        border = BorderStroke(1.dp, PracticeColors.BookmarkYellowBorder),
                        modifier = Modifier.testTag("BookmarkCountBadge")
                    ) {
                        Text(
                            text = "$bookmarkCount ${if (bookmarkCount == 1) "question" else "questions"} saved",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = PracticeColors.BookmarkYellowBorder,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Text(
                    text = "Bookmarked Questions",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = PracticeColors.PrimaryNavy
                )
                Spacer(modifier = Modifier.height(6.dp))
                val descriptionText = if (bookmarkCount > 0) {
                    "Review bookmarked questions organized by subject and microtheme."
                } else {
                    "No bookmarked questions yet. Tap the star button while practicing to bookmark questions for revision."
                }
                Text(
                    text = descriptionText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = PracticeColors.TextSecondary,
                    modifier = Modifier.testTag("BookmarkDescriptionText")
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Navigation indicator chevron
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = PracticeColors.PrimaryNavyLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "→",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PracticeColors.PrimaryNavy
                    )
                }
            }
        }
    }
}

/**
 * Prominent rectangular tile for the Analysis feature.
 * Visually consistent with PracticeProblemsTile and BookmarkedQuestionsTile.
 */
@Composable
fun AnalysisTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("AnalysisTile"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PracticeColors.CardSurface),
        border = BorderStroke(1.dp, PracticeColors.BorderNeutral),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Analysis",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = PracticeColors.PrimaryNavy
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Track your preparation and accuracy",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = PracticeColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Navigation indicator chevron
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = PracticeColors.PrimaryNavyLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "→",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PracticeColors.PrimaryNavy
                    )
                }
            }
        }
    }
}
