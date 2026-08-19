package com.example.data.model

enum class HabitType(
    val id: String,
    val titleFa: String,
    val subtitleFa: String,
    val iconEmoji: String,
    val unitLabelFa: String
) {
    SMOKING(
        id = "SMOKING",
        titleFa = "ترک سیگار",
        subtitleFa = "پاکسازی ریه‌ها، بازگشت تنفس پاک و صرفه‌جویی مالی",
        iconEmoji = "🚬",
        unitLabelFa = "نخ در روز"
    ),
    MASTURBATION(
        id = "MASTURBATION",
        titleFa = "ترک خودارضایی",
        subtitleFa = "بازگرداندن تمرکز ذهن، انرژی حیاتی و عزت نفس پایدار",
        iconEmoji = "🌿",
        unitLabelFa = "بار در روز"
    );

    companion object {
        fun fromId(id: String?): HabitType =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SMOKING
    }
}

enum class Gender(
    val id: String,
    val titleFa: String,
    val emoji: String
) {
    MALE("MALE", "مرد", "🧔"),
    FEMALE("FEMALE", "زن", "👩");

    companion object {
        fun fromId(id: String?): Gender =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MALE
    }
}

enum class DurationUnit(
    val id: String,
    val titleFa: String,
    val daysMultiplier: Int
) {
    YEAR("YEAR", "سال", 365),
    MONTH("MONTH", "ماه", 30),
    DAY("DAY", "روز", 1);

    companion object {
        fun fromId(id: String?): DurationUnit =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MONTH
    }
}

enum class CharacterStage(
    val stageNumber: Int,
    val titleFa: String,
    val moodDescFa: String,
    val dayRangeFa: String,
    val requiredProgressRatio: Float
) {
    STAGE_1_TIRED(
        stageNumber = 1,
        titleFa = "مرحله ۱: آغاز مسیر",
        moodDescFa = "خسته و سنگین، اما آماده برای تحول ۲۱ روزه",
        dayRangeFa = "روزهای ۱ تا ۴",
        requiredProgressRatio = 0.0f
    ),
    STAGE_2_HOPEFUL(
        stageNumber = 2,
        titleFa = "مرحله ۲: جوانه‌های امید",
        moodDescFa = "افزایش انرژی اولیه و حس سبکی در تنفس و ذهن",
        dayRangeFa = "روزهای ۵ تا ۹",
        requiredProgressRatio = 0.10f
    ),
    STAGE_3_HAPPY(
        stageNumber = 3,
        titleFa = "مرحله ۳: احساس سرزندگی",
        moodDescFa = "لبخند پایدار، شادابی چهره و تقویت انگیزه",
        dayRangeFa = "روزهای ۱۰ تا ۱۵",
        requiredProgressRatio = 0.28f
    ),
    STAGE_4_JUMPING(
        stageNumber = 4,
        titleFa = "مرحله ۴: اوج انگیزه و جهش",
        moodDescFa = "پرش‌های پرانرژی و شتاب به سوی رهایی کامل",
        dayRangeFa = "روزهای ۱۶ تا ۲۰",
        requiredProgressRatio = 0.58f
    ),
    STAGE_5_DANCING(
        stageNumber = 5,
        titleFa = "مرحله ۵: رهایی و جشن پیروزی ۲۱ روزه",
        moodDescFa = "پاکی ۱۰۰٪، رقص پیروزی، هاله زرین و تولد دوباره",
        dayRangeFa = "روز ۲۱ به بعد",
        requiredProgressRatio = 1.0f
    );

    companion object {
        fun fromProgress(ratio: Float, streakDays: Int = 0): CharacterStage {
            return when {
                ratio >= 1.0f || streakDays >= 21 -> STAGE_5_DANCING
                ratio >= 0.58f || streakDays >= 16 -> STAGE_4_JUMPING
                ratio >= 0.28f || streakDays >= 10 -> STAGE_3_HAPPY
                ratio >= 0.10f || streakDays >= 5 -> STAGE_2_HOPEFUL
                else -> STAGE_1_TIRED
            }
        }
    }
}
