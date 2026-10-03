/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.afrouzi.hamava.data.model.AudioSourceType
import com.afrouzi.hamava.data.model.DubLanguage
import com.afrouzi.hamava.data.model.DubSettings
import com.afrouzi.hamava.data.model.GeminiConstants
import com.afrouzi.hamava.data.model.GeminiVoice
import com.afrouzi.hamava.domain.repository.SettingsRepositoryInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SettingsRepositoryInterface {

    private val securePrefs: SharedPreferences by lazy {
        createSecurePreferences()
    }

    private val generalPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("hamava_general_prefs", Context.MODE_PRIVATE)
    }

    private val _settingsFlow = MutableStateFlow(readSettings())
    override val settingsFlow: Flow<DubSettings> = _settingsFlow.asStateFlow()

    private fun createSecurePreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "hamava_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to standard private preferences if Keystore unavailable
            context.getSharedPreferences("hamava_secure_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    private fun readSettings(): DubSettings {
        val savedApiKey = securePrefs.getString(KEY_API_KEY, "") ?: ""
        val apiKey = if (savedApiKey.isNotBlank()) {
            savedApiKey
        } else {
            val defaultKey = com.afrouzi.hamava.BuildConfig.DEFAULT_GEMINI_API_KEY
            if (defaultKey.isNotBlank()) {
                securePrefs.edit().putString(KEY_API_KEY, defaultKey).apply()
            }
            defaultKey
        }
        val targetLangCode = generalPrefs.getString(KEY_TARGET_LANG, "fa") ?: "fa"
        val voiceId = generalPrefs.getString(KEY_VOICE_ID, GeminiVoice.AOEDE.id) ?: GeminiVoice.AOEDE.id
        val savedModel = generalPrefs.getString(KEY_MODEL, GeminiConstants.DEFAULT_MODEL) ?: GeminiConstants.DEFAULT_MODEL
        val model = if (savedModel.contains("exp") || savedModel.contains("3.5") || savedModel.contains("2.0") || savedModel.isBlank()) {
            GeminiConstants.DEFAULT_MODEL
        } else {
            savedModel
        }
        val audioSourceStr = generalPrefs.getString(KEY_AUDIO_SOURCE, AudioSourceType.MIC.name) ?: AudioSourceType.MIC.name
        val appTheme = generalPrefs.getString(KEY_APP_THEME, "dark") ?: "dark"
        val appLang = generalPrefs.getString(KEY_APP_LANG, "fa") ?: "fa"
        val dubVolume = generalPrefs.getFloat(KEY_DUB_VOLUME, 0.85f)

        val audioSource = try {
            AudioSourceType.valueOf(audioSourceStr)
        } catch (e: Exception) {
            AudioSourceType.MIC
        }

        return DubSettings(
            apiKey = apiKey,
            targetLanguage = DubLanguage.findByCode(targetLangCode),
            sourceLanguage = "auto",
            voice = GeminiVoice.fromId(voiceId),
            model = model,
            audioSource = audioSource,
            appTheme = appTheme,
            appLanguage = appLang,
            dubVolumeRatio = dubVolume
        )
    }

    override fun getSettingsSnapshot(): DubSettings = _settingsFlow.value

    override suspend fun updateApiKey(apiKey: String) = withContext(ioDispatcher) {
        securePrefs.edit().putString(KEY_API_KEY, apiKey.trim()).apply()
        _settingsFlow.value = _settingsFlow.value.copy(apiKey = apiKey.trim())
    }

    override suspend fun updateTargetLanguage(language: DubLanguage) = withContext(ioDispatcher) {
        generalPrefs.edit().putString(KEY_TARGET_LANG, language.code).apply()
        _settingsFlow.value = _settingsFlow.value.copy(targetLanguage = language)
    }

    override suspend fun updateVoice(voice: GeminiVoice) = withContext(ioDispatcher) {
        generalPrefs.edit().putString(KEY_VOICE_ID, voice.id).apply()
        _settingsFlow.value = _settingsFlow.value.copy(voice = voice)
    }

    override suspend fun updateModel(model: String) = withContext(ioDispatcher) {
        generalPrefs.edit().putString(KEY_MODEL, model).apply()
        _settingsFlow.value = _settingsFlow.value.copy(model = model)
    }

    override suspend fun updateAudioSource(source: AudioSourceType) = withContext(ioDispatcher) {
        generalPrefs.edit().putString(KEY_AUDIO_SOURCE, source.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(audioSource = source)
    }

    override suspend fun updateDubVolumeRatio(ratio: Float) = withContext(ioDispatcher) {
        generalPrefs.edit().putFloat(KEY_DUB_VOLUME, ratio).apply()
        _settingsFlow.value = _settingsFlow.value.copy(dubVolumeRatio = ratio)
    }

    override suspend fun updateAppTheme(theme: String) = withContext(ioDispatcher) {
        generalPrefs.edit().putString(KEY_APP_THEME, theme).apply()
        _settingsFlow.value = _settingsFlow.value.copy(appTheme = theme)
    }

    override suspend fun updateAppLanguage(language: String) = withContext(ioDispatcher) {
        generalPrefs.edit().putString(KEY_APP_LANG, language).apply()
        _settingsFlow.value = _settingsFlow.value.copy(appLanguage = language)
    }

    companion object {
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_TARGET_LANG = "target_language"
        private const val KEY_VOICE_ID = "voice_id"
        private const val KEY_MODEL = "gemini_model"
        private const val KEY_AUDIO_SOURCE = "audio_source"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_APP_LANG = "app_language"
        private const val KEY_DUB_VOLUME = "dub_volume"
    }
}
