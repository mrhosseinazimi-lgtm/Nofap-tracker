package com.example.data.model

data class Achievement(
    val id: String,
    val titleFa: String,
    val descFa: String,
    val requiredDays: Int,
    val iconEmoji: String,
    val colorHex: Long = 0xFF4CAF50
) {
    companion object {
        val ALL_ACHIEVEMENTS = listOf(
            Achievement("day_1", "گام نخست", "ثبت موفق اولین ۲۴ ساعت پاکی", 1, "🌱", 0xFF81C784),
            Achievement("day_3", "سه‌گانه اراده", "عبور از سخت‌ترین موج‌های اولیه", 3, "🔥", 0xFFFFB74D),
            Achievement("day_7", "هفته طلایی", "یک هفته استواری و بازسازی انرژی", 7, "⭐", 0xFFFFD54F),
            Achievement("day_10", "ده روزه افتخار", "کاهش محسوس تمایلات وسوسه‌انگیز", 10, "🛡️", 0xFF4DD0E1),
            Achievement("day_14", "دو هفته قهرمانی", "پاکسازی نیمی از لکه‌های سابقه عادت", 14, "💎", 0xFF64B5F6),
            Achievement("day_21", "قانون ۲۱ روزه", "تثبیت الگوهای رفتاری و مسیرهای عصبی نو", 21, "🏆", 0xFFBA68C8),
            Achievement("day_30", "ماه پیروزی", "یک ماه تمام کنترل زندگی در دستان شما", 30, "👑", 0xFFFF8A65),
            Achievement("day_60", "شصت روز رهایی", "استحکام روحی و درخشش فوق‌العاده شخصیت", 60, "🌟", 0xFF4DB6AC),
            Achievement("day_90", "تولد دوباره (۹۰ روز)", "بازسازی کامل گیرنده‌های دوپامین و آرامش درون", 90, "🦅", 0xFFFFD700)
        )
    }
}
