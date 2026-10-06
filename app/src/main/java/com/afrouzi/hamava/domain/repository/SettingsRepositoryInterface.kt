/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.domain.repository

import com.afrouzi.hamava.data.model.AudioSourceType
import com.afrouzi.hamava.data.model.DubLanguage
import com.afrouzi.hamava.data.model.DubSettings
import com.afrouzi.hamava.data.model.DubTone
import com.afrouzi.hamava.data.model.GeminiVoice
import kotlinx.coroutines.flow.Flow

interface SettingsRepositoryInterface {
    val settingsFlow: Flow<DubSettings>
    fun getSettingsSnapshot(): DubSettings
    suspend fun updateApiKey(apiKey: String)
    suspend fun updateFallbackApiKeys(keys: List<String>)
    suspend fun updateTargetLanguage(language: DubLanguage)
    suspend fun updateVoice(voice: GeminiVoice)
    suspend fun updateModel(model: String)
    suspend fun updateAudioSource(source: AudioSourceType)
    suspend fun updateDubVolumeRatio(ratio: Float)
    suspend fun updateOriginalAudioVolume(volume: Float)
    suspend fun updateLowLatencyMode(enable: Boolean)
    suspend fun updateAppTheme(theme: String)
    suspend fun updateAppLanguage(language: String)
    suspend fun updateEnableFloatingOverlay(enable: Boolean)
    suspend fun updateEnableSubtitles(enable: Boolean)
    suspend fun updateDubTone(tone: DubTone)
    suspend fun updateEnableAec(enable: Boolean)
    suspend fun updateSilenceSuppression(enable: Boolean)
    suspend fun updateProxySettings(type: String, host: String, port: Int)
}
