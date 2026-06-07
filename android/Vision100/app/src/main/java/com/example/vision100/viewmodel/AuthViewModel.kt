package com.example.vision100.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vision100.network.ApiService
import com.example.vision100.repository.AuthRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _isFullyAuthenticated = mutableStateOf(false)
    val isFullyAuthenticated: State<Boolean> = _isFullyAuthenticated

    fun clearError() {
        _errorMessage.value = null
    }

    fun sendPasswordResetEmail(email: String, onComplete: (String?) -> Unit) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email.trim()).await()
                onComplete(null)
            } catch (e: Exception) {
                onComplete(e.localizedMessage ?: "Password reset failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        _isLoading.value = true
        _errorMessage.value = null
        _isFullyAuthenticated.value = false
        
        viewModelScope.launch {
            try {
                repository.checkServerHealth()
                auth.signInWithEmailAndPassword(email.trim(), pass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            syncAndComplete()
                        } else {
                            _isLoading.value = false
                            _errorMessage.value = task.exception?.localizedMessage ?: "Login failed"
                        }
                    }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = ApiService.parseError(e)
            }
        }
    }

    fun signInWithCredential(credential: AuthCredential) {
        _isLoading.value = true
        _errorMessage.value = null
        _isFullyAuthenticated.value = false
        
        viewModelScope.launch {
            try {
                repository.checkServerHealth()
                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        syncAndComplete()
                    } else {
                        _isLoading.value = false
                        _errorMessage.value = task.exception?.localizedMessage ?: "Google sign-in failed"
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = ApiService.parseError(e)
            }
        }
    }

    private fun syncAndComplete() {
        viewModelScope.launch {
            try {
                repository.syncUserWithBackend()
                _isLoading.value = false
                _isFullyAuthenticated.value = true
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = ApiService.parseError(e)
                repository.signOut()
                _isFullyAuthenticated.value = false
            }
        }
    }

    fun register(username: String, email: String, pass: String) {
        _isLoading.value = true
        _errorMessage.value = null
        _isFullyAuthenticated.value = false
        
        viewModelScope.launch {
            try {
                repository.checkServerHealth()
                auth.createUserWithEmailAndPassword(email.trim(), pass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                                displayName = username
                            }
                            user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    syncAfterRegister(username, email)
                                } else {
                                    _isLoading.value = false
                                    _errorMessage.value = profileTask.exception?.localizedMessage
                                    repository.signOut()
                                }
                            }
                        } else {
                            _isLoading.value = false
                            _errorMessage.value = task.exception?.localizedMessage ?: "Registration failed"
                        }
                    }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = ApiService.parseError(e)
            }
        }
    }

    private fun syncAfterRegister(username: String, email: String) {
        viewModelScope.launch {
            try {
                repository.syncUserAfterManualRegister(username, email)
                _isLoading.value = false
                _isFullyAuthenticated.value = true
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = ApiService.parseError(e)
                val user = auth.currentUser
                user?.delete()?.addOnCompleteListener {
                    repository.signOut()
                    _isFullyAuthenticated.value = false
                }
            }
        }
    }
}
