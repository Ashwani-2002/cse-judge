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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csejudge.app.data.model.TestType
import com.csejudge.app.ui.practice.PracticeColors

/**
 * Pre-test instructions screen displaying mandatory rules and requirements
 * prior to starting a Full Length or Half Length test.
 */
@Composable
fun TestInstructionsScreen(
    testType: TestType,
    viewModel: TestViewModel,
    onStartTest: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(testType) {
        viewModel.checkEligibility(testType)
    }

    val isSufficient = uiState.availableUnattemptedQuestions >= testType.totalQuestions

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onBack)
                        .testTag("InstructionsBackButton"),
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
                        text = testType.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = PracticeColors.PrimaryNavy
                    )
                    Text(
                        text = "Instructions and Test Rules",
                        style = MaterialTheme.typography.bodySmall,
                        color = PracticeColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Test Overview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("TestOverviewCard"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PracticeColors.CardSurface),
                border = BorderStroke(1.dp, PracticeColors.BorderNeutral)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Test Specifications",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PracticeColors.PrimaryNavy
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SpecItem(label = "Questions", value = "${testType.totalQuestions}")
                        SpecItem(label = "Duration", value = "${testType.durationMinutes} mins")
                        SpecItem(label = "Max Marks", value = "${testType.maxScore.toInt()}")
                        SpecItem(label = "Marking", value = "+2 / -2/3")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Instructions List
            Text(
                text = "Important Instructions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = PracticeColors.PrimaryNavy
            )

            Spacer(modifier = Modifier.height(12.dp))

            InstructionItem(
                number = "1",
                text = "There are a total of ${testType.totalQuestions} questions in the test."
            )
            InstructionItem(
                number = "2",
                text = "Test duration is ${testType.durationMinutes} minutes. The countdown timer starts immediately."
            )
            InstructionItem(
                number = "3",
                text = "Only a single attempt is allowed. The test cannot be paused or intentionally aborted. Leaving the app or backgrounding will abort the test."
            )
            InstructionItem(
                number = "4",
                text = "Questions can be bookmarked for revision afterwards, after test completion."
            )
            InstructionItem(
                number = "5",
                text = "Each correct answer awards +2 marks. Each incorrect answer deducts 2/3 marks (-0.67). Unanswered questions receive 0 marks."
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Question Bank Availability Warning
            if (!uiState.isLoading) {
                if (!isSufficient) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("InsufficientQuestionsBanner"),
                        shape = RoundedCornerShape(8.dp),
                        color = PracticeColors.IncorrectBackground,
                        border = BorderStroke(1.dp, PracticeColors.IncorrectBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Insufficient Questions Available",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PracticeColors.IncorrectText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Only ${uiState.availableUnattemptedQuestions} unattempted questions remain in the question bank, but ${testType.totalQuestions} unique questions are required to start this test.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PracticeColors.IncorrectText
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = PracticeColors.CorrectBackground,
                        border = BorderStroke(1.dp, PracticeColors.CorrectBorder)
                    ) {
                        Text(
                            text = "${uiState.availableUnattemptedQuestions} unattempted questions eligible in question bank.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = PracticeColors.CorrectText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(16.dp))

            // Start Test Button
            Button(
                onClick = {
                    viewModel.startTest(testType)
                    onStartTest()
                },
                enabled = !uiState.isLoading && isSufficient,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("StartTestButton"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PracticeColors.PrimaryNavy,
                    contentColor = PracticeColors.OnPrimary
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = PracticeColors.OnPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Start Test",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SpecItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = PracticeColors.PrimaryNavy
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PracticeColors.TextSecondary
        )
    }
}

@Composable
fun InstructionItem(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = RoundedCornerShape(12.dp),
            color = PracticeColors.PrimaryNavyLight
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PracticeColors.PrimaryNavy
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            color = PracticeColors.TextPrimary
        )
    }
}
