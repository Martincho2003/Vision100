package com.example.vision100.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.vision100.R
import com.example.vision100.data.VisitResponse
import com.example.vision100.network.ApiService
import com.example.vision100.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visits by viewModel.visits
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    
    var selectedPhotoUrl by remember { mutableStateOf<String?>(null) }

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(Unit) {
        viewModel.fetchHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_progress)) },
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
            } else if (visits.isEmpty()) {
                Text(stringResource(R.string.no_visits_yet), modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visits) { visit ->
                        VisitItem(visit, onPhotoClick = { selectedPhotoUrl = it })
                    }
                }
            }
        }
    }

    if (selectedPhotoUrl != null) {
        PhotoDialog(photoUrl = selectedPhotoUrl!!, onDismiss = { selectedPhotoUrl = null })
    }
}

@Composable
fun VisitItem(visit: VisitResponse, onPhotoClick: (String) -> Unit) {
    val touristObject = visit.touristObject ?: return
    var dateText = visit.visitedAt ?: ""
    
    try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val parsedDate = inputFormat.parse(dateText)
        
        if (parsedDate != null) {
            val outputFormat = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            dateText = outputFormat.format(parsedDate)
        }
    } catch (e: Exception) {

    }

    val hasPhoto = !visit.photoUrl.isNullOrEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = hasPhoto) { 
                onPhotoClick(ApiService.getVisitPhotoUrl(visit.id))
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = touristObject.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.visited_on, dateText),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = stringResource(R.string.pts_earned, visit.pointsAwarded),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (hasPhoto) {
                    Text("📸 " + stringResource(R.string.photo_saved), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun PhotoDialog(photoUrl: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Visit Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
