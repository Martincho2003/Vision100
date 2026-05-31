package com.example.vision100.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vision100.R
import com.example.vision100.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userData by viewModel.userData
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    val sessionExpired by viewModel.sessionExpired
    val canChangePassword by viewModel.canChangePassword
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val passwordsMismatchMessage = stringResource(R.string.passwords_mismatch)
    val passwordTooShortMessage = stringResource(R.string.password_too_short)
    val passwordChangedMessage = stringResource(R.string.password_changed_successfully)

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(Unit) {
        viewModel.fetchProfile()
    }

    LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            onLogout()
        }
    }

    if (showEditNameDialog) {
        var newName by remember { mutableStateOf(userData?.displayName ?: "") }
        var isUpdating by remember { mutableStateOf(false) }
        var dialogError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isUpdating) showEditNameDialog = false },
            title = { Text(stringResource(R.string.change_nickname)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { 
                            newName = it
                            dialogError = null 
                        },
                        label = { Text(stringResource(R.string.enter_new_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdating
                    )
                    if (dialogError != null) {
                        Text(
                            text = dialogError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isUpdating = true
                        viewModel.updateName(newName) { error ->
                            isUpdating = false
                            if (error == null) {
                                showEditNameDialog = false
                            } else {
                                dialogError = error
                            }
                        }
                    },
                    enabled = newName.isNotEmpty() && !isUpdating
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.save))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }, enabled = !isUpdating) { 
                    Text(stringResource(R.string.cancel)) 
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        var currentPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var isUpdating by remember { mutableStateOf(false) }
        var dialogError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isUpdating) showChangePasswordDialog = false },
            title = { Text(stringResource(R.string.change_password)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = {
                            currentPassword = it
                            dialogError = null
                        },
                        label = { Text(stringResource(R.string.current_password)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        enabled = !isUpdating
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            dialogError = null
                        },
                        label = { Text(stringResource(R.string.new_password)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        enabled = !isUpdating
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            dialogError = null
                        },
                        label = { Text(stringResource(R.string.confirm_new_password)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        enabled = !isUpdating
                    )
                    if (dialogError != null) {
                        Text(
                            text = dialogError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            newPassword.length < 6 -> dialogError = passwordTooShortMessage
                            newPassword != confirmPassword -> dialogError = passwordsMismatchMessage
                            else -> {
                                isUpdating = true
                                viewModel.changePassword(currentPassword, newPassword) { error ->
                                    isUpdating = false
                                    if (error == null) {
                                        showChangePasswordDialog = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar(passwordChangedMessage)
                                        }
                                    } else {
                                        dialogError = error
                                    }
                                }
                            }
                        }
                    },
                    enabled = currentPassword.isNotBlank() &&
                            newPassword.isNotBlank() &&
                            confirmPassword.isNotBlank() &&
                            !isUpdating
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.save))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }, enabled = !isUpdating) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(top = 0.dp)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onLogout) { Text("Re-login") }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = userData?.displayName ?: "User",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showEditNameDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Name",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Text(
                        text = userData?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(R.string.total_points), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "${userData?.totalPoints ?: 0}",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    ProfileMenuItem(
                        icon = Icons.AutoMirrored.Filled.ShowChart, 
                        text = stringResource(R.string.my_progress), 
                        onClick = onNavigateToHistory
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (canChangePassword) {
                        ProfileMenuItem(
                            icon = Icons.Default.Lock,
                            text = stringResource(R.string.change_password),
                            onClick = { showChangePasswordDialog = true }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    ProfileMenuItem(
                        icon = Icons.AutoMirrored.Filled.ExitToApp, 
                        text = stringResource(R.string.logout), 
                        onClick = {
                            viewModel.logout()
                            onLogout()
                        }, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = text, style = MaterialTheme.typography.bodyLarge, color = color)
        }
    }
}
