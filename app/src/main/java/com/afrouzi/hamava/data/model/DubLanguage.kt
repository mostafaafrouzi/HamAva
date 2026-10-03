/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.data.model

data class DubLanguage(
    val code: String,
    val nameEn: String,
    val nameFa: String,
    val flag: String
) {
    companion object {
        val PERSIAN = DubLanguage("fa", "Persian", "فارسی", "🇮🇷")
        val ENGLISH = DubLanguage("en", "English", "انگلیسی", "🇺🇸")
        val ARABIC = DubLanguage("ar", "Arabic", "عربی", "🇸🇦")
        val TURKISH = DubLanguage("tr", "Turkish", "ترکی استانبولی", "🇹🇷")
        val FRENCH = DubLanguage("fr", "French", "فرانسوی", "🇫🇷")
        val GERMAN = DubLanguage("de", "German", "آلمانی", "🇩🇪")
        val SPANISH = DubLanguage("es", "Spanish", "اسپانیایی", "🇪🇸")
        val ITALIAN = DubLanguage("it", "Italian", "ایتالیایی", "🇮🇹")
        val RUSSIAN = DubLanguage("ru", "Russian", "روسی", "🇷🇺")
        val CHINESE = DubLanguage("zh", "Chinese (Mandarin)", "چینی", "🇨🇳")
        val JAPANESE = DubLanguage("ja", "Japanese", "ژاپنی", "🇯🇵")
        val KOREAN = DubLanguage("ko", "Korean", "کره‌ای", "🇰🇷")
        val HINDI = DubLanguage("hi", "Hindi", "هندی", "🇮🇳")
        val PORTUGUESE = DubLanguage("pt", "Portuguese", "پرتغالی", "🇧🇷")
        val DUTCH = DubLanguage("nl", "Dutch", "هلندی", "🇳🇱")
        val SWEDISH = DubLanguage("sv", "Swedish", "سوئدی", "🇸🇪")
        val POLISH = DubLanguage("pl", "Polish", "لهستانی", "🇵🇱")
        val UKRAINIAN = DubLanguage("uk", "Ukrainian", "اوکراینی", "🇺🇦")
        val INDONESIAN = DubLanguage("id", "Indonesian", "اندونزیایی", "🇮🇩")
        val URDU = DubLanguage("ur", "Urdu", "اردو", "🇵🇰")
        val AZERBAIJANI = DubLanguage("az", "Azerbaijani", "ترکی آذربایجانی", "🇦🇿")
        val KURDISH = DubLanguage("ku", "Kurdish", "کردی", "🇮🇷")
        val PASHTO = DubLanguage("ps", "Pashto", "پشتو", "🇦🇫")

        val SUPPORTED_LANGUAGES = listOf(
            PERSIAN,
            ENGLISH,
            ARABIC,
            TURKISH,
            FRENCH,
            GERMAN,
            SPANISH,
            ITALIAN,
            RUSSIAN,
            CHINESE,
            JAPANESE,
            KOREAN,
            HINDI,
            PORTUGUESE,
            DUTCH,
            SWEDISH,
            POLISH,
            UKRAINIAN,
            INDONESIAN,
            URDU,
            AZERBAIJANI,
            KURDISH,
            PASHTO
        )

        fun findByCode(code: String): DubLanguage {
            return SUPPORTED_LANGUAGES.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: PERSIAN
        }
    }
}
