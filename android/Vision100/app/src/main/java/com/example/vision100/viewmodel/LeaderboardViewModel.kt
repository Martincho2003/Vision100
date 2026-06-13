package com.example.vision100.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vision100.data.LeaderboardUser
import com.example.vision100.network.ApiService
import kotlinx.coroutines.launch

class LeaderboardViewModel(private val apiService: ApiService) : ViewModel() {
    
    private val _users = mutableStateOf<List<LeaderboardUser>>(emptyList())
    val users: State<List<LeaderboardUser>> = _users

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    fun fetchLeaderboard() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val token = ApiService.getAuthHeader()
                val lang = ApiService.getLanguageHeader()
                _users.value = apiService.getLeaderboard(token, lang)
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = ApiService.parseError(e)
            }
        }
    }
}
