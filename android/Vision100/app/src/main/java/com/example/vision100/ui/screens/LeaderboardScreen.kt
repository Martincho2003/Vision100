package com.example.vision100.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
            TopAppBar(
                title = { Text(stringResource(R.string.leaderboard)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(top = 0.dp)
            )
        },
        modifier = modifier
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(users) { index, user ->
                        LeaderboardItem(user, index + 1)
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardItem(user: LeaderboardUser, rank: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (rank <= 3) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) 
                 else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.width(48.dp)
            )
            
            Icon(
                imageVector = if (rank == 1) Icons.Default.EmojiEvents else Icons.Default.Person,
                contentDescription = null,
                tint = if (rank == 1) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "${user.totalPoints} pts", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
