package com.example.vision100.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vision100.data.TouristObject
import com.example.vision100.repository.ObjectRepository
import kotlinx.coroutines.launch

class ObjectViewModel(private val repository: ObjectRepository) : ViewModel() {
    
    private val _objects = mutableStateOf<List<TouristObject>>(emptyList())
    val objects: State<List<TouristObject>> = _objects

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    fun fetchObjects() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                _objects.value = repository.getTouristObjects()
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = e.localizedMessage ?: "Failed to load objects"
            }
        }
    }
}
