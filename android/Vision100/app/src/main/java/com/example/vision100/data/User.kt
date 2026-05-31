package com.example.vision100.data

import com.google.gson.annotations.SerializedName

data class UserRegisterRequest(
    @SerializedName("display_name") val displayName: String?,
    val email: String?
)

data class UserResponse(
    val id: Int,
    @SerializedName("firebase_uid") val firebaseUid: String,
    val email: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("total_points") val totalPoints: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class UpdateNameRequest(
    @SerializedName("display_name") val displayName: String
)
