package com.example.vision100.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vision100.network.ApiService
import com.example.vision100.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _isFullyAuthenticated = mutableStateOf(false)
    val isFullyAuthenticated: State<Boolean> = _isFullyAuthenticated

    fun clearError() {
        _errorMessage.value = null
    }

    fun setError(message: String) {
        _isLoading.value = false
        _errorMessage.value = message
    }

    fun sendPasswordResetEmail(email: String, onComplete: (String?) -> Unit) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                repository.sendPasswordResetEmail(email.trim())
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
                repository.loginWithEmail(email.trim(), pass)
                syncAndComplete()
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = ApiService.parseError(e)
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _isLoading.value = true
        _errorMessage.value = null
        _isFullyAuthenticated.value = false
        
        viewModelScope.launch {
            try {
                repository.checkServerHealth()
                repository.loginWithGoogle(idToken)
                syncAndComplete()
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = ApiService.parseError(e)
            }
        }
    }

    private suspend fun syncAndComplete() {
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

    fun register(username: String, email: String, pass: String) {
        _isLoading.value = true
        _errorMessage.value = null
        _isFullyAuthenticated.value = false
        
        viewModelScope.launch {
            try {
                repository.checkServerHealth()
                repository.register(username, email.trim(), pass)
                syncAfterRegister(username, email.trim())
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = ApiService.parseError(e)
            }
        }
    }

    private suspend fun syncAfterRegister(username: String, email: String) {
        try {
            repository.syncUserAfterManualRegister(username, email)
            _isLoading.value = false
            _isFullyAuthenticated.value = true
        } catch (e: Exception) {
            _isLoading.value = false
            _errorMessage.value = ApiService.parseError(e)
            
            try {
                repository.deleteCurrentUser()
            } catch (deleteError: Exception) {
                repository.signOut()
            }
            _isFullyAuthenticated.value = false
        }
    }
}
