package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileDirect(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profile")
    suspend fun clearProfile()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLog(log: DailyLogEntity): Long

    @Query("SELECT * FROM daily_logs ORDER BY timestamp DESC")
    fun getAllDailyLogsFlow(): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs ORDER BY timestamp DESC LIMIT 30")
    suspend fun getRecentLogs(): List<DailyLogEntity>

    @Query("DELETE FROM daily_logs")
    suspend fun clearAllLogs()
}
