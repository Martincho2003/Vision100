package com.example.vision100

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vision100.network.ApiService
import com.example.vision100.repository.AuthRepository
import com.example.vision100.repository.ObjectRepository
import com.example.vision100.ui.screens.*
import com.example.vision100.ui.theme.Vision100Theme
import com.example.vision100.viewmodel.*
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private enum class ServerHealthState {
        Checking,
        Available,
        Unavailable
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val apiService = ApiService.create()
        val authRepository = AuthRepository(apiService)
        val objectRepository = ObjectRepository(apiService)

        enableEdgeToEdge()
        setContent {
            Vision100Theme {
                var serverHealthState by remember { mutableStateOf(ServerHealthState.Checking) }

                LaunchedEffect(Unit) {
                    serverHealthState = try {
                        apiService.checkHealth(ApiService.getLanguageHeader())
                        ServerHealthState.Available
                    } catch (e: Exception) {
                        ServerHealthState.Unavailable
                    }
                }

                if (serverHealthState == ServerHealthState.Checking) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    return@Vision100Theme
                }

                if (serverHealthState == ServerHealthState.Unavailable) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(stringResource(R.string.error_title)) },
                        text = { Text(stringResource(R.string.server_problem_exit_message)) },
                        confirmButton = {
                            Button(onClick = { finishAffinity() }) {
                                Text(stringResource(R.string.ok))
                            }
                        }
                    )
                    return@Vision100Theme
                }

                val auth = FirebaseAuth.getInstance()
                var currentUser by remember { mutableStateOf(auth.currentUser) }
                var screenState by remember { mutableStateOf("home") }

                DisposableEffect(auth) {
                    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        if (firebaseAuth.currentUser == null) {
                            currentUser = null
                            screenState = "home"
                        }
                    }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val user = currentUser
                    if (user == null) {
                        var authScreen by remember { mutableStateOf("login") }
                        val authViewModel: AuthViewModel = viewModel { AuthViewModel(authRepository) }
                        val isFullyAuth by authViewModel.isFullyAuthenticated
                        
                        LaunchedEffect(isFullyAuth) {
                            if (isFullyAuth) { currentUser = auth.currentUser }
                        }

                        if (screenState == "settings") {
                            SettingsScreen(onNavigateBack = { screenState = "home" }, modifier = Modifier.padding(innerPadding))
                        } else {
                            when (authScreen) {
                                "login" -> LoginScreen(
                                    viewModel = authViewModel,
                                    onLoginSuccess = {},
                                    onNavigateToRegister = { authScreen = "register" },
                                    onNavigateToSettings = { screenState = "settings" },
                                    modifier = Modifier.padding(innerPadding)
                                )
                                "register" -> RegisterScreen(
                                    viewModel = authViewModel,
                                    onRegisterSuccess = {},
                                    onNavigateToLogin = { authScreen = "login" },
                                    onNavigateToSettings = { screenState = "settings" },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    } else {
                        when (screenState) {
                            "home" -> HomeScreen(
                                onNavigateToObjects = { screenState = "objects" },
                                onNavigateToProfile = { screenState = "profile" },
                                onNavigateToSettings = { screenState = "settings" },
                                onNavigateToCheckIn = { screenState = "checkin" },
                                onNavigateToLeaderboard = { screenState = "leaderboard" },
                                modifier = Modifier.padding(innerPadding)
                            )
                            "checkin" -> {
                                val checkInViewModel: CheckInViewModel = viewModel { CheckInViewModel(apiService) }
                                CheckInScreen(viewModel = checkInViewModel, onNavigateBack = { screenState = "home" }, modifier = Modifier.padding(innerPadding))
                            }
                            "leaderboard" -> {
                                val leaderboardViewModel: LeaderboardViewModel = viewModel { LeaderboardViewModel(apiService) }
                                LeaderboardScreen(viewModel = leaderboardViewModel, onNavigateBack = { screenState = "home" }, modifier = Modifier.padding(innerPadding))
                            }
                            "objects" -> {
                                val objectViewModel: ObjectViewModel = viewModel { ObjectViewModel(objectRepository) }
                                ObjectsListScreen(viewModel = objectViewModel, onNavigateBack = { screenState = "home" }, modifier = Modifier.padding(innerPadding))
                            }
                            "profile" -> {
                                val profileViewModel: ProfileViewModel = viewModel { ProfileViewModel(apiService, authRepository) }
                                ProfileScreen(
                                    viewModel = profileViewModel,
                                    onNavigateBack = { screenState = "home" },
                                    onNavigateToHistory = { screenState = "history" },
                                    onLogout = { authRepository.signOut() },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            "history" -> {
                                val historyViewModel: HistoryViewModel = viewModel { HistoryViewModel(apiService) }
                                HistoryScreen(viewModel = historyViewModel, onNavigateBack = { screenState = "profile" }, modifier = Modifier.padding(innerPadding))
                            }
                            "settings" -> {
                                SettingsScreen(onNavigateBack = { screenState = "home" }, modifier = Modifier.padding(innerPadding))
                            }
                        }
                    }
                }
            }
        }
    }
}
