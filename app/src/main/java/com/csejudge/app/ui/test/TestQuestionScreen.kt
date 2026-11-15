package com.csejudge.app.ui.test

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.csejudge.app.ui.practice.PracticeColors

/**
 * Active test question screen.
 * Features:
 * - Persistent countdown timer (starts at 120:00 for FLT, 60:00 for HLT).
 * - Hamburger icon button to open question palette overlay.
 * - Question statement and options A-D with neutral selection highlight (no answer revelation).
 * - Fixed bottom action bar: "Mark for Review", "Next", "Submit".
 * - Strict background detection: Aborts test if app is sent to background or phone is locked.
 * - Back button abort confirmation.
 */
@Composable
fun TestQuestionScreen(
    viewModel: TestViewModel,
    onTestCompleted: (String) -> Unit,
    onTestAborted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Strict Android Lifecycle Detection:
    // When the user leaves the app or phone locks, ON_STOP is triggered on the Activity.
    // Configuration changes (e.g. rotation) are explicitly excluded.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val activity = context as? Activity
                val isChangingConfigs = activity?.isChangingConfigurations == true
                if (!isChangingConfigs) {
                    viewModel.abortTest(reason = "Test aborted: Application was sent to background")
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // React to test completion or abort
    LaunchedEffect(uiState.isCompleted, uiState.completedTestId) {
        if (uiState.isCompleted && uiState.completedTestId != null) {
            onTestCompleted(uiState.completedTestId!!)
        }
    }

    LaunchedEffect(uiState.isAborted) {
        if (uiState.isAborted) {
            onTestAborted()
        }
    }

    // Intercept back button to confirm abort
    BackHandler {
        viewModel.showAbortDialog()
    }

    val currentQuestion = uiState.currentQuestion
    val currentIndex = uiState.currentQuestionIndex
    val totalCount = uiState.totalQuestions
    val isMarkedForReview = uiState.isCurrentMarkedForReview
    val selectedAnswer = uiState.currentSelectedAnswer

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PracticeColors.ScreenBackground,
        topBar = {
            TestHeaderBar(
                formattedTimer = uiState.formattedTimer,
                onOpenPalette = { viewModel.openPalette() }
            )
        },
        bottomBar = {
            TestBottomActionBar(
                isMarkedForReview = isMarkedForReview,
                isFinalQuestion = currentIndex >= totalCount - 1,
                isSubmitting = uiState.isSubmitting,
                onToggleMarkForReview = { viewModel.toggleMarkForReview() },
                onNext = { viewModel.nextQuestion() },
                onSubmit = { viewModel.showSubmitDialog() }
            )
        }
    ) { innerPadding ->
        if (currentQuestion == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No questions loaded",
                    color = PracticeColors.TextSecondary
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Question Number Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question ${currentIndex + 1} of $totalCount",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = PracticeColors.PrimaryNavy,
                        modifier = Modifier.testTag("QuestionNumberText")
                    )

                    if (isMarkedForReview) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PalettePurple.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, PalettePurple)
                        ) {
                            Text(
                                text = "Marked for Review",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PalettePurple,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Question Statements (Direct rendering matching Practice, no enclosing card)
                TestQuestionStatementsSection(statements = currentQuestion.question)

                Spacer(modifier = Modifier.height(20.dp))

                // Options A, B, C, D (Neutral selection highlight, no correctness revealed)
                val options = listOf(
                    "A" to currentQuestion.options.a,
                    "B" to currentQuestion.options.b,
                    "C" to currentQuestion.options.c,
                    "D" to currentQuestion.options.d
                )

                options.forEach { (letter, text) ->
                    val isSelected = selectedAnswer == letter
                    TestOptionCard(
                        letter = letter,
                        text = text,
                        isSelected = isSelected,
                        enabled = !uiState.isSubmitting && uiState.remainingSeconds > 0,
                        onClick = { viewModel.selectOption(letter) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Submitting Overlay Dialog (Un-dismissible during persistence)
    if (uiState.isSubmitting) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = null,
            text = {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = PracticeColors.PrimaryNavy,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (uiState.remainingSeconds <= 0) "Time expired. Submitting test..." else "Submitting test...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = PracticeColors.TextPrimary
                    )
                }
            },
            containerColor = PracticeColors.CardSurface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("SubmittingProgressDialog")
        )
    }

    // Question Palette Overlay
    if (uiState.isPaletteOpen) {
        TestPaletteDialog(
            totalQuestions = totalCount,
            currentQuestionIndex = currentIndex,
            getPaletteState = { idx -> uiState.getPaletteState(idx) },
            onSelectQuestion = { idx -> viewModel.navigateToQuestion(idx) },
            onDismiss = { viewModel.closePalette() }
        )
    }

    // Submit Confirmation Dialog
    if (uiState.showSubmitConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSubmitDialog() },
            title = {
                Text(
                    text = "Submit Test?",
                    fontWeight = FontWeight.Bold,
                    color = PracticeColors.PrimaryNavy
                )
            },
            text = {
                Text(
                    text = "Submitting the test will end the test session and your result will be generated. You cannot modify your answers after submission.",
                    color = PracticeColors.TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.submitTest() },
                    colors = ButtonDefaults.buttonColors(containerColor = PracticeColors.PrimaryNavy),
                    modifier = Modifier.testTag("ConfirmSubmitButton")
                ) {
                    Text("Confirm Submit", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissSubmitDialog() },
                    modifier = Modifier.testTag("CancelSubmitButton")
                ) {
                    Text("Cancel", color = PracticeColors.TextSecondary)
                }
            },
            containerColor = PracticeColors.CardSurface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("SubmitConfirmationDialog")
        )
    }

    // Exit & Abort Confirmation Dialog
    if (uiState.showAbortConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAbortDialog() },
            title = {
                Text(
                    text = "Exit & Abort Test?",
                    fontWeight = FontWeight.Bold,
                    color = PracticeColors.IncorrectBorder
                )
            },
            text = {
                Text(
                    text = "If you exit now, your active test will be aborted immediately. No answers or scores will be saved, and no report will be generated.",
                    color = PracticeColors.TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissAbortDialog()
                        viewModel.abortTest(reason = "Test aborted: User exited session")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PracticeColors.IncorrectBorder),
                    modifier = Modifier.testTag("ConfirmAbortButton")
                ) {
                    Text("Exit & Abort", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissAbortDialog() },
                    modifier = Modifier.testTag("ContinueTestButton")
                ) {
                    Text("Continue Test", color = PracticeColors.PrimaryNavy)
                }
            },
            containerColor = PracticeColors.CardSurface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("AbortConfirmationDialog")
        )
    }
}

/**
 * Renders each element of question statements on a separate line, preserving array order.
 * Visually and semantically identical to Practice QuestionStatementsSection.
 */
@Composable
fun TestQuestionStatementsSection(statements: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("QuestionStatementsSection")
    ) {
        statements.forEachIndexed { index, statement ->
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
 * Top header containing countdown timer on left and hamburger palette button on right.
 */
@Composable
fun TestHeaderBar(
    formattedTimer: String,
    onOpenPalette: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PracticeColors.CardSurface,
        border = BorderStroke(1.dp, PracticeColors.BorderNeutral),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Countdown Timer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("TestTimerContainer")
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PracticeColors.PrimaryNavyLight,
                    border = BorderStroke(1.dp, PracticeColors.PrimaryNavy.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏱",
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formattedTimer,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = PracticeColors.PrimaryNavy,
                            modifier = Modifier.testTag("CountdownTimerText")
                        )
                    }
                }
            }

            // Question Palette Hamburger Button (Icon with 3 horizontal bars)
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onOpenPalette)
                    .testTag("PaletteHamburgerButton"),
                shape = RoundedCornerShape(8.dp),
                color = PracticeColors.PrimaryNavyLight,
                border = BorderStroke(1.dp, PracticeColors.BorderNeutral)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    HamburgerIcon(
                        color = PracticeColors.PrimaryNavy,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Three horizontal bars hamburger icon drawn on Canvas.
 */
@Composable
fun HamburgerIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.2.dp.toPx()
        val w = size.width
        val h = size.height

        val y1 = h * 0.22f
        val y2 = h * 0.50f
        val y3 = h * 0.78f

        drawLine(color, Offset(0f, y1), Offset(w, y1), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(0f, y2), Offset(w, y2), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(0f, y3), Offset(w, y3), strokeWidth, StrokeCap.Round)
    }
}

/**
 * Option card for active test questions.
 * Highlights selection without revealing correctness.
 */
@Composable
fun TestOptionCard(
    letter: String,
    text: String,
    isSelected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) PracticeColors.PrimaryNavyLight else PracticeColors.CardSurface
    val borderColor = if (isSelected) PracticeColors.PrimaryNavy else PracticeColors.BorderNeutral
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val badgeBg = if (isSelected) PracticeColors.PrimaryNavy else PracticeColors.PrimaryNavyLight
    val badgeText = if (isSelected) Color.White else PracticeColors.PrimaryNavy

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("TestOptionCard_$letter"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(6.dp),
                color = badgeBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = letter,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.5.sp,
                    lineHeight = 21.sp
                ),
                color = PracticeColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Fixed bottom action bar: "Mark for Review", "Next", "Submit".
 */
@Composable
fun TestBottomActionBar(
    isMarkedForReview: Boolean,
    isFinalQuestion: Boolean,
    isSubmitting: Boolean = false,
    onToggleMarkForReview: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PracticeColors.CardSurface,
        border = BorderStroke(1.dp, PracticeColors.BorderNeutral),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Mark for Review Button
            OutlinedButton(
                onClick = onToggleMarkForReview,
                enabled = !isSubmitting,
                modifier = Modifier
                    .weight(1.5f)
                    .height(50.dp)
                    .testTag("MarkForReviewButton"),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                border = BorderStroke(
                    1.5.dp,
                    if (isMarkedForReview) PalettePurple else PracticeColors.BorderNeutral
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isMarkedForReview) PalettePurple.copy(alpha = 0.12f) else Color.Transparent,
                    contentColor = if (isMarkedForReview) PalettePurple else PracticeColors.TextPrimary
                )
            ) {
                Text(
                    text = if (isMarkedForReview) "Unmark Review" else "Mark for Review",
                    fontSize = 12.5.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }

            // 2. Next Button
            Button(
                onClick = onNext,
                enabled = !isSubmitting,
                modifier = Modifier
                    .weight(0.8f)
                    .height(50.dp)
                    .testTag("TestNextButton"),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PracticeColors.PrimaryNavyLight,
                    contentColor = PracticeColors.PrimaryNavy
                )
            ) {
                Text(
                    text = "Next",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // 3. Submit Button
            Button(
                onClick = onSubmit,
                enabled = !isSubmitting,
                modifier = Modifier
                    .weight(0.95f)
                    .height(50.dp)
                    .testTag("TestSubmitButton"),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PracticeColors.PrimaryNavy,
                    contentColor = PracticeColors.OnPrimary
                )
            ) {
                Text(
                    text = "Submit",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
