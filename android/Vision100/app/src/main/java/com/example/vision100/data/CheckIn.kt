package com.example.vision100.data

import com.google.gson.annotations.SerializedName

data class CheckInResponse(
    val verified: Boolean,
    val reason: String,
    @SerializedName("already_visited") val alreadyVisited: Boolean,
    @SerializedName("points_awarded") val pointsAwarded: Int,
    @SerializedName("total_points") val totalPoints: Int,
    @SerializedName("distance_meters") val distanceMeters: Double,
    @SerializedName("ai_confidence") val aiConfidence: Double,
    @SerializedName("ai_matched_label") val aiMatchedLabel: String?,
    @SerializedName("object") val touristObject: TouristObject?,
    val detections: List<Detection>?
)

data class Detection(
    val label: String,
    val score: Double,
    val source: String
)

data class LeaderboardUser(
    val id: Int,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("total_points") val totalPoints: Int
)

data class VisitResponse(
    val id: Int,
    @SerializedName("points_awarded") val pointsAwarded: Int,
    @SerializedName("photo_url") val photoUrl: String?,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("visited_at") val visitedAt: String? = null,
    @SerializedName("tourist_object") val touristObject: TouristObject? = null
)
