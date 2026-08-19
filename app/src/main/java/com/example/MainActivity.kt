package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Enforce RTL for full Persian experience
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    HabitBreakerApp()
                }
            }
        }
    }
}

@Composable
fun HabitBreakerApp(viewModel: HomeViewModel = viewModel()) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Crossfade(targetState = userProfile, label = "app_navigation") { profile ->
            if (profile == null || !profile.onboardingCompleted) {
                OnboardingScreen(
                    onComplete = { habitType, gender, durationUnit, durationValue, dailyFreq ->
                        viewModel.completeOnboarding(
                            habitType = habitType,
                            gender = gender,
                            durationUnit = durationUnit,
                            durationValue = durationValue,
                            dailyFrequency = dailyFreq
                        )
                    }
                )
            } else {
                HomeScreen(
                    viewModel = viewModel,
                    profile = profile
                )
            }
        }
    }
}
