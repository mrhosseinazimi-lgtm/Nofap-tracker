package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.CharacterStage
import com.example.data.model.DurationUnit
import com.example.data.model.Gender
import com.example.data.model.HabitType
import com.example.data.repository.HabitRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: HabitRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = HabitRepository(db.habitDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testAppNameString() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ترک عادت", appName)
    }

    @Test
    fun test21DaysCleansingTimeline() {
        val totalSpots = 70

        // Day 0
        assertEquals(0, repository.calculateCleansedSpotsForDay(totalSpots, 0))

        // Day 1 (gentle start)
        val day1Cleansed = repository.calculateCleansedSpotsForDay(totalSpots, 1)
        assertTrue(day1Cleansed in 1..3)

        // Day 7 (end of week 1)
        val day7Cleansed = repository.calculateCleansedSpotsForDay(totalSpots, 7)
        assertTrue(day7Cleansed in 8..15)

        // Day 14 (end of week 2)
        val day14Cleansed = repository.calculateCleansedSpotsForDay(totalSpots, 14)
        assertTrue(day14Cleansed in 30..40)

        // Day 20 (must not be fully cleansed before day 21)
        val day20Cleansed = repository.calculateCleansedSpotsForDay(totalSpots, 20)
        assertTrue("Day 20 should leave some spots remaining", day20Cleansed < totalSpots)
        assertTrue(day20Cleansed >= 60)

        // Day 21 (Exactly 100% complete!)
        val day21Cleansed = repository.calculateCleansedSpotsForDay(totalSpots, 21)
        assertEquals(totalSpots, day21Cleansed)

        // Day 22+ (Stays 100% complete)
        val day22Cleansed = repository.calculateCleansedSpotsForDay(totalSpots, 22)
        assertEquals(totalSpots, day22Cleansed)
    }

    @Test
    fun test24HourCooldownRestriction() = runBlocking {
        repository.setupInitialProfile(
            habitType = HabitType.SMOKING,
            gender = Gender.MALE,
            durationUnit = DurationUnit.YEAR,
            durationValue = 2,
            dailyFrequency = 10.0
        )

        val profile = repository.getProfile()
        assertNotNull(profile)
        assertTrue(profile!!.initialTotalSpots in 50..90)
        assertTrue("Check-in should be allowed initially", repository.canCheckInSuccess(profile))

        // Perform first check-in
        val day1 = repository.logSuccessfulDay("روز اول")
        assertNotNull(day1)
        assertEquals(1, day1!!.currentStreakDays)
        assertTrue(day1.lastSuccessTimestamp > 0L)

        // Immediately checking again must be blocked by 24h cooldown
        assertFalse("Second check-in within 24h must be blocked", repository.canCheckInSuccess(day1))
        val remainingMs = repository.getRemainingCooldownMillis(day1)
        assertTrue(remainingMs > 0L)

        // Attempting another check-in while in cooldown does not advance streak
        val blockedAttempt = repository.logSuccessfulDay("تلاش مجدد")
        assertEquals(1, blockedAttempt?.currentStreakDays)
    }

    @Test
    fun testCharacterStageProgression() {
        assertEquals(CharacterStage.STAGE_1_TIRED, CharacterStage.fromProgress(0.0f, 1))
        assertEquals(CharacterStage.STAGE_1_TIRED, CharacterStage.fromProgress(0.08f, 3))
        assertEquals(CharacterStage.STAGE_2_HOPEFUL, CharacterStage.fromProgress(0.12f, 6))
        assertEquals(CharacterStage.STAGE_3_HAPPY, CharacterStage.fromProgress(0.35f, 12))
        assertEquals(CharacterStage.STAGE_4_JUMPING, CharacterStage.fromProgress(0.70f, 18))
        assertEquals(CharacterStage.STAGE_5_DANCING, CharacterStage.fromProgress(1.0f, 21))
    }
}
