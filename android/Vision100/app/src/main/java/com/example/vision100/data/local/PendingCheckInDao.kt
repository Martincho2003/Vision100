package com.example.vision100.data.local

import androidx.room.*

@Dao
interface PendingCheckInDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(checkIn: PendingCheckIn)

    @Query("SELECT * FROM pending_checkins ORDER BY timestamp ASC")
    suspend fun getAllPending(): List<PendingCheckIn>

    @Query("DELETE FROM pending_checkins WHERE id = :id")
    suspend fun deleteById(id: Int)
}
