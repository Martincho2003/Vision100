package com.example.vision100.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_checkins")
data class PendingCheckIn(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val photoPath: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long = System.currentTimeMillis()
)
