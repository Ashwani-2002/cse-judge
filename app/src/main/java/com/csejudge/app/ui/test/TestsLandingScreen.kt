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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csejudge.app.data.model.TestType
import com.csejudge.app.ui.practice.PracticeColors

/**
 * Tests landing screen presenting three options:
 * 1. Full Length Test (100 questions, 120 minutes)
 * 2. Half Length Test (50 questions, 60 minutes)
 * 3. Past Tests (Completed test reports)
 */
@Composable
fun TestsLandingScreen(
    onSelectTestType: (TestType) -> Unit,
    onNavigateToPastTests: () -> Unit,
    onBack: () -> Unit,
    abortedMessage: String? = null,
    onDismissAbortedMessage: () -> Unit = {},
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
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onBack)
                        .testTag("TestsBackButton"),
                    shape = RoundedCornerShape(8.dp),
                    color = PracticeColors.CardSurface,
                    border = BorderStroke(1.dp, PracticeColors.BorderNeutral)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "←",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PracticeColors.PrimaryNavy
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Tests",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = PracticeColors.PrimaryNavy
                    )
                    Text(
                        text = "Timed prelims mock tests & past reports",
                        style = MaterialTheme.typography.bodySmall,
                        color = PracticeColors.TextSecondary
                    )
                }
            }

            // Aborted banner (if user was previously returned from an aborted test)
            if (!abortedMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("AbortedBanner"),
                    shape = RoundedCornerShape(8.dp),
                    color = PracticeColors.IncorrectBackground,
                    border = BorderStroke(1.dp, PracticeColors.IncorrectBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = abortedMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = PracticeColors.IncorrectText,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✕",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PracticeColors.IncorrectText,
                            modifier = Modifier
                                .clickable(onClick = onDismissAbortedMessage)
                                .padding(4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Available Test Formats",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = PracticeColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option 1: Full Length Test
            TestOptionCard(
                title = "Full Length Test",
                tag = "100 Qs • 120 Mins",
                description = "Comprehensive mock test following the exact UPSC Prelims pattern. 100 questions, 120 minutes, 200 marks (+2 / -2/3).",
                testTag = "FullLengthTestCard",
                onClick = { onSelectTestType(TestType.FULL_LENGTH) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Option 2: Half Length Test
            TestOptionCard(
                title = "Half Length Test",
                tag = "50 Qs • 60 Mins",
                description = "Focused timed practice across all subjects. 50 questions, 60 minutes, 100 marks (+2 / -2/3).",
                testTag = "HalfLengthTestCard",
                onClick = { onSelectTestType(TestType.HALF_LENGTH) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Historical Reports",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = PracticeColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option 3: Past Tests
            TestOptionCard(
                title = "Past Tests",
                tag = "History & Reports",
                description = "Review previously completed tests, subject-wise analytics, detailed explanations, and bookmark questions for revision.",
                testTag = "PastTestsCard",
                onClick = onNavigateToPastTests
            )
        }
    }
}

@Composable
fun TestOptionCard(
    title: String,
    tag: String,
    description: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
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
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PracticeColors.PrimaryNavyLight
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PracticeColors.PrimaryNavy,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = PracticeColors.PrimaryNavy
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    ),
                    color = PracticeColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

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
