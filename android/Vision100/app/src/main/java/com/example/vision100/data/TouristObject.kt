package com.example.vision100.data

import com.google.gson.annotations.SerializedName

data class TouristObject(
    val id: Int,
    val number: String,
    val name: String,
    val description: String?,
    val region: String?,
    val category: String?,
    val latitude: Float,
    val longitude: Float,
    @SerializedName("ai_labels") val aiLabels: String?,
    @SerializedName("is_visited") val isVisited: Int = 0,
)
