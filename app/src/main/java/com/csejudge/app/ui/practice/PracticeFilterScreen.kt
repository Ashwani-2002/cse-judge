package com.csejudge.app.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Stateful entry point for the Practice Filter configuration screen.
 */
@Composable
fun PracticeFilterScreen(
    viewModel: PracticeViewModel,
    onStartPractice: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    PracticeFilterContent(
        uiState = uiState,
        onToggleRandom = { viewModel.setRandomMode(it) },
        onSelectSubject = { viewModel.selectSubject(it) },
        onSelectMicrotheme = { viewModel.selectMicrotheme(it) },
        onStartPractice = onStartPractice,
        onClearError = { viewModel.clearError() },
        onBack = onBack,
        modifier = modifier
    )
}

/**
 * Stateless presentation for the Practice Filter configuration screen.
 */
@Composable
fun PracticeFilterContent(
    uiState: PracticeUiState,
    onToggleRandom: (Boolean) -> Unit,
    onSelectSubject: (String) -> Unit,
    onSelectMicrotheme: (String?) -> Unit,
    onStartPractice: () -> Unit,
    onClearError: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Validation for starting practice
    val canStart = if (uiState.isRandomMode) {
        !uiState.isLoading
    } else {
        !uiState.selectedSubject.isNullOrBlank() && !uiState.isLoading
    }

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
            // Screen Header with optional Back navigation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (onBack != null) {
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(onClick = onBack)
                            .testTag("FilterBackButton"),
                        shape = RoundedCornerShape(6.dp),
                        color = PracticeColors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PracticeColors.BorderNeutral)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "←",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PracticeColors.TextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Text(
                    text = "Practice Questions",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = PracticeColors.TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Configure your practice mode and subject filters.",
                style = MaterialTheme.typography.bodyMedium,
                color = PracticeColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error banner if any
            if (uiState.errorMessage != null) {
                Surface(
                    color = PracticeColors.IncorrectBackground,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PracticeColors.IncorrectBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("FilterErrorMessage")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = PracticeColors.IncorrectText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onClearError) {
                            Text("Dismiss", color = PracticeColors.IncorrectText)
                        }
                    }
                }
            }

            // 1. Random Practice Toggle Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("RandomToggleCard"),
                shape = RoundedCornerShape(8.dp),
                color = PracticeColors.CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, PracticeColors.BorderNeutral)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Random Mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = PracticeColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Practice questions uniformly across all 7 subjects",
                            style = MaterialTheme.typography.bodySmall,
                            color = PracticeColors.TextSecondary
                        )
                    }

                    Switch(
                        checked = uiState.isRandomMode,
                        onCheckedChange = onToggleRandom,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PracticeColors.PrimaryNavy,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = PracticeColors.BorderNeutral
                        ),
                        modifier = Modifier.testTag("RandomModeSwitch")
                    )
                }
            }

            // 2. Subject and Microtheme Dropdowns (Visible ONLY when Random is disabled)
            if (!uiState.isRandomMode) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Subject & Theme Selection",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = PracticeColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Subject Dropdown
                SubjectDropdown(
                    availableSubjects = uiState.availableSubjects,
                    selectedSubject = uiState.selectedSubject,
                    onSubjectSelected = onSelectSubject
                )

                // Microtheme Dropdown (Visible only when subject is selected)
                if (uiState.selectedSubject != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    MicrothemeDropdown(
                        availableMicrothemes = uiState.availableMicrothemes,
                        selectedMicrotheme = uiState.selectedMicrotheme,
                        onMicrothemeSelected = onSelectMicrotheme
                    )
                }
            }

            // 3. Session Statistics Summary (if active)
            if (uiState.sessionStats.answeredCount > 0 || uiState.sessionStats.skippedCount > 0) {
                Spacer(modifier = Modifier.height(24.dp))
                SessionStatsSummaryCard(stats = uiState.sessionStats)
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(32.dp))

            // 4. Start Practice Button
            Button(
                onClick = onStartPractice,
                enabled = canStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PracticeColors.PrimaryNavy,
                    contentColor = PracticeColors.OnPrimary,
                    disabledContainerColor = PracticeColors.BorderNeutral,
                    disabledContentColor = PracticeColors.TextSecondary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("StartPracticeButton")
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Loading session...", fontWeight = FontWeight.SemiBold)
                } else {
                    Text(
                        text = "Start Practice",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!uiState.isRandomMode && uiState.selectedSubject == null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please select a subject to begin practice",
                    style = MaterialTheme.typography.bodySmall,
                    color = PracticeColors.TextSecondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

/**
 * Dropdown selector for available subjects.
 */
@Composable
private fun SubjectDropdown(
    availableSubjects: List<String>,
    selectedSubject: String?,
    onSubjectSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Subject *",
            style = MaterialTheme.typography.labelMedium,
            color = PracticeColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .testTag("SubjectDropdown"),
                shape = RoundedCornerShape(8.dp),
                color = PracticeColors.CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, PracticeColors.BorderNeutral)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedSubject ?: "Select a subject",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedSubject != null) PracticeColors.TextPrimary else PracticeColors.TextSecondary
                    )
                    Text("▼", fontSize = 12.sp, color = PracticeColors.TextSecondary)
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                availableSubjects.forEach { subject ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = subject,
                                color = if (subject == selectedSubject) PracticeColors.PrimaryNavy else PracticeColors.TextPrimary,
                                fontWeight = if (subject == selectedSubject) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            expanded = false
                            onSubjectSelected(subject)
                        },
                        modifier = Modifier.testTag("SubjectItem_$subject")
                    )
                }
            }
        }
    }
}

/**
 * Dropdown selector for available microthemes of the selected subject.
 */
@Composable
private fun MicrothemeDropdown(
    availableMicrothemes: List<String>,
    selectedMicrotheme: String?,
    onMicrothemeSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Microtheme (Optional)",
            style = MaterialTheme.typography.labelMedium,
            color = PracticeColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .testTag("MicrothemeDropdown"),
                shape = RoundedCornerShape(8.dp),
                color = PracticeColors.CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, PracticeColors.BorderNeutral)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedMicrotheme ?: "All microthemes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PracticeColors.TextPrimary
                    )
                    Text("▼", fontSize = 12.sp, color = PracticeColors.TextSecondary)
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                // "All microthemes" choice
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "All microthemes",
                            color = if (selectedMicrotheme == null) PracticeColors.PrimaryNavy else PracticeColors.TextPrimary,
                            fontWeight = if (selectedMicrotheme == null) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        expanded = false
                        onMicrothemeSelected(null)
                    },
                    modifier = Modifier.testTag("MicrothemeItem_All")
                )

                availableMicrothemes.forEach { mt ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = mt,
                                color = if (mt == selectedMicrotheme) PracticeColors.PrimaryNavy else PracticeColors.TextPrimary,
                                fontWeight = if (mt == selectedMicrotheme) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            expanded = false
                            onMicrothemeSelected(mt)
                        },
                        modifier = Modifier.testTag("MicrothemeItem_$mt")
                    )
                }
            }
        }
    }
}

/**
 * Compact session stats overview for the filter screen.
 */
@Composable
private fun SessionStatsSummaryCard(stats: PracticeSessionStats) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("SessionStatsSummaryCard"),
        shape = RoundedCornerShape(8.dp),
        color = PracticeColors.CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, PracticeColors.BorderNeutral)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Current Session Progress",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = PracticeColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatBadge("Solved", stats.answeredCount.toString(), PracticeColors.TextPrimary)
                StatBadge("Correct", stats.correctCount.toString(), PracticeColors.CorrectText)
                StatBadge("Incorrect", stats.incorrectCount.toString(), PracticeColors.IncorrectText)
                StatBadge("Skipped", stats.skippedCount.toString(), PracticeColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = PracticeColors.TextSecondary)
    }
}
