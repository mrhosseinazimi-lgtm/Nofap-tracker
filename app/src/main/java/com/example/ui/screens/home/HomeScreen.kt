package com.example.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.UserProfileEntity
import com.example.data.model.CharacterStage
import com.example.data.model.Gender
import com.example.data.model.HabitType
import com.example.data.repository.HabitRepository
import com.example.ui.components.AchievementsDialog
import com.example.ui.components.AnimatedCharacterAvatar
import com.example.ui.components.CheckInSuccessDialog
import com.example.ui.components.CleanseProgressBar
import com.example.ui.components.HistoryDialog
import com.example.ui.components.MotivationalCard
import com.example.ui.components.RelapseSupportDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.StatCard
import com.example.ui.components.UrgeSurfingDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    profile: UserProfileEntity
) {
    val dailyLogs by viewModel.dailyLogs.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var successDialogData by remember { mutableStateOf<HomeUiEvent.ShowSuccessCelebration?>(null) }
    var showRelapseDialog by remember { mutableStateOf(false) }
    var showUrgeSurfingDialog by remember { mutableStateOf(false) }
    var showAchievementsDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Live 24-Hour Cooldown Timer
    var remainingCooldownMillis by remember { mutableLongStateOf(0L) }

    LaunchedEffect(profile.lastSuccessTimestamp) {
        while (true) {
            val now = System.currentTimeMillis()
            val elapsed = now - profile.lastSuccessTimestamp
            val remaining = (HabitRepository.COOLDOWN_24_HOURS_MS - elapsed).coerceAtLeast(0L)
            remainingCooldownMillis = if (profile.lastSuccessTimestamp > 0L) remaining else 0L
            if (remaining <= 0L && profile.lastSuccessTimestamp > 0L) {
                break
            }
            delay(1000L)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is HomeUiEvent.ShowSuccessCelebration -> {
                    successDialogData = event
                    showSuccessDialog = true
                }
                is HomeUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    val habitType = HabitType.fromId(profile.habitType)
    val gender = Gender.fromId(profile.gender)

    val progressRatio = (profile.cleansedSpots.toFloat() / profile.initialTotalSpots.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val currentStage = CharacterStage.fromProgress(progressRatio, profile.currentStreakDays)

    val isCheckInAllowed = remainingCooldownMillis <= 0L

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ترک عادت",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${habitType.iconEmoji} ${habitType.titleFa}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // SOS Breathing button
                    IconButton(
                        onClick = { showUrgeSurfingDialog = true },
                        modifier = Modifier.testTag("sos_breathing_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelfImprovement,
                            contentDescription = "آرامش فوری و تنفس SOS",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Achievements button
                    IconButton(
                        onClick = { showAchievementsDialog = true },
                        modifier = Modifier.testTag("achievements_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "دستاوردها",
                            tint = Color(0xFFFFD700)
                        )
                    }

                    // History button
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "تاریخچه",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Settings button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "تنظیمات",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
                .testTag("home_screen_content"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stage Mood Banner with 21-day timeline guidance
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Column {
                        Text(
                            text = currentStage.moodDescFa,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = "مسیر تحول ۲۱ روزه • هدف: پاکسازی کامل در پایان روز ۲۱",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Central Animated Character Frame
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("character_container_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedCharacterAvatar(
                        gender = gender,
                        stage = currentStage,
                        initialTotalSpots = profile.initialTotalSpots,
                        cleansedSpots = profile.cleansedSpots,
                        size = 250.dp
                    )

                    CleanseProgressBar(
                        totalSpots = profile.initialTotalSpots,
                        cleansedSpots = profile.cleansedSpots,
                        stage = currentStage,
                        currentStreak = profile.currentStreakDays
                    )
                }
            }

            // Action Buttons Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Action Button: "امروز موفق بودم" with strict 24-hour lock & countdown
                if (isCheckInAllowed) {
                    Button(
                        onClick = { viewModel.logSuccessfulDay() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("check_in_success_button"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "امروز موفق بودم ✓",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                } else {
                    // Locked Button with 24h Countdown Display
                    val totalSec = remainingCooldownMillis / 1000
                    val h = totalSec / 3600
                    val m = (totalSec % 3600) / 60
                    val s = totalSec % 60
                    val countdownText = if (h > 0) {
                        "ثبت موفقیت بعدی: $h ساعت و $m دقیقه دیگر"
                    } else if (m > 0) {
                        "ثبت موفقیت بعدی: $m دقیقه و $s ثانیه دیگر"
                    } else {
                        "ثبت موفقیت بعدی: $s ثانیه دیگر"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("check_in_locked_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = countdownText,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Secondary Action: Relapse / Difficult Day
                OutlinedButton(
                    onClick = { showRelapseDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("log_relapse_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = "امروز سخت بود / نیاز به شروع دوباره",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Core Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    titleFa = "روزهای پاکی",
                    value = "${profile.currentStreakDays}",
                    unitFa = "از ۲۱ روز",
                    iconEmoji = "🔥",
                    accentColor = Color(0xFFF97316),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    titleFa = "بهترین رکورد",
                    value = "${profile.bestStreakDays}",
                    unitFa = "روز",
                    iconEmoji = "🏆",
                    accentColor = Color(0xFFFFD700),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    titleFa = "امتیاز پاکی",
                    value = "${profile.totalScore}",
                    unitFa = "امتیاز",
                    iconEmoji = "⭐",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                val totalAvoided = (profile.totalCleanDays * profile.dailyFrequency).toInt()
                StatCard(
                    titleFa = if (habitType == HabitType.SMOKING) "سیگار دوری‌شده" else "دفعات کنترل‌شده",
                    value = "$totalAvoided",
                    unitFa = if (habitType == HabitType.SMOKING) "نخ" else "بار",
                    iconEmoji = if (habitType == HabitType.SMOKING) "🚭" else "🛡️",
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Daily Motivation Card
            MotivationalCard(
                habitType = habitType,
                streakDays = profile.currentStreakDays
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Dialogs
    if (showSuccessDialog && successDialogData != null) {
        val data = successDialogData!!
        CheckInSuccessDialog(
            streakDays = data.streakDays,
            cleansedUnits = data.cleansedUnits,
            totalCleansed = data.totalCleansed,
            totalSpots = data.totalSpots,
            onDismiss = {
                showSuccessDialog = false
                successDialogData = null
            }
        )
    }

    if (showRelapseDialog) {
        RelapseSupportDialog(
            onConfirmRelapse = { reason, note ->
                viewModel.logRelapseDay(reason, note)
                showRelapseDialog = false
            },
            onDismiss = { showRelapseDialog = false }
        )
    }

    if (showUrgeSurfingDialog) {
        UrgeSurfingDialog(
            onDismiss = { showUrgeSurfingDialog = false }
        )
    }

    if (showAchievementsDialog) {
        AchievementsDialog(
            currentStreakDays = profile.currentStreakDays,
            bestStreakDays = profile.bestStreakDays,
            onDismiss = { showAchievementsDialog = false }
        )
    }

    if (showHistoryDialog) {
        HistoryDialog(
            logs = dailyLogs,
            onDismiss = { showHistoryDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentHabitType = habitType,
            currentGender = gender,
            onUpdateHabit = { newHabit -> viewModel.updateSettings(habitType = newHabit) },
            onUpdateGender = { newGender -> viewModel.updateSettings(gender = newGender) },
            onResetAllData = {
                viewModel.resetAllData()
                showSettingsDialog = false
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}
