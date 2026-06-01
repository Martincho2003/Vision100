package com.example.vision100.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vision100.R
import com.example.vision100.data.LeaderboardUser
import com.example.vision100.ui.components.FlagAccentBar
import com.example.vision100.ui.components.FlagChip
import com.example.vision100.ui.components.VisionBackground
import com.example.vision100.ui.components.VisionEmptyState
import com.example.vision100.ui.components.VisionLoadingState
import com.example.vision100.ui.components.VisionTopBar
import com.example.vision100.viewmodel.LeaderboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val users by viewModel.users
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(Unit) {
        viewModel.fetchLeaderboard()
    }

    Scaffold(
        topBar = {
            VisionTopBar(
                title = stringResource(R.string.leaderboard),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = "Back",
                onNavigationClick = onNavigateBack
            )
        },
        modifier = modifier
    ) { padding ->
        VisionBackground {
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (isLoading) {
                    VisionLoadingState(
                        title = stringResource(R.string.leaderboard),
                        message = stringResource(R.string.leaderboard_action_body),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (errorMessage != null) {
                    VisionEmptyState(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        title = stringResource(R.string.error_title),
                        message = errorMessage!!,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (users.isEmpty()) {
                    VisionEmptyState(
                        icon = Icons.Default.EmojiEvents,
                        title = stringResource(R.string.leaderboard),
                        message = stringResource(R.string.leaderboard_empty),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            LeaderboardHeader(users = users.take(3))
                        }
                        itemsIndexed(users, key = { _, user -> user.id }) { index, user ->
                            LeaderboardItem(user, index + 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardHeader(users: List<LeaderboardUser>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            FlagAccentBar(height = 4.dp)
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.leaderboard),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    users.forEachIndexed { index, user ->
                        PodiumTile(
                            user = user,
                            rank = index + 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumTile(
    user: LeaderboardUser,
    rank: Int,
    modifier: Modifier = Modifier
) {
    val color = rankColor(rank)
    Surface(
        modifier = modifier.heightIn(min = 112.dp),
        shape = MaterialTheme.shapes.large,
        color = color.copy(alpha = 0.1f),
        contentColor = color
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (rank == 1) Icons.Default.EmojiEvents else Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = "${user.totalPoints} pts",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LeaderboardItem(user: LeaderboardUser, rank: Int) {
    val color = rankColor(rank)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rank <= 3) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (rank <= 3) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.width(52.dp),
                shape = MaterialTheme.shapes.medium,
                color = color.copy(alpha = 0.14f),
                contentColor = color
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                contentColor = color
            ) {
                Icon(
                    imageVector = if (rank == 1) Icons.Default.EmojiEvents else Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "${user.totalPoints} pts", style = MaterialTheme.typography.bodySmall)
            }

            if (rank <= 3) {
                FlagChip(text = "TOP $rank", color = color)
            }
        }
    }
}

private fun rankColor(rank: Int): Color {
    return when (rank) {
        1 -> Color(0xFFE0A800)
        2 -> Color(0xFF00966E)
        3 -> Color(0xFFD62612)
        else -> Color(0xFF61736C)
    }
}
