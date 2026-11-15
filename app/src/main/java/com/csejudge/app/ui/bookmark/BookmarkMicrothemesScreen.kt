package com.csejudge.app.ui.bookmark

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csejudge.app.ui.practice.PracticeColors

/**
 * Screen displaying microthemes containing bookmarked questions for a chosen subject.
 * Implements Level 2 of the bookmark navigation hierarchy:
 * Bookmarked Questions -> Subject -> Microtheme -> Questions
 */
@Composable
fun BookmarkMicrothemesScreen(
    subject: String,
    viewModel: BookmarkViewModel,
    onSelectMicrotheme: (String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(subject) {
        if (uiState.selectedSubject != subject) {
            viewModel.selectSubject(subject)
        }
    }

    BookmarkMicrothemesContent(
        subject = subject,
        uiState = uiState,
        onSelectMicrotheme = { microtheme ->
            viewModel.selectMicrotheme(subject, microtheme)
            onSelectMicrotheme(subject, microtheme)
        },
        onBack = {
            viewModel.clearSubject()
            onBack()
        },
        modifier = modifier
    )
}

@Composable
fun BookmarkMicrothemesContent(
    subject: String,
    uiState: BookmarkUiState,
    onSelectMicrotheme: (String) -> Unit,
    onBack: () -> Unit,
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
            // Header with Back button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = onBack)
                        .testTag("BookmarkMicrothemesBackButton"),
                    shape = RoundedCornerShape(6.dp),
                    color = PracticeColors.CardSurface,
                    border = BorderStroke(1.dp, PracticeColors.BorderNeutral)
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
                Column {
                    Text(
                        text = subject,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = PracticeColors.TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Select a microtheme to review bookmarked questions.",
                style = MaterialTheme.typography.bodyMedium,
                color = PracticeColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.microthemes.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("NoMicrothemesEmptyView"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PracticeColors.CardSurface),
                    border = BorderStroke(1.dp, PracticeColors.BorderNeutral)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Bookmarks in $subject",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PracticeColors.PrimaryNavy
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "All bookmarked questions in this subject have been removed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PracticeColors.TextSecondary
                        )
                    }
                }
            } else {
                Text(
                    text = "Microthemes",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = PracticeColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.microthemes.forEach { item ->
                        BookmarkMicrothemeCard(
                            item = item,
                            onClick = { onSelectMicrotheme(item.microtheme) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarkMicrothemeCard(
    item: BookmarkMicrothemeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("BookmarkMicrothemeItem_${item.microtheme}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PracticeColors.CardSurface),
        border = BorderStroke(1.dp, PracticeColors.BorderNeutral),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.microtheme,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    ),
                    color = PracticeColors.PrimaryNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.questionCount} ${if (item.questionCount == 1) "question" else "questions"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PracticeColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp),
                color = PracticeColors.PrimaryNavyLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "→",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PracticeColors.PrimaryNavy
                    )
                }
            }
        }
    }
}
