package com.example.data.repository

import com.example.data.local.DailyLogEntity
import com.example.data.local.HabitDao
import com.example.data.local.UserProfileEntity
import com.example.data.model.CharacterStage
import com.example.data.model.DurationUnit
import com.example.data.model.Gender
import com.example.data.model.HabitType
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

class HabitRepository(private val habitDao: HabitDao) {

    companion object {
        const val COOLDOWN_24_HOURS_MS: Long = 24L * 60L * 60L * 1000L
        const val TARGET_DAYS: Int = 21
    }

    val userProfileFlow: Flow<UserProfileEntity?> = habitDao.getUserProfileFlow()
    val dailyLogsFlow: Flow<List<DailyLogEntity>> = habitDao.getAllDailyLogsFlow()

    suspend fun getProfile(): UserProfileEntity? = habitDao.getUserProfileDirect()

    /**
     * Calculates initial visual spots based on user's history and frequency.
     * Guaranteed balanced range: 50 to 90 spots for clear, granular daily visibility.
     */
    fun calculateInitialSpots(historyDays: Int, dailyFrequency: Double): Int {
        val totalEvents = (historyDays * dailyFrequency).coerceAtLeast(1.0)
        val logFactor = ln(1.0 + (totalEvents / 20.0))
        val rawSpots = 50 + (logFactor * 8.0).toInt()
        return rawSpots.coerceIn(50, 90)
    }

    /**
     * Checks if the 24-hour window has passed since the last success check-in.
     */
    fun canCheckInSuccess(profile: UserProfileEntity): Boolean {
        if (profile.lastSuccessTimestamp <= 0L) return true
        val elapsed = System.currentTimeMillis() - profile.lastSuccessTimestamp
        return elapsed >= COOLDOWN_24_HOURS_MS
    }

    /**
     * Returns the remaining cooldown milliseconds until the next success check-in is allowed.
     */
    fun getRemainingCooldownMillis(profile: UserProfileEntity): Long {
        if (profile.lastSuccessTimestamp <= 0L) return 0L
        val elapsed = System.currentTimeMillis() - profile.lastSuccessTimestamp
        val remaining = COOLDOWN_24_HOURS_MS - elapsed
        return remaining.coerceAtLeast(0L)
    }

    /**
     * Initializes user profile upon onboarding completion.
     */
    suspend fun setupInitialProfile(
        habitType: HabitType,
        gender: Gender,
        durationUnit: DurationUnit,
        durationValue: Int,
        dailyFrequency: Double
    ) {
        val safeDuration = durationValue.coerceAtLeast(1)
        val historyDays = safeDuration * durationUnit.daysMultiplier
        val estimatedEvents = (historyDays * dailyFrequency).roundToInt().coerceAtLeast(1)
        val initialSpots = calculateInitialSpots(historyDays, dailyFrequency)

        val entity = UserProfileEntity(
            id = 1,
            habitType = habitType.id,
            gender = gender.id,
            durationUnit = durationUnit.id,
            durationValue = safeDuration,
            historyDays = historyDays,
            dailyFrequency = dailyFrequency,
            estimatedTotalEvents = estimatedEvents,
            initialTotalSpots = initialSpots,
            currentStreakDays = 0,
            bestStreakDays = 0,
            totalCleanDays = 0,
            totalScore = 0,
            cleansedSpots = 0,
            lastCheckInDate = null,
            lastSuccessTimestamp = 0L,
            onboardingCompleted = true,
            createdAtTimestamp = System.currentTimeMillis()
        )
        habitDao.insertOrUpdateProfile(entity)
    }

    /**
     * Calculates the exact cumulative cleansed spots for a given streak day on the 21-day timeline.
     * - Day 1-6 (Early): gentle slow cleansing
     * - Day 7-15 (Mid): increasing pace
     * - Day 16-20 (Late): rapid accelerated cleansing
     * - End of Day 21: Exactly 100% of all spots cleansed.
     */
    fun calculateCleansedSpotsForDay(totalSpots: Int, streakDay: Int): Int {
        if (streakDay <= 0) return 0
        if (streakDay >= TARGET_DAYS) return totalSpots

        // Power curve normalization across 21 days: (d / 21)^1.75
        val progressRatio = (streakDay.toDouble() / TARGET_DAYS.toDouble()).pow(1.75)
        val calculated = (progressRatio * totalSpots).roundToInt()

        // Ensure at least 1 spot is cleansed on day 1, and not fully cleansed before day 21
        return calculated.coerceIn(1, totalSpots - 1)
    }

    /**
     * Logs a successful clean day.
     * Enforces the 24-hour interval restriction permanently in local database.
     */
    suspend fun logSuccessfulDay(note: String? = null): UserProfileEntity? {
        val profile = habitDao.getUserProfileDirect() ?: return null

        // 24-hour cooldown validation
        if (!canCheckInSuccess(profile)) {
            return profile
        }

        val nowTimestamp = System.currentTimeMillis()
        val todayStr = getCurrentDateString()

        val newStreak = profile.currentStreakDays + 1
        val newBestStreak = maxOf(profile.bestStreakDays, newStreak)
        val newTotalClean = profile.totalCleanDays + 1

        val newCleansedSpots = calculateCleansedSpotsForDay(profile.initialTotalSpots, newStreak)
        val cleansedUnitsThisTime = (newCleansedSpots - profile.cleansedSpots).coerceAtLeast(1)

        val scoreGain = (10 + (newStreak * 5) + (cleansedUnitsThisTime * 10))
        val newTotalScore = profile.totalScore + scoreGain

        val updatedProfile = profile.copy(
            currentStreakDays = newStreak,
            bestStreakDays = newBestStreak,
            totalCleanDays = newTotalClean,
            totalScore = newTotalScore,
            cleansedSpots = newCleansedSpots,
            lastCheckInDate = todayStr,
            lastSuccessTimestamp = nowTimestamp
        )

        habitDao.insertOrUpdateProfile(updatedProfile)

        val log = DailyLogEntity(
            date = todayStr,
            isSuccess = true,
            streakAtDay = newStreak,
            cleansedUnitsToday = cleansedUnitsThisTime,
            note = note,
            timestamp = nowTimestamp
        )
        habitDao.insertDailyLog(log)

        return updatedProfile
    }

    /**
     * Logs a difficult / relapse day with compassionate non-judgmental tracking.
     * Resets current streak gently, recalculates spots based on streak 0, preserves best streak and total score.
     */
    suspend fun logRelapseDay(reason: String, note: String? = null): UserProfileEntity? {
        val profile = habitDao.getUserProfileDirect() ?: return null
        val nowTimestamp = System.currentTimeMillis()
        val todayStr = getCurrentDateString()

        // When restarting, character gently returns to initial stage for renewed 21-day journey
        val updatedProfile = profile.copy(
            currentStreakDays = 0,
            cleansedSpots = 0,
            lastCheckInDate = todayStr,
            lastSuccessTimestamp = 0L // Reset cooldown on relapse so user can begin new streak when ready
        )

        habitDao.insertOrUpdateProfile(updatedProfile)

        val log = DailyLogEntity(
            date = todayStr,
            isSuccess = false,
            streakAtDay = 0,
            cleansedUnitsToday = 0,
            relapseReason = reason,
            note = note,
            timestamp = nowTimestamp
        )
        habitDao.insertDailyLog(log)

        return updatedProfile
    }

    /**
     * Updates gender or habit if user switches in settings.
     */
    suspend fun updateSettings(habitType: HabitType? = null, gender: Gender? = null) {
        val profile = habitDao.getUserProfileDirect() ?: return
        val updated = profile.copy(
            habitType = habitType?.id ?: profile.habitType,
            gender = gender?.id ?: profile.gender
        )
        habitDao.insertOrUpdateProfile(updated)
    }

    /**
     * Resets all user progress and returns to Onboarding.
     */
    suspend fun resetAllData() {
        habitDao.clearProfile()
        habitDao.clearAllLogs()
    }

    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
