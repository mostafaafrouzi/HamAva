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
import com.afrouzi.hamava.data.model.DubTone
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

        val rawFallbackKeys = securePrefs.getString(KEY_FALLBACK_KEYS, "") ?: ""
        val fallbackKeys = if (rawFallbackKeys.isNotBlank()) {
            rawFallbackKeys.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            emptyList()
        }

        val targetLangCode = generalPrefs.getString(KEY_TARGET_LANG, "fa") ?: "fa"
        val voiceId = generalPrefs.getString(KEY_VOICE_ID, GeminiVoice.AOEDE.id) ?: GeminiVoice.AOEDE.id
        val savedModel = generalPrefs.getString(KEY_MODEL, GeminiConstants.DEFAULT_MODEL) ?: GeminiConstants.DEFAULT_MODEL
        val model = if (savedModel.contains("exp") || savedModel.contains("2.0") || savedModel.isBlank()) {
            GeminiConstants.DEFAULT_MODEL
        } else {
            savedModel
        }
        val audioSourceStr = generalPrefs.getString(KEY_AUDIO_SOURCE, AudioSourceType.MIC.name) ?: AudioSourceType.MIC.name
        val appTheme = generalPrefs.getString(KEY_APP_THEME, "dark") ?: "dark"
        val appLang = generalPrefs.getString(KEY_APP_LANG, "fa") ?: "fa"
        val dubVolume = generalPrefs.getFloat(KEY_DUB_VOLUME, 1.0f)
        val originalVolume = generalPrefs.getFloat(KEY_ORIGINAL_AUDIO_VOLUME, 0.20f)
        val lowLatency = generalPrefs.getBoolean(KEY_LOW_LATENCY_MODE, true)
        val enableOverlay = generalPrefs.getBoolean(KEY_ENABLE_FLOATING_OVERLAY, true)
        val enableSubtitles = generalPrefs.getBoolean(KEY_ENABLE_SUBTITLES, true)
        val toneId = generalPrefs.getString(KEY_DUB_TONE, DubTone.COLLOQUIAL.id) ?: DubTone.COLLOQUIAL.id
        val enableAec = generalPrefs.getBoolean(KEY_ENABLE_AEC, true)
        val silenceSuppression = generalPrefs.getBoolean(KEY_SILENCE_SUPPRESSION, true)
        val proxyType = generalPrefs.getString(KEY_PROXY_TYPE, "NONE") ?: "NONE"
        val proxyHost = generalPrefs.getString(KEY_PROXY_HOST, "") ?: ""
        val proxyPort = generalPrefs.getInt(KEY_PROXY_PORT, 0)

        val audioSource = try {
            AudioSourceType.valueOf(audioSourceStr)
        } catch (e: Exception) {
            AudioSourceType.MIC
        }

        return DubSettings(
            apiKey = apiKey,
            fallbackApiKeys = fallbackKeys,
            targetLanguage = DubLanguage.findByCode(targetLangCode),
            sourceLanguage = "auto",
            voice = GeminiVoice.fromId(voiceId),
            model = model,
            audioSource = audioSource,
            appTheme = appTheme,
            appLanguage = appLang,
            dubVolumeRatio = dubVolume,
            originalAudioVolume = originalVolume,
            lowLatencyMode = lowLatency,
            enableFloatingOverlay = enableOverlay,
            enableSubtitles = enableSubtitles,
            dubTone = DubTone.fromId(toneId),
            enableAec = enableAec,
            silenceSuppression = silenceSuppression,
            proxyType = proxyType,
            proxyHost = proxyHost,
            proxyPort = proxyPort
        )
    }

    override fun getSettingsSnapshot(): DubSettings = _settingsFlow.value

    override suspend fun updateApiKey(apiKey: String) = withContext(ioDispatcher) {
        securePrefs.edit().putString(KEY_API_KEY, apiKey.trim()).apply()
        _settingsFlow.value = _settingsFlow.value.copy(apiKey = apiKey.trim())
    }

    override suspend fun updateFallbackApiKeys(keys: List<String>) = withContext(ioDispatcher) {
        val serialized = keys.joinToString(",")
        securePrefs.edit().putString(KEY_FALLBACK_KEYS, serialized).apply()
        _settingsFlow.value = _settingsFlow.value.copy(fallbackApiKeys = keys)
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

    override suspend fun updateEnableFloatingOverlay(enable: Boolean) = withContext(ioDispatcher) {
        generalPrefs.edit().putBoolean(KEY_ENABLE_FLOATING_OVERLAY, enable).apply()
        _settingsFlow.value = _settingsFlow.value.copy(enableFloatingOverlay = enable)
    }

    override suspend fun updateEnableSubtitles(enable: Boolean) = withContext(ioDispatcher) {
        generalPrefs.edit().putBoolean(KEY_ENABLE_SUBTITLES, enable).apply()
        _settingsFlow.value = _settingsFlow.value.copy(enableSubtitles = enable)
    }

    override suspend fun updateDubTone(tone: DubTone) = withContext(ioDispatcher) {
        generalPrefs.edit().putString(KEY_DUB_TONE, tone.id).apply()
        _settingsFlow.value = _settingsFlow.value.copy(dubTone = tone)
    }

    override suspend fun updateEnableAec(enable: Boolean) = withContext(ioDispatcher) {
        generalPrefs.edit().putBoolean(KEY_ENABLE_AEC, enable).apply()
        _settingsFlow.value = _settingsFlow.value.copy(enableAec = enable)
    }

    override suspend fun updateSilenceSuppression(enable: Boolean) = withContext(ioDispatcher) {
        generalPrefs.edit().putBoolean(KEY_SILENCE_SUPPRESSION, enable).apply()
        _settingsFlow.value = _settingsFlow.value.copy(silenceSuppression = enable)
    }

    override suspend fun updateProxySettings(type: String, host: String, port: Int) = withContext(ioDispatcher) {
        generalPrefs.edit()
            .putString(KEY_PROXY_TYPE, type)
            .putString(KEY_PROXY_HOST, host.trim())
            .putInt(KEY_PROXY_PORT, port)
            .apply()
        _settingsFlow.value = _settingsFlow.value.copy(
            proxyType = type,
            proxyHost = host.trim(),
            proxyPort = port
        )
    }

    override suspend fun updateOriginalAudioVolume(volume: Float) = withContext(ioDispatcher) {
        generalPrefs.edit().putFloat(KEY_ORIGINAL_AUDIO_VOLUME, volume).apply()
        _settingsFlow.value = _settingsFlow.value.copy(originalAudioVolume = volume)
    }

    override suspend fun updateLowLatencyMode(enable: Boolean) = withContext(ioDispatcher) {
        generalPrefs.edit().putBoolean(KEY_LOW_LATENCY_MODE, enable).apply()
        _settingsFlow.value = _settingsFlow.value.copy(lowLatencyMode = enable)
    }

    companion object {
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_FALLBACK_KEYS = "gemini_fallback_keys"
        private const val KEY_TARGET_LANG = "target_language"
        private const val KEY_VOICE_ID = "voice_id"
        private const val KEY_MODEL = "gemini_model"
        private const val KEY_AUDIO_SOURCE = "audio_source"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_APP_LANG = "app_language"
        private const val KEY_DUB_VOLUME = "dub_volume"
        private const val KEY_ORIGINAL_AUDIO_VOLUME = "original_audio_volume"
        private const val KEY_LOW_LATENCY_MODE = "low_latency_mode"
        private const val KEY_ENABLE_FLOATING_OVERLAY = "enable_floating_overlay"
        private const val KEY_ENABLE_SUBTITLES = "enable_subtitles"
        private const val KEY_DUB_TONE = "dub_tone"
        private const val KEY_ENABLE_AEC = "enable_aec"
        private const val KEY_SILENCE_SUPPRESSION = "silence_suppression"
        private const val KEY_PROXY_TYPE = "proxy_type"
        private const val KEY_PROXY_HOST = "proxy_host"
        private const val KEY_PROXY_PORT = "proxy_port"
    }
}
