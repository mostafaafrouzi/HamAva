/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.domain.repository

import com.afrouzi.hamava.data.model.AudioSourceType
import com.afrouzi.hamava.data.model.DubLanguage
import com.afrouzi.hamava.data.model.DubSettings
import com.afrouzi.hamava.data.model.GeminiVoice
import kotlinx.coroutines.flow.Flow

interface SettingsRepositoryInterface {
    val settingsFlow: Flow<DubSettings>
    fun getSettingsSnapshot(): DubSettings
    suspend fun updateApiKey(apiKey: String)
    suspend fun updateTargetLanguage(language: DubLanguage)
    suspend fun updateVoice(voice: GeminiVoice)
    suspend fun updateModel(model: String)
    suspend fun updateAudioSource(source: AudioSourceType)
    suspend fun updateDubVolumeRatio(ratio: Float)
    suspend fun updateAppTheme(theme: String)
    suspend fun updateAppLanguage(language: String)
}
