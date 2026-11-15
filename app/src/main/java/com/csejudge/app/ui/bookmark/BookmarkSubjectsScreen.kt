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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csejudge.app.ui.practice.PracticeColors

/**
 * Screen displaying subjects containing bookmarked questions.
 * Implements Level 1 of the bookmark navigation hierarchy:
 * Bookmarked Questions -> Subject -> Microtheme -> Questions
 */
@Composable
fun BookmarkSubjectsScreen(
    viewModel: BookmarkViewModel,
    onSelectSubject: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToPractice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    BookmarkSubjectsContent(
        uiState = uiState,
        onSelectSubject = { subject ->
            viewModel.selectSubject(subject)
            onSelectSubject(subject)
        },
        onBack = onBack,
        onNavigateToPractice = onNavigateToPractice,
        modifier = modifier
    )
}

@Composable
fun BookmarkSubjectsContent(
    uiState: BookmarkUiState,
    onSelectSubject: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToPractice: () -> Unit,
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
                        .testTag("BookmarkSubjectsBackButton"),
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
                        text = "Bookmarked Questions",
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
                text = "Select a subject to review your bookmarked questions.",
                style = MaterialTheme.typography.bodyMedium,
                color = PracticeColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.subjects.isEmpty()) {
                // Empty state view
                BookmarkEmptyStateView(
                    onNavigateToPractice = onNavigateToPractice
                )
            } else {
                Text(
                    text = "Subjects with Bookmarks",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = PracticeColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.subjects.forEach { item ->
                        BookmarkSubjectCard(
                            item = item,
                            onClick = { onSelectSubject(item.subject) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarkSubjectCard(
    item: BookmarkSubjectItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("BookmarkSubjectItem_${item.subject}"),
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
                    text = item.subject,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
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

@Composable
fun BookmarkEmptyStateView(
    onNavigateToPractice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("BookmarkEmptyStateView"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PracticeColors.CardSurface),
        border = BorderStroke(1.dp, PracticeColors.BorderNeutral)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(14.dp),
                color = PracticeColors.BookmarkYellowLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    StarIcon(
                        isBookmarked = true,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Bookmarked Questions",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = PracticeColors.PrimaryNavy
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You haven't bookmarked any questions yet. While practicing questions, tap the star icon next to Filters to bookmark questions for revision here.",
                style = MaterialTheme.typography.bodyMedium,
                color = PracticeColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onNavigateToPractice,
                colors = ButtonDefaults.buttonColors(containerColor = PracticeColors.PrimaryNavy),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("GoToPracticeButton")
            ) {
                Text(
                    text = "Go to Practice",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
