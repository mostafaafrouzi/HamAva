/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.data.model

enum class AudioSourceType {
    MIC,
    SYSTEM
}

data class GeminiVoice(
    val id: String,
    val displayName: String,
    val description: String,
    val nameFa: String = displayName
) {
    companion object {
        val AOEDE = GeminiVoice("Aoede", "Aoede", "Clear, natural, warm (Female)", "آئوده (زن)")
        val CHARON = GeminiVoice("Charon", "Charon", "Deep, resonant, calm (Male)", "شارون (مرد)")
        val FENRIR = GeminiVoice("Fenrir", "Fenrir", "Strong, authorative (Male)", "فنریر (مرد)")
        val KORE = GeminiVoice("Kore", "Kore", "Friendly, energetic (Female)", "کوره (زن)")
        val PUCK = GeminiVoice("Puck", "Puck", "Playful, lively (Male)", "پوک (مرد)")
        val ZEPHYR = GeminiVoice("Zephyr", "Zephyr", "Gentle, balanced (Neutral)", "زفیر (خنثی)")

        val ALL_VOICES = listOf(AOEDE, CHARON, FENRIR, KORE, PUCK, ZEPHYR)

        fun fromId(id: String): GeminiVoice {
            return ALL_VOICES.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AOEDE
        }
    }
}

object GeminiConstants {
    const val DEFAULT_MODEL = "models/gemini-3.5-live-translate-preview"
    const val FALLBACK_MODEL = "models/gemini-3.8-live"

    val AVAILABLE_MODELS = listOf(
        DEFAULT_MODEL to "Gemini 3.5 Live Translate (مخصوص ترجمه همزمان)",
        FALLBACK_MODEL to "Gemini 3.8 Live (مدل چندمنظوره)",
        "models/gemini-3.1-flash-live-preview" to "Gemini 3.1 Flash Live"
    )

    const val LIVE_API_WS_URL =
        "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"

    const val SAMPLE_RATE_IN = 16000
    const val SAMPLE_RATE_OUT = 24000
    const val CHUNK_MS = 160
    const val CHUNK_BYTES = (SAMPLE_RATE_IN * 2 * CHUNK_MS) / 1000 // 5120 bytes for 160ms 16kHz 16-bit mono
}

data class DubSettings(
    val apiKey: String = "",
    val targetLanguage: DubLanguage = DubLanguage.PERSIAN,
    val sourceLanguage: String = "auto",
    val voice: GeminiVoice = GeminiVoice.AOEDE,
    val model: String = GeminiConstants.DEFAULT_MODEL,
    val audioSource: AudioSourceType = AudioSourceType.MIC,
    val appTheme: String = "dark",
    val appLanguage: String = "fa",
    val dubVolumeRatio: Float = 0.85f
)
