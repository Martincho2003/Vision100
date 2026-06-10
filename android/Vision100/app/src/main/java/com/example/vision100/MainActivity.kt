package com.example.vision100

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vision100.network.ApiService
import com.example.vision100.repository.AuthRepository
import com.example.vision100.repository.ObjectRepository
import com.example.vision100.ui.components.VisionBackground
import com.example.vision100.ui.components.VisionLoadingState
import com.example.vision100.ui.screens.*
import com.example.vision100.ui.theme.AppThemeMode
import com.example.vision100.ui.theme.Vision100Theme
import com.example.vision100.viewmodel.*
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private companion object {
        const val PREFS_NAME = "vision100_preferences"
        const val PREF_THEME_MODE = "theme_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        val apiService = ApiService.create()
        val authRepository = AuthRepository(apiService)
        val objectRepository = ObjectRepository(apiService)

        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = viewModel { MainViewModel(apiService) }
            val isOffline by mainViewModel.isOffline.collectAsState()
            val isCheckingHealth by mainViewModel.isCheckingHealth.collectAsState()

            val preferences = remember {
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
            var appThemeMode by remember {
                mutableStateOf(AppThemeMode.fromStoredValue(preferences.getString(PREF_THEME_MODE, null)))
            }
            val systemDarkTheme = isSystemInDarkTheme()
            val useDarkTheme = when (appThemeMode) {
                AppThemeMode.System -> systemDarkTheme
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
            }

            Vision100Theme(darkTheme = useDarkTheme) {
                val auth = FirebaseAuth.getInstance()
                var currentUser by remember { mutableStateOf(auth.currentUser) }
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            mainViewModel.checkHealth()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                if (isCheckingHealth) {
                    VisionBackground {
                        VisionLoadingState(
                            title = stringResource(R.string.loading_server),
                            message = stringResource(R.string.checking_server),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        )
                    }
                    return@Vision100Theme
                }

                if (isOffline && currentUser == null) {
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
                    val onThemeModeChange: (AppThemeMode) -> Unit = { mode ->
                        appThemeMode = mode
                        preferences.edit().putString(PREF_THEME_MODE, mode.name).apply()
                    }

                    if (user == null) {
                        var authScreen by remember { mutableStateOf("login") }
                        val authViewModel: AuthViewModel = viewModel { AuthViewModel(authRepository) }
                        val isFullyAuth by authViewModel.isFullyAuthenticated
                        
                        LaunchedEffect(isFullyAuth) {
                            if (isFullyAuth) { currentUser = auth.currentUser }
                        }

                        if (screenState == "settings") {
                            SettingsScreen(
                                themeMode = appThemeMode,
                                onThemeModeChange = onThemeModeChange,
                                onNavigateBack = { screenState = "home" },
                                modifier = Modifier.padding(innerPadding)
                            )
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
                                isOffline = isOffline,
                                modifier = Modifier.padding(innerPadding)
                            )
                            "checkin" -> {
                                val checkInViewModel: CheckInViewModel = viewModel { CheckInViewModel(apiService) }
                                CheckInScreen(
                                    viewModel = checkInViewModel, 
                                    onNavigateBack = { screenState = "home" }, 
                                    isOffline = isOffline,
                                    modifier = Modifier.padding(innerPadding)
                                )
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
                                    isOffline = isOffline,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            "history" -> {
                                val historyViewModel: HistoryViewModel = viewModel { HistoryViewModel(apiService) }
                                HistoryScreen(viewModel = historyViewModel, onNavigateBack = { screenState = "profile" }, modifier = Modifier.padding(innerPadding))
                            }
                            "settings" -> {
                                SettingsScreen(
                                    themeMode = appThemeMode,
                                    onThemeModeChange = onThemeModeChange,
                                    onNavigateBack = { screenState = "home" },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
