package com.csejudge.app.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csejudge.app.data.model.Question
import com.csejudge.app.ui.ai.AiExplanationButton
import com.csejudge.app.ui.bookmark.BookmarkStarButton

/**
 * Stateful entry point for the Question Practice screen.
 */
@Composable
fun PracticeQuestionScreen(
    viewModel: PracticeViewModel,
    onBackToFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    PracticeQuestionContent(
        uiState = uiState,
        onSubmitAnswer = { viewModel.submitAnswer(it) },
        onSkip = { viewModel.skipQuestion() },
        onNext = { viewModel.nextQuestion() },
        onBackToFilter = onBackToFilter,
        onClearError = { viewModel.clearError() },
        onToggleBookmark = { viewModel.toggleBookmark() },
        modifier = modifier
    )
}

/**
 * Stateless presentation for the Question Practice screen.
 */
@Composable
fun PracticeQuestionContent(
    uiState: PracticeUiState,
    onSubmitAnswer: (String) -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onBackToFilter: () -> Unit,
    onClearError: () -> Unit,
    onToggleBookmark: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = PracticeColors.ScreenBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                // 1. Loading State
                uiState.isLoading && uiState.currentQuestion == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = PracticeColors.PrimaryNavy,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Preparing practice questions...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PracticeColors.TextSecondary
                            )
                        }
                    }
                }

                // 2. Exhausted State
                uiState.isExhausted -> {
                    ExhaustedStateView(
                        uiState = uiState,
                        onBackToFilter = onBackToFilter
                    )
                }

                // 3. Active Question State
                uiState.currentQuestion != null -> {
                    val question = uiState.currentQuestion

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            // Crucial bottom padding so scrollable content is NEVER hidden behind fixed action button
                            .padding(bottom = 96.dp + navBarPadding.calculateBottomPadding())
                    ) {
                        // Top Header: Subject, Microtheme badge, Bookmark Star button, and Back to Filter
                        HeaderSection(
                            uiState = uiState,
                            onBackToFilter = onBackToFilter,
                            onToggleBookmark = onToggleBookmark
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Error Banner if persistence failed
                        if (uiState.errorMessage != null) {
                            ErrorBanner(
                                message = uiState.errorMessage,
                                onDismiss = onClearError
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Question Statements (Each array element on a separate line, preserving order)
                        QuestionStatementsSection(question = question)

                        Spacer(modifier = Modifier.height(20.dp))

                        // Four Vertically Stacked Option Cards: A, B, C, D
                        OptionsSection(
                            question = question,
                            uiState = uiState,
                            onSubmitAnswer = onSubmitAnswer
                        )

                        // Solution Section (Rendered ONLY after answer has been submitted)
                        if (uiState.isAnswered) {
                            Spacer(modifier = Modifier.height(24.dp))
                            SolutionSection(question = question)
                            Spacer(modifier = Modifier.height(12.dp))
                            AiExplanationButton(question = question)
                        }
                    }

                    // 4. Fixed Bottom-Right Action Button (Skip / Next)
                    // Placed outside scrollable column so it never scrolls with content
                    FixedActionButtonArea(
                        uiState = uiState,
                        onSkip = onSkip,
                        onNext = onNext,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 16.dp + navBarPadding.calculateBottomPadding(), end = 16.dp)
                    )
                }

                // 4. Fallback when no question is selected yet
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No question selected.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = PracticeColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onBackToFilter,
                                colors = ButtonDefaults.buttonColors(containerColor = PracticeColors.PrimaryNavy)
                            ) {
                                Text("Choose Subject & Mode")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top header containing breadcrumb info, restrained session stats, and action buttons.
 */
@Composable
private fun HeaderSection(
    uiState: PracticeUiState,
    onBackToFilter: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    val q = uiState.currentQuestion ?: return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Subject & Microtheme Breadcrumb
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = PracticeColors.PrimaryNavyLight,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = q.subject,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PracticeColors.PrimaryNavy,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "•  ${q.microtheme}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PracticeColors.TextSecondary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Restrained Session Statistics
            val stats = uiState.sessionStats
            Text(
                text = "Solved: ${stats.answeredCount} (${stats.correctCount} correct, ${stats.incorrectCount} wrong) • Skipped: ${stats.skippedCount}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = PracticeColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Actions: Bookmark Star Button immediately to the left of the Filters button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BookmarkStarButton(
                isBookmarked = uiState.isBookmarked,
                isLoading = uiState.isBookmarkToggling,
                onClick = onToggleBookmark
            )

            // Change Filter / Back Button
            OutlinedButton(
                onClick = onBackToFilter,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PracticeColors.BorderNeutral),
                modifier = Modifier.testTag("BackToFilterButton")
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.labelMedium,
                    color = PracticeColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Renders each element of question.question on a separate line, preserving array order.
 */
@Composable
private fun QuestionStatementsSection(question: Question) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("QuestionStatementsSection")
    ) {
        question.question.forEachIndexed { index, statement ->
            Text(
                text = statement,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 25.sp,
                    fontWeight = if (index == 0 || statement.startsWith("Which") || statement.startsWith("How many")) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    }
                ),
                color = PracticeColors.TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("QuestionStatement_$index")
            )
        }
    }
}

/**
 * Renders four rectangular option cards vertically in A, B, C, D order.
 */
@Composable
private fun OptionsSection(
    question: Question,
    uiState: PracticeUiState,
    onSubmitAnswer: (String) -> Unit
) {
    val optionsList = listOf(
        "A" to question.options.a,
        "B" to question.options.b,
        "C" to question.options.c,
        "D" to question.options.d
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("OptionsSection"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        optionsList.forEach { (letter, text) ->
            val visualState = computeOptionVisualState(
                letter = letter,
                isAnswered = uiState.isAnswered,
                selectedAnswer = uiState.selectedAnswer,
                correctAnswer = question.correctAnswer
            )

            PracticeOptionCard(
                letter = letter,
                text = text,
                visualState = visualState,
                enabled = !uiState.isAnswered && !uiState.isSubmitting && !uiState.isLoading,
                onClick = { onSubmitAnswer(letter) }
            )
        }
    }
}

/**
 * Computes option color state based on user selection and correct answer.
 */
private fun computeOptionVisualState(
    letter: String,
    isAnswered: Boolean,
    selectedAnswer: String?,
    correctAnswer: String
): OptionVisualState {
    if (!isAnswered) {
        return OptionVisualState.DEFAULT
    }

    val isThisOptionSelected = letter.equals(selectedAnswer, ignoreCase = true)
    val isThisOptionCorrect = letter.equals(correctAnswer, ignoreCase = true)

    return when {
        isThisOptionSelected && isThisOptionCorrect -> OptionVisualState.SELECTED_CORRECT
        isThisOptionSelected && !isThisOptionCorrect -> OptionVisualState.SELECTED_INCORRECT
        !isThisOptionSelected && isThisOptionCorrect -> OptionVisualState.UNSELECTED_CORRECT
        else -> OptionVisualState.DISABLED_NEUTRAL
    }
}

/**
 * Complete explanation and solution display, rendered only after answering.
 * Preserves solution array order and line breaks without concatenating paragraphs.
 */
@Composable
private fun SolutionSection(question: Question) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("SolutionSection"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = PracticeColors.SolutionBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, PracticeColors.SolutionBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Solution & Explanation",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = PracticeColors.PrimaryNavy
            )
            Spacer(modifier = Modifier.height(10.dp))

            question.solution.forEachIndexed { index, line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.5.sp,
                        lineHeight = 22.sp,
                        fontWeight = if (line.startsWith("Correct Option") || line.startsWith("Explanation:")) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    ),
                    color = PracticeColors.TextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .testTag("SolutionLine_$index")
                )
            }
        }
    }
}

/**
 * Fixed Bottom-Right Action Button Area.
 * Stays visible and anchored at the bottom-right corner while content scrolls underneath.
 */
@Composable
private fun FixedActionButtonArea(
    uiState: PracticeUiState,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (!uiState.isAnswered) {
            // Before answering: Display "Skip" button
            Button(
                onClick = onSkip,
                enabled = uiState.isSkipEnabled && !uiState.isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PracticeColors.SkipButtonBackground,
                    contentColor = PracticeColors.SkipButtonText,
                    disabledContainerColor = PracticeColors.BorderNeutral,
                    disabledContentColor = PracticeColors.TextSecondary
                ),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PracticeColors.SkipButtonBorder),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .height(46.dp)
                    .testTag("SkipButton")
            ) {
                Text(
                    text = "Skip",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        } else {
            // After answering: Display "Next" button
            Button(
                onClick = onNext,
                enabled = uiState.isNextEnabled && !uiState.isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PracticeColors.PrimaryNavy,
                    contentColor = PracticeColors.OnPrimary,
                    disabledContainerColor = PracticeColors.BorderNeutral,
                    disabledContentColor = PracticeColors.TextSecondary
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .height(46.dp)
                    .testTag("NextButton")
            ) {
                Text(
                    text = "Next",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/**
 * Completion card when question pool is exhausted.
 */
@Composable
private fun ExhaustedStateView(
    uiState: PracticeUiState,
    onBackToFilter: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("ExhaustedStateView"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PracticeColors.CardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, PracticeColors.BorderNeutral)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Practice Complete",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = PracticeColors.PrimaryNavy
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "All questions matching your filter have been attempted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PracticeColors.TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Session Summary
                val stats = uiState.sessionStats
                Surface(
                    color = PracticeColors.ScreenBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        SummaryItem("Answered", stats.answeredCount.toString())
                        SummaryItem("Correct", stats.correctCount.toString(), PracticeColors.CorrectText)
                        SummaryItem("Incorrect", stats.incorrectCount.toString(), PracticeColors.IncorrectText)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onBackToFilter,
                    colors = ButtonDefaults.buttonColors(containerColor = PracticeColors.PrimaryNavy),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("ChooseAnotherSubjectButton")
                ) {
                    Text("Choose Another Subject / Filter", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: Color = PracticeColors.TextPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = PracticeColors.TextSecondary)
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        color = PracticeColors.IncorrectBackground,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PracticeColors.IncorrectBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = PracticeColors.IncorrectText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = PracticeColors.IncorrectText)
            }
        }
    }
}
