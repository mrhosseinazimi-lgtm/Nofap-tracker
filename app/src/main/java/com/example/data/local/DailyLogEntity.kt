package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // ISO YYYY-MM-DD
    val isSuccess: Boolean,
    val streakAtDay: Int,
    val cleansedUnitsToday: Int,
    val relapseReason: String? = null,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
