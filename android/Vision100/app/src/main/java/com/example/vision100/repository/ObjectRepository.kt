package com.example.vision100.repository

import com.example.vision100.data.TouristObject
import com.example.vision100.network.ApiService

class ObjectRepository(private val apiService: ApiService) {
    suspend fun getTouristObjects(): List<TouristObject> {
        return apiService.getObjects(ApiService.getLanguageHeader())
    }
}
