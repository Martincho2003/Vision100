package com.example.vision100.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.vision100.R
import com.example.vision100.data.VisitResponse
import com.example.vision100.network.ApiService
import com.example.vision100.ui.components.FlagAccentBar
import com.example.vision100.ui.components.FlagChip
import com.example.vision100.ui.components.VisionBackground
import com.example.vision100.ui.components.VisionEmptyState
import com.example.vision100.ui.components.VisionLoadingState
import com.example.vision100.ui.components.VisionStatTile
import com.example.vision100.ui.components.VisionTopBar
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
    var searchQuery by remember { mutableStateOf("") }

    val filteredVisits = remember(visits, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            visits
        } else {
            visits.filter { visit ->
                val touristObject = visit.touristObject
                listOfNotNull(
                    touristObject?.number,
                    touristObject?.name,
                    touristObject?.description,
                    touristObject?.region,
                    touristObject?.category,
                    touristObject?.aiLabels,
                    visit.visitedAt,
                    visit.pointsAwarded.toString()
                ).any { value -> value.contains(query, ignoreCase = true) }
            }
        }
    }

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(Unit) {
        viewModel.fetchHistory()
    }

    Scaffold(
        topBar = {
            VisionTopBar(
                title = stringResource(R.string.my_progress),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = "Back",
                onNavigationClick = onNavigateBack
            )
        },
        modifier = modifier
    ) { padding ->
        VisionBackground {
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when {
                    isLoading -> {
                        VisionLoadingState(
                            title = stringResource(R.string.my_progress),
                            message = stringResource(R.string.verified_visits),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    errorMessage != null -> {
                        VisionEmptyState(
                            icon = Icons.Default.CalendarMonth,
                            title = stringResource(R.string.error_title),
                            message = errorMessage!!,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    visits.isEmpty() -> {
                        VisionEmptyState(
                            icon = Icons.Default.PhotoCamera,
                            title = stringResource(R.string.my_progress),
                            message = stringResource(R.string.no_visits_yet),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            HistorySearchBar(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                filteredCount = filteredVisits.size,
                                onClearSearch = { searchQuery = "" }
                            )

                            if (filteredVisits.isEmpty()) {
                                VisionEmptyState(
                                    icon = Icons.Default.FilterListOff,
                                    title = stringResource(R.string.no_visits_found),
                                    message = stringResource(R.string.clear_filters),
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item {
                                        HistorySummary(visits = visits)
                                    }
                                    items(filteredVisits, key = { it.id }) { visit ->
                                        VisitItem(visit, onPhotoClick = { selectedPhotoUrl = it })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedPhotoUrl?.let { photoUrl ->
        PhotoDialog(photoUrl = photoUrl, onDismiss = { selectedPhotoUrl = null })
    }
}

@Composable
private fun HistorySearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filteredCount: Int,
    onClearSearch: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text(stringResource(R.string.search_history)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.visits_count, filteredCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (searchQuery.isNotBlank()) {
                    TextButton(onClick = onClearSearch) {
                        Text(stringResource(R.string.clear_filters))
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySummary(visits: List<VisitResponse>) {
    val totalPoints = visits.sumOf { it.pointsAwarded }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        VisionStatTile(
            label = stringResource(R.string.verified_visits),
            value = visits.size.toString(),
            icon = Icons.Default.CalendarMonth,
            modifier = Modifier.weight(1f)
        )
        VisionStatTile(
            label = stringResource(R.string.total_points),
            value = totalPoints.toString(),
            icon = Icons.Default.Stars,
            modifier = Modifier.weight(1f),
            accentColor = MaterialTheme.colorScheme.secondary
        )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            FlagAccentBar(height = 3.dp)
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp).size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = touristObject.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.visited_on, dateText),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlagChip(
                        text = stringResource(R.string.pts_earned, visit.pointsAwarded),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (hasPhoto) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.photo_saved),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
