package com.example.vision100.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.vision100.R
import com.example.vision100.viewmodel.AuthViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showResetPasswordDialog by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    val isFullyAuthenticated by viewModel.isFullyAuthenticated

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val enterEmailMessage = stringResource(R.string.enter_email_for_reset)
    val resetEmailSentMessage = stringResource(R.string.password_reset_email_sent)

    LaunchedEffect(isFullyAuthenticated) {
        if (isFullyAuthenticated) {
            onLoginSuccess()
        }
    }

    fun handleGoogleSignIn() {
        viewModel.clearError()
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("932457770837-7g5jql83jmp8v1iv1hmf43c0v5cgeob1.apps.googleusercontent.com")
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    viewModel.signInWithCredential(firebaseCredential)
                }
            } catch (e: Exception) {

            }
        }
    }

    if (showResetPasswordDialog) {
        var resetEmail by remember { mutableStateOf(email) }
        var dialogError by remember { mutableStateOf<String?>(null) }
        var isResetting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isResetting) showResetPasswordDialog = false },
            title = { Text(stringResource(R.string.reset_password_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.reset_password_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = {
                            resetEmail = it
                            dialogError = null
                        },
                        label = { Text(stringResource(R.string.email_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        enabled = !isResetting
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
                        if (resetEmail.isBlank()) {
                            dialogError = enterEmailMessage
                        } else {
                            isResetting = true
                            viewModel.sendPasswordResetEmail(resetEmail) { error ->
                                isResetting = false
                                if (error == null) {
                                    showResetPasswordDialog = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar(resetEmailSentMessage)
                                    }
                                } else {
                                    dialogError = error
                                }
                            }
                        }
                    },
                    enabled = !isResetting
                ) {
                    if (isResetting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.send_reset_email))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetPasswordDialog = false },
                    enabled = !isResetting
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password_label)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showResetPasswordDialog = true }, enabled = !isLoading) {
                    Text(stringResource(R.string.forgot_password))
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.signInWithEmail(email, password) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = email.isNotEmpty() && password.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.login_button))
                }
            }

            TextButton(onClick = onNavigateToRegister, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.register_link))
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { handleGoogleSignIn() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.google_sign_in))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}
