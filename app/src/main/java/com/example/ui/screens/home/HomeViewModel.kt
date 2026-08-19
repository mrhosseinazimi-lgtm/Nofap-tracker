package com.example.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DailyLogEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.DurationUnit
import com.example.data.model.Gender
import com.example.data.model.HabitType
import com.example.data.repository.HabitRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class HomeUiEvent {
    data class ShowSuccessCelebration(
        val streakDays: Int,
        val cleansedUnits: Int,
        val totalCleansed: Int,
        val totalSpots: Int
    ) : HomeUiEvent()
    data class ShowMessage(val message: String) : HomeUiEvent()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = HabitRepository(db.habitDao())
    }

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val dailyLogs: StateFlow<List<DailyLogEntity>> = repository.dailyLogsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _eventFlow = MutableSharedFlow<HomeUiEvent>()
    val eventFlow: SharedFlow<HomeUiEvent> = _eventFlow.asSharedFlow()

    fun completeOnboarding(
        habitType: HabitType,
        gender: Gender,
        durationUnit: DurationUnit,
        durationValue: Int,
        dailyFrequency: Double
    ) {
        viewModelScope.launch {
            repository.setupInitialProfile(
                habitType = habitType,
                gender = gender,
                durationUnit = durationUnit,
                durationValue = durationValue,
                dailyFrequency = dailyFrequency
            )
        }
    }

    fun canCheckIn(profile: UserProfileEntity): Boolean {
        return repository.canCheckInSuccess(profile)
    }

    fun getRemainingCooldown(profile: UserProfileEntity): Long {
        return repository.getRemainingCooldownMillis(profile)
    }

    fun logSuccessfulDay(note: String? = null) {
        viewModelScope.launch {
            val current = repository.getProfile() ?: return@launch
            if (!repository.canCheckInSuccess(current)) {
                val remainingMs = repository.getRemainingCooldownMillis(current)
                val hours = remainingMs / (1000 * 60 * 60)
                val minutes = (remainingMs / (1000 * 60)) % 60
                _eventFlow.emit(
                    HomeUiEvent.ShowMessage("ثبت موفقیت بعدی: $hours ساعت و $minutes دقیقه دیگر فعال می‌شود.")
                )
                return@launch
            }

            val updated = repository.logSuccessfulDay(note)
            if (updated != null) {
                val cleansedThisTime = updated.cleansedSpots - current.cleansedSpots
                _eventFlow.emit(
                    HomeUiEvent.ShowSuccessCelebration(
                        streakDays = updated.currentStreakDays,
                        cleansedUnits = cleansedThisTime.coerceAtLeast(1),
                        totalCleansed = updated.cleansedSpots,
                        totalSpots = updated.initialTotalSpots
                    )
                )
            }
        }
    }

    fun logRelapseDay(reason: String, note: String? = null) {
        viewModelScope.launch {
            repository.logRelapseDay(reason, note)
            _eventFlow.emit(HomeUiEvent.ShowMessage("شروع دوباره با اراده‌ای قوی‌تر! شما می‌توانید."))
        }
    }

    fun updateSettings(habitType: HabitType? = null, gender: Gender? = null) {
        viewModelScope.launch {
            repository.updateSettings(habitType, gender)
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
        }
    }
}
