package com.example.vision100.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vision100.data.VisitResponse
import com.example.vision100.network.ApiService
import kotlinx.coroutines.launch

class HistoryViewModel(private val apiService: ApiService) : ViewModel() {
    
    private val _visits = mutableStateOf<List<VisitResponse>>(emptyList())
    val visits: State<List<VisitResponse>> = _visits

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    fun fetchHistory() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val token = ApiService.getAuthHeader()
                val lang = ApiService.getLanguageHeader()
                _visits.value = apiService.getMyVisits(token, lang)
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = ApiService.parseError(e)
            }
        }
    }
}
