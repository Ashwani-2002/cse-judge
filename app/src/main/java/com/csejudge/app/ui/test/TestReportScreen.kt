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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csejudge.app.data.model.SubjectTestPerformance
import com.csejudge.app.data.model.TestQuestionDetail
import com.csejudge.app.data.model.TestReport
import com.csejudge.app.ui.ai.AiExplanationButton
import com.csejudge.app.ui.bookmark.BookmarkStarButton
import com.csejudge.app.ui.practice.PracticeColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Report screen presented after test completion and accessible via Past Tests.
 * Features two tabs:
 * 1. Overall: Summary statistics, final score, accuracy, and subject breakdown.
 * 2. Question Wise: Sequential review of all questions with answers, explanations, and bookmark toggling.
 */
@Composable
fun TestReportScreen(
    testId: String,
    viewModel: TestViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedQuestionIds.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(testId) {
        if (uiState.currentReport?.testId != testId) {
            viewModel.loadTestReport(testId)
        }
    }

    val report = uiState.currentReport

    Surface(
        modifier = modifier.fillMaxSize(),
        color = PracticeColors.ScreenBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
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
                            .testTag("ReportBackButton"),
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
                            text = report?.testType?.title ?: "Test Report",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            ),
                            color = PracticeColors.PrimaryNavy
                        )
                        if (report != null) {
                            val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
                                .format(Date(report.completedAt))
                            Text(
                                text = "Completed on $formattedDate",
                                style = MaterialTheme.typography.bodySmall,
                                color = PracticeColors.TextSecondary
                            )
                        }
                    }
                }
            }

            if (report == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PracticeColors.PrimaryNavy,
                        strokeWidth = 3.dp
                    )
                }
            } else {
                // Two Tabs: Overall and Question Wise
                SecondaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = PracticeColors.CardSurface,
                    contentColor = PracticeColors.PrimaryNavy,
                    modifier = Modifier.testTag("ReportTabRow")
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Text(
                                text = "Overall",
                                fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("OverallTab")
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Text(
                                text = "Question Wise",
                                fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("QuestionWiseTab")
                    )
                }

                when (selectedTabIndex) {
                    0 -> OverallTabContent(report = report)
                    1 -> QuestionWiseTabContent(
                        questions = report.questions,
                        bookmarkedIds = bookmarkedIds,
                        onToggleBookmark = { qId -> viewModel.toggleBookmark(qId) }
                    )
                }
            }
        }
    }
}

@Composable
fun OverallTabContent(report: TestReport) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .testTag("OverallTabContent")
    ) {
        // Score & Accuracy Highlights Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ScoreHighlightCard"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PracticeColors.CardSurface),
            border = BorderStroke(1.dp, PracticeColors.BorderNeutral),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Marks",
                            style = MaterialTheme.typography.labelMedium,
                            color = PracticeColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.US, "%.2f", report.score),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp
                                ),
                                color = PracticeColors.PrimaryNavy,
                                modifier = Modifier.testTag("ReportTotalMarksText")
                            )
                            Text(
                                text = " / ${report.maxScore.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                color = PracticeColors.TextSecondary,
                                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Accuracy",
                            style = MaterialTheme.typography.labelMedium,
                            color = PracticeColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = report.accuracyPercentage?.let {
                                String.format(Locale.US, "%.1f%%", it)
                            } ?: "N/A",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            ),
                            color = if ((report.accuracyPercentage ?: 0.0) >= 60.0) {
                                PaletteGreen
                            } else {
                                PracticeColors.PrimaryNavy
                            },
                            modifier = Modifier.testTag("ReportAccuracyText")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4 Stat Badges: Attempted, Correct, Incorrect, Unanswered
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBadge(label = "Attempted", value = "${report.attemptedCount} / ${report.totalQuestions}", color = PracticeColors.PrimaryNavy)
                    StatBadge(label = "Correct", value = "${report.correctCount}", color = PaletteGreen)
                    StatBadge(label = "Incorrect", value = "${report.incorrectCount}", color = PaletteRed)
                    StatBadge(label = "Unanswered", value = "${report.unansweredCount}", color = PracticeColors.TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Subject-wise Performance Section
        Text(
            text = "Subject Performance",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = PracticeColors.PrimaryNavy
        )

        Spacer(modifier = Modifier.height(12.dp))

        report.subjectPerformanceList.forEach { perf ->
            SubjectPerformanceCard(performance = perf)
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun StatBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = PracticeColors.TextSecondary
        )
    }
}

@Composable
fun SubjectPerformanceCard(performance: SubjectTestPerformance) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("SubjectPerformanceCard_${performance.subject}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = PracticeColors.CardSurface),
        border = BorderStroke(1.dp, PracticeColors.BorderNeutral)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = performance.subject,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = PracticeColors.PrimaryNavy
                )
                Text(
                    text = performance.accuracyPercentage?.let {
                        String.format(Locale.US, "%.1f%% Accuracy", it)
                    } ?: "N/A",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (performance.accuracyPercentage != null && performance.accuracyPercentage >= 60.0) {
                        PaletteGreen
                    } else {
                        PracticeColors.TextSecondary
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total: ${performance.totalQuestions}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PracticeColors.TextSecondary
                )
                Text(
                    text = "Attempted: ${performance.attemptedQuestions}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PracticeColors.TextSecondary
                )
                Text(
                    text = "Correct: ${performance.correctAnswers}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = PaletteGreen
                )
                Text(
                    text = "Wrong: ${performance.incorrectAnswers}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = PaletteRed
                )
            }
        }
    }
}

@Composable
fun QuestionWiseTabContent(
    questions: List<TestQuestionDetail>,
    bookmarkedIds: Set<String>,
    onToggleBookmark: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("QuestionWiseTabContent"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(questions, key = { it.questionOrder }) { item ->
            ReportQuestionCard(
                detail = item,
                isBookmarked = item.questionId in bookmarkedIds,
                onToggleBookmark = { onToggleBookmark(item.questionId) }
            )
        }
    }
}

@Composable
fun ReportQuestionCard(
    detail: TestQuestionDetail,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ReportQuestionCard_${detail.questionOrder}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PracticeColors.CardSurface),
        border = BorderStroke(1.dp, PracticeColors.BorderNeutral),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Question number + Status chip + Bookmark Star button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Q ${detail.questionOrder}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PracticeColors.PrimaryNavy
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    val (statusText, statusBg, statusColor) = when (detail.status) {
                        "CORRECT" -> Triple("Correct (+2)", PracticeColors.CorrectBackground, PracticeColors.CorrectText)
                        "INCORRECT" -> Triple("Incorrect (-2/3)", PracticeColors.IncorrectBackground, PracticeColors.IncorrectText)
                        else -> Triple("Unanswered (0)", PracticeColors.SolutionBackground, PracticeColors.TextSecondary)
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusBg,
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                BookmarkStarButton(
                    isBookmarked = isBookmarked,
                    onClick = onToggleBookmark
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subject & Microtheme
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = PracticeColors.PrimaryNavyLight
                ) {
                    Text(
                        text = detail.subject,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PracticeColors.PrimaryNavy,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "• ${detail.microtheme}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PracticeColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Question Statement Lines
            detail.questionStatements.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.5.sp,
                        lineHeight = 21.sp
                    ),
                    color = PracticeColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Options with evaluated styling
            val options = listOf(
                "A" to detail.options.a,
                "B" to detail.options.b,
                "C" to detail.options.c,
                "D" to detail.options.d
            )

            options.forEach { (letter, text) ->
                ReportOptionRow(
                    letter = letter,
                    text = text,
                    isCorrectOption = letter.equals(detail.correctAnswer.trim(), ignoreCase = true),
                    isSelectedOption = letter.equals(detail.selectedAnswer?.trim(), ignoreCase = true)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Solution & Explanation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = PracticeColors.SolutionBackground),
                border = BorderStroke(1.dp, PracticeColors.SolutionBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Explanation",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = PracticeColors.PrimaryNavy
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    detail.solutionStatements.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            ),
                            color = PracticeColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AiExplanationButton(detail = detail)
        }
    }
}

@Composable
fun ReportOptionRow(
    letter: String,
    text: String,
    isCorrectOption: Boolean,
    isSelectedOption: Boolean
) {
    val (bg, border, textColor) = when {
        isSelectedOption && isCorrectOption -> Triple(PracticeColors.CorrectBackground, PracticeColors.CorrectBorder, PracticeColors.CorrectText)
        isSelectedOption && !isCorrectOption -> Triple(PracticeColors.IncorrectBackground, PracticeColors.IncorrectBorder, PracticeColors.IncorrectText)
        isCorrectOption -> Triple(PracticeColors.CorrectBackground, PracticeColors.CorrectBorder, PracticeColors.CorrectText)
        else -> Triple(PracticeColors.CardSurface, PracticeColors.BorderNeutral, PracticeColors.TextPrimary)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(26.dp),
                shape = RoundedCornerShape(6.dp),
                color = if (isCorrectOption) PracticeColors.CorrectBorder else if (isSelectedOption) PracticeColors.IncorrectBorder else PracticeColors.PrimaryNavyLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = letter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCorrectOption || isSelectedOption) Color.White else PracticeColors.PrimaryNavy
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                ),
                color = textColor,
                modifier = Modifier.weight(1f)
            )

            if (isSelectedOption) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isCorrectOption) "✓ Your Answer" else "✗ Your Answer",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = if (isCorrectOption) PracticeColors.CorrectText else PracticeColors.IncorrectText
                )
            } else if (isCorrectOption) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "✓ Correct",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = PracticeColors.CorrectText
                )
            }
        }
    }
}
