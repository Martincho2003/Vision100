package com.example.vision100.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vision100.R
import com.example.vision100.ui.theme.Vision100Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToObjects: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCheckIn: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Vision100", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                },
                windowInsets = WindowInsets(top = 0.dp)
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "100",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 100.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            MenuButton(
                text = stringResource(R.string.smart_check_in),
                icon = Icons.Default.QrCodeScanner,
                onClick = onNavigateToCheckIn
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            MenuButton(
                text = stringResource(R.string.tourist_objects),
                icon = Icons.Default.Map,
                onClick = onNavigateToObjects
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            MenuButton(
                text = stringResource(R.string.leaderboard),
                icon = Icons.Default.EmojiEvents,
                onClick = onNavigateToLeaderboard,
                isSecondary = true
            )
        }
    }
}

@Composable
fun MenuButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isSecondary: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        colors = if (isSecondary) {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        } else {
            ButtonDefaults.buttonColors()
        },
        shape = MaterialTheme.shapes.large
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp))
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    Vision100Theme {
        HomeScreen(onNavigateToObjects = {}, onNavigateToProfile = {}, onNavigateToSettings = {}, onNavigateToCheckIn = {}, onNavigateToLeaderboard = {})
    }
}
