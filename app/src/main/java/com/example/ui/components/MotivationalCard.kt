package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.HabitType

@Composable
fun MotivationalCard(
    habitType: HabitType,
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    val quotes = remember(habitType, streakDays) {
        when (habitType) {
            HabitType.SMOKING -> listOf(
                "هر نفسی که بدون دود فرو می‌دهی، هزاران سلول ریه را به زندگی برمی‌گرداند.",
                "بزرگ‌ترین پیروزی، تسلط بر خواسته‌های زودگذر به خاطر آرامش پایدار است.",
                "شما قوی‌تر از هر میل گذرایی هستید. هر روز یک گام به رهایی نزدیک‌تر می‌شوید.",
                "با هر نخ سیگاری که نمی‌کشی، انرژی، سلامتی و ثروتت را حفظ می‌کنی.",
                "به شخصیتت نگاه کن؛ او هر روز با اراده فولادین تو پاک‌تر و شاداب‌تر می‌شود."
            )
            HabitType.MASTURBATION -> listOf(
                "انرژی حیاتی تو باارزش‌ترین سرمایه برای ساختن آینده و موفقیت توست.",
                "وسوسه مانند موج دریاست؛ اوج می‌گیرد و می‌گذرد، تو نظاره‌گر آرام ساحل باش.",
                "عزت نفس واقعی در لحظاتی ساخته می‌شود که به لذت‌های گذرا «نه» می‌گویی.",
                "مغز و گیرنده‌های دوپامین تو در حال بازسازی و درمان هستند، به روند اعتماد کن.",
                "هر روز پاکی، نگاه تو را شفاف‌تر، ذهنت را متمرکزتر و روحت را سبک‌تر می‌کند."
            )
        }
    }

    val selectedQuote = remember(streakDays) {
        quotes[streakDays % quotes.size]
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("motivational_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = "نقل قول",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "پیام انگیزشی روز",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = selectedQuote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }
            }
        }
    }
}
