package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val habitType: String,
    val gender: String,
    val durationUnit: String,
    val durationValue: Int,
    val historyDays: Int,
    val dailyFrequency: Double,
    val estimatedTotalEvents: Int,
    val initialTotalSpots: Int,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val totalCleanDays: Int = 0,
    val totalScore: Int = 0,
    val cleansedSpots: Int = 0,
    val lastCheckInDate: String? = null,
    val lastSuccessTimestamp: Long = 0L, // 24-hour strict check-in timestamp
    val onboardingCompleted: Boolean = false,
    val createdAtTimestamp: Long = System.currentTimeMillis()
)
