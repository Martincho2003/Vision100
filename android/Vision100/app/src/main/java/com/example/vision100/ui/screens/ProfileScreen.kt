package com.example.vision100.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.vision100.R
import com.example.vision100.data.UserResponse
import com.example.vision100.ui.components.FlagAccentBar
import com.example.vision100.ui.components.VisionBackground
import com.example.vision100.ui.components.VisionEmptyState
import com.example.vision100.ui.components.VisionLoadingState
import com.example.vision100.ui.components.VisionStatTile
import com.example.vision100.ui.components.VisionTopBar
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
            VisionTopBar(
                title = stringResource(R.string.profile_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = "Back",
                onNavigationClick = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        VisionBackground {
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (isLoading) {
                    VisionLoadingState(
                        title = stringResource(R.string.profile_title),
                        message = stringResource(R.string.my_progress),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (errorMessage != null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        VisionEmptyState(
                            icon = Icons.Default.PersonOff,
                            title = stringResource(R.string.error_title),
                            message = errorMessage!!
                        )
                        Button(onClick = onLogout, modifier = Modifier.padding(top = 8.dp)) {
                            Text(stringResource(R.string.retry_login))
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ProfileHero(
                            user = userData,
                            onEditName = { showEditNameDialog = true }
                        )

                        VisionStatTile(
                            label = stringResource(R.string.total_points),
                            value = "${userData?.totalPoints ?: 0}",
                            icon = Icons.Default.Stars,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProfileMenuItem(
                                icon = Icons.AutoMirrored.Filled.ShowChart,
                                text = stringResource(R.string.my_progress),
                                onClick = onNavigateToHistory
                            )
                            if (canChangePassword) {
                                ProfileMenuItem(
                                    icon = Icons.Default.Lock,
                                    text = stringResource(R.string.change_password),
                                    onClick = { showChangePasswordDialog = true }
                                )
                            }
                            ProfileMenuItem(
                                icon = Icons.AutoMirrored.Filled.ExitToApp,
                                text = stringResource(R.string.logout),
                                onClick = {
                                    viewModel.logout()
                                    onLogout()
                                },
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHero(
    user: UserResponse?,
    onEditName: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            FlagAccentBar(height = 4.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(92.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                ) {
                    if (!user?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = user?.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(20.dp).fillMaxSize()
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Празно пространство за балансиране на иконата за редакция и точно центриране на името
                    Spacer(modifier = Modifier.width(48.dp))
                    
                    Text(
                        text = user?.displayName ?: "User",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = onEditName,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Text(
                    text = user?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
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
        color = if (color == MaterialTheme.colorScheme.error) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.48f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = color,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = color.copy(alpha = 0.7f)
            )
        }
    }
}
