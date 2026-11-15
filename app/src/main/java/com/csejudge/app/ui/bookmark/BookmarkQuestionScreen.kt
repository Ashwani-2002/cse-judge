package com.csejudge.app.ui.bookmark

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.csejudge.app.ui.practice.OptionVisualState
import com.csejudge.app.ui.practice.PracticeColors
import com.csejudge.app.ui.practice.PracticeOptionCard

/**
 * Screen displaying bookmarked questions for a chosen subject and microtheme.
 * Implements Level 3 of the bookmark navigation hierarchy:
 * Bookmarked Questions -> Subject -> Microtheme -> Questions
 *
 * Capabilities:
 * - Detects and presents previous attempt information (previous answer, correctness, solution)
 * - Allows full re-attempt workflow: user can click any option to re-attempt, updating existing record via upsert
 * - Bookmark star button to remove or restore bookmark without affecting attempt history
 * - Previous and Next navigation between bookmarked questions
 */
@Composable
fun BookmarkQuestionScreen(
    subject: String,
    microtheme: String,
    viewModel: BookmarkViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(subject, microtheme) {
        if (uiState.selectedSubject != subject || uiState.selectedMicrotheme != microtheme) {
            viewModel.selectMicrotheme(subject, microtheme)
        }
    }

    BookmarkQuestionContent(
        uiState = uiState,
        onAnswerOption = { viewModel.answerQuestion(it) },
        onToggleBookmark = { viewModel.toggleBookmarkCurrentQuestion() },
        onNextQuestion = { viewModel.nextQuestion() },
        onPreviousQuestion = { viewModel.previousQuestion() },
        onBack = {
            viewModel.clearMicrotheme()
            onBack()
        },
        onClearError = { viewModel.clearError() },
        modifier = modifier
    )
}

@Composable
fun BookmarkQuestionContent(
    uiState: BookmarkUiState,
    onAnswerOption: (String) -> Unit,
    onToggleBookmark: () -> Unit,
    onNextQuestion: () -> Unit,
    onPreviousQuestion: () -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("BookmarkQuestionScreen"),
        color = PracticeColors.ScreenBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                // Loading State
                uiState.isLoading && uiState.currentQuestionState == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = PracticeColors.PrimaryNavy,
                            strokeWidth = 3.dp
                        )
                    }
                }

                // Active Question State
                uiState.currentQuestionState != null -> {
                    val qState = uiState.currentQuestionState
                    val question = qState.question

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .padding(bottom = 96.dp + navBarPadding.calculateBottomPadding())
                    ) {
                        // Header: Subject, Microtheme badge, Bookmark Star button, and Back button
                        BookmarkHeaderSection(
                            question = question,
                            isBookmarked = qState.isBookmarked,
                            isBookmarkToggling = uiState.isBookmarkToggling,
                            questionIndex = uiState.currentQuestionIndex,
                            totalQuestions = uiState.totalQuestionsInCurrentMicrotheme,
                            onToggleBookmark = onToggleBookmark,
                            onBack = onBack
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Error Banner if persistence failed
                        if (uiState.errorMessage != null) {
                            BookmarkErrorBanner(
                                message = uiState.errorMessage,
                                onDismiss = onClearError
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Question Statements
                        BookmarkQuestionStatementsSection(question = question)

                        Spacer(modifier = Modifier.height(20.dp))

                        // Four Option Cards (Clickable for first attempt OR re-attempt)
                        BookmarkOptionsSection(
                            question = question,
                            qState = qState,
                            isSubmitting = uiState.isSubmitting,
                            onAnswerOption = onAnswerOption
                        )

                        // Solution Section (Rendered if attempted or answered)
                        if (qState.isAttempted) {
                            Spacer(modifier = Modifier.height(24.dp))
                            BookmarkSolutionSection(question = question)
                            Spacer(modifier = Modifier.height(12.dp))
                            AiExplanationButton(question = question)
                        }
                    }

                    // Fixed Bottom Action Bar: Previous and Next buttons
                    BookmarkFixedNavigationArea(
                        hasPrevious = uiState.hasPreviousQuestion,
                        hasNext = uiState.hasNextQuestion,
                        currentIndex = uiState.currentQuestionIndex,
                        totalQuestions = uiState.totalQuestionsInCurrentMicrotheme,
                        onPrevious = onPreviousQuestion,
                        onNext = onNextQuestion,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 16.dp + navBarPadding.calculateBottomPadding()
                            )
                    )
                }

                // Empty / No Questions State
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "No bookmarked questions here.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = PracticeColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onBack,
                                colors = ButtonDefaults.buttonColors(containerColor = PracticeColors.PrimaryNavy)
                            ) {
                                Text("Back to Microthemes")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top header containing breadcrumb, question counter, star toggle, and back button.
 */
@Composable
private fun BookmarkHeaderSection(
    question: Question,
    isBookmarked: Boolean,
    isBookmarkToggling: Boolean,
    questionIndex: Int,
    totalQuestions: Int,
    onToggleBookmark: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("BookmarkHeaderSection"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = PracticeColors.PrimaryNavyLight,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = question.subject,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PracticeColors.PrimaryNavy,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "•  ${question.microtheme}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PracticeColors.TextSecondary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Bookmarked Question ${questionIndex + 1} of $totalQuestions",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = PracticeColors.TextSecondary,
                modifier = Modifier.testTag("BookmarkQuestionCounter")
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Actions: Bookmark Star Button immediately to the left of Back button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BookmarkStarButton(
                isBookmarked = isBookmarked,
                isLoading = isBookmarkToggling,
                onClick = onToggleBookmark
            )

            OutlinedButton(
                onClick = onBack,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, PracticeColors.BorderNeutral),
                modifier = Modifier.testTag("BookmarkBackButton")
            ) {
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.labelMedium,
                    color = PracticeColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Question statements displayed preserving array order and line breaks.
 */
@Composable
private fun BookmarkQuestionStatementsSection(question: Question) {
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
 * Four option cards.
 * If attempted: displays previous answer (green/red) and correct answer (green).
 * Re-attempt capability: options remain clickable to allow re-answering!
 */
@Composable
private fun BookmarkOptionsSection(
    question: Question,
    qState: BookmarkQuestionState,
    isSubmitting: Boolean,
    onAnswerOption: (String) -> Unit
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
            val visualState = computeBookmarkOptionVisualState(
                letter = letter,
                isAttempted = qState.isAttempted,
                selectedAnswer = qState.selectedAnswer,
                correctAnswer = question.correctAnswer
            )

            PracticeOptionCard(
                letter = letter,
                text = text,
                visualState = visualState,
                enabled = !isSubmitting,
                onClick = { onAnswerOption(letter) }
            )
        }
    }
}

/**
 * Evaluates option visual state:
 * - If not attempted: DEFAULT
 * - If attempted:
 *   - Selected and correct: SELECTED_CORRECT (Green)
 *   - Selected and wrong: SELECTED_INCORRECT (Red)
 *   - Unselected but correct: UNSELECTED_CORRECT (Green)
 *   - Others: DISABLED_NEUTRAL
 */
private fun computeBookmarkOptionVisualState(
    letter: String,
    isAttempted: Boolean,
    selectedAnswer: String?,
    correctAnswer: String
): OptionVisualState {
    if (!isAttempted) {
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
 * Question explanation and solution.
 */
@Composable
private fun BookmarkSolutionSection(question: Question) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("SolutionSection"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = PracticeColors.SolutionBackground),
        border = BorderStroke(1.dp, PracticeColors.SolutionBorder)
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
 * Fixed Bottom Navigation Bar for Bookmarked Questions.
 * Displays Previous and Next buttons.
 */
@Composable
private fun BookmarkFixedNavigationArea(
    hasPrevious: Boolean,
    hasNext: Boolean,
    currentIndex: Int,
    totalQuestions: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Button
        Button(
            onClick = onPrevious,
            enabled = hasPrevious,
            colors = ButtonDefaults.buttonColors(
                containerColor = PracticeColors.SkipButtonBackground,
                contentColor = PracticeColors.SkipButtonText,
                disabledContainerColor = PracticeColors.BorderNeutral,
                disabledContentColor = PracticeColors.TextSecondary
            ),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, PracticeColors.SkipButtonBorder),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .height(46.dp)
                .testTag("BookmarkPreviousButton")
        ) {
            Text(
                text = "← Previous",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        // Question counter indicator
        Text(
            text = "${currentIndex + 1} / $totalQuestions",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = PracticeColors.TextSecondary,
            modifier = Modifier.testTag("BookmarkIndexIndicator")
        )

        // Next Button
        Button(
            onClick = onNext,
            enabled = hasNext,
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
                .testTag("BookmarkNextButton")
        ) {
            Text(
                text = "Next →",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun BookmarkErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        color = PracticeColors.IncorrectBackground,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, PracticeColors.IncorrectBorder),
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
