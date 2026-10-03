/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.data.model

enum class DubStatus {
    IDLE,
    CONNECTING,
    ACTIVE_LISTENING,
    ACTIVE_SPEAKING,
    PAUSED,
    ERROR
}

sealed class DubError {
    object ApiKeyMissing : DubError()
    object ApiKeyInvalid : DubError()
    data class NetworkError(val message: String) : DubError()
    data class AudioRecordError(val message: String) : DubError()
    data class AudioTrackError(val message: String) : DubError()
    data class MediaProjectionError(val message: String) : DubError()
    data class UnknownError(val message: String) : DubError()

    fun getUserMessage(isPersian: Boolean): String {
        return when (this) {
            is ApiKeyMissing -> if (isPersian) "کلید Gemini API وارد نشده است" else "Gemini API key is missing"
            is ApiKeyInvalid -> if (isPersian) "کلید Gemini API نامعتبر است" else "Gemini API key is invalid"
            is NetworkError -> if (isPersian) "خطای شبکه یا اینترنت: $message" else "Network error: $message"
            is AudioRecordError -> if (isPersian) "خطا در ضبط صدا: $message" else "Audio record error: $message"
            is AudioTrackError -> if (isPersian) "خطا در پخش صدا: $message" else "Audio playback error: $message"
            is MediaProjectionError -> if (isPersian) "مجوز ضبط صدای سیستم صادر نشد" else "System audio capture permission denied"
            is UnknownError -> if (isPersian) "خطای ناشناخته: $message" else "Unknown error: $message"
        }
    }
}

data class DubUiState(
    val status: DubStatus = DubStatus.IDLE,
    val latencyMs: Long = 0L,
    val error: DubError? = null,
    val inputRms: Float = 0f,
    val outputRms: Float = 0f,
    val currentSettings: DubSettings = DubSettings(),
    val sessionDurationSeconds: Long = 0L
)
