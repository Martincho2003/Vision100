package com.example.vision100.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vision100.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val apiService: ApiService) : ViewModel() {
    private val _isOffline = MutableStateFlow(false)
    val isOffline = _isOffline.asStateFlow()

    private val _isCheckingHealth = MutableStateFlow(true)
    val isCheckingHealth = _isCheckingHealth.asStateFlow()

    fun checkHealth() {
        viewModelScope.launch {
            _isCheckingHealth.value = true
            try {
                apiService.checkHealth(ApiService.getLanguageHeader())
                _isOffline.value = false
            } catch (e: Exception) {
                Log.d("MainViewModel", "Server unreachable: ${e.message}")
                _isOffline.value = true
            } finally {
                _isCheckingHealth.value = false
            }
        }
    }
}
