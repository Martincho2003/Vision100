package com.example.vision100.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vision100.data.UpdateNameRequest
import com.example.vision100.data.UserResponse
import com.example.vision100.network.ApiService
import com.example.vision100.repository.AuthRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _userData = mutableStateOf<UserResponse?>(null)
    val userData: State<UserResponse?> = _userData

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _sessionExpired = mutableStateOf(false)
    val sessionExpired: State<Boolean> = _sessionExpired

    private val _canChangePassword = mutableStateOf(false)
    val canChangePassword: State<Boolean> = _canChangePassword

    fun fetchProfile() {
        _isLoading.value = true
        _errorMessage.value = null
        _canChangePassword.value = authRepository.canCurrentUserChangePassword()
        viewModelScope.launch {
            try {
                val token = ApiService.getAuthHeader()
                val lang = ApiService.getLanguageHeader()
                _userData.value = apiService.getMe(token, lang)
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                val errorMsg = ApiService.parseError(e)
                if (e is retrofit2.HttpException && e.code() == 401) {
                    _sessionExpired.value = true
                } else {
                    _errorMessage.value = errorMsg
                }
            }
        }
    }

    fun updateName(newName: String, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val token = ApiService.getAuthHeader()
                val lang = ApiService.getLanguageHeader()
                _userData.value = apiService.updateName(token, lang, UpdateNameRequest(newName))
                onComplete(null)
            } catch (e: Exception) {
                onComplete(ApiService.parseError(e))
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.changePassword(currentPassword, newPassword)
                onComplete(null)
            } catch (e: Exception) {
                onComplete(e.localizedMessage ?: "Password update failed")
            }
        }
    }

    fun logout() {
        authRepository.signOut()
    }
}
