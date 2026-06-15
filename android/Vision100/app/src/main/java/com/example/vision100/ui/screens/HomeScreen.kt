package com.example.vision100.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.vision100.R
import com.example.vision100.ui.components.FlagAccentBar
import com.example.vision100.ui.components.VisionBackground
import com.example.vision100.ui.components.VisionLogo
import com.example.vision100.ui.components.VisionTopBar
import com.example.vision100.ui.theme.Vision100Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToObjects: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCheckIn: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    isOffline: Boolean = false,
    modifier: Modifier = Modifier
) {
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        contentVisible = true
    }

    Scaffold(
        topBar = {
            VisionTopBar(
                title = "Vision100",
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    if (!isOffline) {
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        VisionBackground {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 8 })
            ) {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VisionLogo(
                        modifier = Modifier.size(152.dp),
                        animated = true
                    )

                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Text(
                        text = stringResource(R.string.home_hero_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    Text(
                        text = stringResource(R.string.home_hero_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    Button(
                        onClick = onNavigateToCheckIn,
                        modifier = Modifier
                            .padding(top = 26.dp)
                            .fillMaxWidth()
                            .height(58.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.start_exploring))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    FlagAccentBar(height = 4.dp)
                    Spacer(modifier = Modifier.height(20.dp))

                    if (!isOffline) {
                        HomeActionCard(
                            title = stringResource(R.string.tourist_objects),
                            body = stringResource(R.string.objects_action_body),
                            icon = Icons.Default.Map,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToObjects
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        HomeActionCard(
                            title = stringResource(R.string.leaderboard),
                            body = stringResource(R.string.leaderboard_action_body),
                            icon = Icons.Default.EmojiEvents,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            onClick = onNavigateToLeaderboard
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.working_offline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    body: String,
    icon: ImageVector,
    onClick: () -> Unit,
    accentColor: Color
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = accentColor.copy(alpha = 0.12f),
                contentColor = accentColor
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp).size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = accentColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    Vision100Theme {
        HomeScreen(onNavigateToObjects = {}, onNavigateToProfile = {}, onNavigateToSettings = {}, onNavigateToCheckIn = {}, onNavigateToLeaderboard = {})
    }
}
