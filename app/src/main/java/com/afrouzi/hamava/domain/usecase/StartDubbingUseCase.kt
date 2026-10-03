/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.afrouzi.hamava.core.utils.PermissionUtils
import com.afrouzi.hamava.data.model.AudioSourceType
import com.afrouzi.hamava.data.model.DubError
import com.afrouzi.hamava.domain.repository.SettingsRepositoryInterface
import com.afrouzi.hamava.service.DubForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

sealed class StartDubbingResult {
    object Success : StartDubbingResult()
    data class Failure(val error: DubError) : StartDubbingResult()
}

class StartDubbingUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepositoryInterface
) {

    operator fun invoke(): StartDubbingResult {
        val settings = settingsRepository.getSettingsSnapshot()

        if (settings.apiKey.isBlank()) {
            return StartDubbingResult.Failure(DubError.ApiKeyMissing)
        }

        if (settings.audioSource == AudioSourceType.MIC && !PermissionUtils.hasRecordAudioPermission(context)) {
            return StartDubbingResult.Failure(DubError.AudioRecordError("Microphone permission not granted"))
        }

        val intent = Intent(context, DubForegroundService::class.java).apply {
            action = DubForegroundService.ACTION_START
        }
        try {
            ContextCompat.startForegroundService(context, intent)
            return StartDubbingResult.Success
        } catch (e: Exception) {
            return StartDubbingResult.Failure(DubError.UnknownError(e.localizedMessage ?: "Failed to start service"))
        }
    }
}
