/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afrouzi.hamava.data.model.DubSettings
import com.afrouzi.hamava.data.model.DubTone
import com.afrouzi.hamava.domain.repository.SettingsRepositoryInterface
import com.afrouzi.hamava.domain.usecase.ApiKeyValidationResult
import com.afrouzi.hamava.domain.usecase.ValidateApiKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ValidationStatus {
    IDLE,
    VALIDATING,
    SUCCESS,
    FAILED
}

data class SettingsUiState(
    val settings: DubSettings = DubSettings(),
    val apiKeyInput: String = "",
    val fallbackKeysInput: String = "",
    val proxyTypeInput: String = "NONE",
    val proxyHostInput: String = "",
    val proxyPortInput: String = "",
    val validationStatus: ValidationStatus = ValidationStatus.IDLE,
    val validationMessage: String = "",
    val fallbackValidationStatus: ValidationStatus = ValidationStatus.IDLE,
    val fallbackValidationMessage: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepositoryInterface,
    private val validateApiKeyUseCase: ValidateApiKeyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { current ->
                    current.copy(
                        settings = settings,
                        apiKeyInput = if (current.apiKeyInput.isBlank()) settings.apiKey else current.apiKeyInput,
                        fallbackKeysInput = if (current.fallbackKeysInput.isBlank()) settings.fallbackApiKeys.joinToString("\n") else current.fallbackKeysInput,
                        proxyTypeInput = settings.proxyType,
                        proxyHostInput = if (current.proxyHostInput.isBlank()) settings.proxyHost else current.proxyHostInput,
                        proxyPortInput = if (current.proxyPortInput.isBlank() && settings.proxyPort > 0) settings.proxyPort.toString() else current.proxyPortInput
                    )
                }
            }
        }
    }

    fun onApiKeyChanged(newKey: String) {
        _uiState.update {
            it.copy(
                apiKeyInput = newKey,
                validationStatus = ValidationStatus.IDLE,
                validationMessage = ""
            )
        }
    }

    fun onFallbackKeysChanged(text: String) {
        _uiState.update {
            it.copy(
                fallbackKeysInput = text,
                fallbackValidationStatus = ValidationStatus.IDLE,
                fallbackValidationMessage = ""
            )
        }
    }

    fun onProxyHostChanged(host: String) {
        _uiState.update { it.copy(proxyHostInput = host) }
    }

    fun onProxyPortChanged(port: String) {
        _uiState.update { it.copy(proxyPortInput = port) }
    }

    fun onProxyTypeChanged(type: String) {
        _uiState.update { it.copy(proxyTypeInput = type) }
    }

    fun saveApiKey() {
        val key = _uiState.value.apiKeyInput.trim()
        viewModelScope.launch {
            settingsRepository.updateApiKey(key)
        }
    }

    fun saveFallbackKeys() {
        val keys = _uiState.value.fallbackKeysInput
            .split("\n", ",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        viewModelScope.launch {
            settingsRepository.updateFallbackApiKeys(keys)
        }
    }

    fun saveProxySettings() {
        val type = _uiState.value.proxyTypeInput
        val host = _uiState.value.proxyHostInput.trim()
        val port = _uiState.value.proxyPortInput.toIntOrNull() ?: 0
        viewModelScope.launch {
            settingsRepository.updateProxySettings(type, host, port)
        }
    }

    fun toggleFloatingOverlay(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEnableFloatingOverlay(enabled)
        }
    }

    fun toggleSubtitles(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEnableSubtitles(enabled)
        }
    }

    fun selectDubTone(tone: DubTone) {
        viewModelScope.launch {
            settingsRepository.updateDubTone(tone)
        }
    }

    fun toggleAec(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEnableAec(enabled)
        }
    }

    fun toggleSilenceSuppression(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSilenceSuppression(enabled)
        }
    }

    fun toggleLowLatencyMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateLowLatencyMode(enabled)
        }
    }

    fun updateOriginalVolume(volume: Float) {
        viewModelScope.launch {
            settingsRepository.updateOriginalAudioVolume(volume)
        }
    }

    fun testConnection() {
        val key = _uiState.value.apiKeyInput.trim()
        if (key.isBlank()) {
            _uiState.update {
                it.copy(
                    validationStatus = ValidationStatus.FAILED,
                    validationMessage = "Please enter an API key"
                )
            }
            return
        }

        _uiState.update { it.copy(validationStatus = ValidationStatus.VALIDATING) }

        viewModelScope.launch {
            when (val result = validateApiKeyUseCase(key)) {
                is ApiKeyValidationResult.Valid -> {
                    settingsRepository.updateApiKey(key)
                    _uiState.update {
                        it.copy(
                            validationStatus = ValidationStatus.SUCCESS,
                            validationMessage = "API Key is valid!"
                        )
                    }
                }
                is ApiKeyValidationResult.Invalid -> {
                    _uiState.update {
                        it.copy(
                            validationStatus = ValidationStatus.FAILED,
                            validationMessage = "Invalid API Key"
                        )
                    }
                }
                is ApiKeyValidationResult.NetworkFailure -> {
                    _uiState.update {
                        it.copy(
                            validationStatus = ValidationStatus.FAILED,
                            validationMessage = "Network error: ${result.error}"
                        )
                    }
                }
            }
        }
    }

    fun testFallbackKeys() {
        val keys = _uiState.value.fallbackKeysInput
            .split("\n", ",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (keys.isEmpty()) {
            _uiState.update {
                it.copy(
                    fallbackValidationStatus = ValidationStatus.FAILED,
                    fallbackValidationMessage = "هیچ کلید پشتیبانی وارد نشده است"
                )
            }
            return
        }

        _uiState.update { it.copy(fallbackValidationStatus = ValidationStatus.VALIDATING) }

        viewModelScope.launch {
            var validCount = 0
            for (key in keys) {
                if (validateApiKeyUseCase(key) is ApiKeyValidationResult.Valid) {
                    validCount++
                }
            }
            if (validCount > 0) {
                settingsRepository.updateFallbackApiKeys(keys)
                _uiState.update {
                    it.copy(
                        fallbackValidationStatus = ValidationStatus.SUCCESS,
                        fallbackValidationMessage = "$validCount کلید پشتیبان معتبر است"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        fallbackValidationStatus = ValidationStatus.FAILED,
                        fallbackValidationMessage = "هیچ‌کدام از کلیدها معتبر نیستند"
                    )
                }
            }
        }
    }

    fun selectModel(model: String) {
        viewModelScope.launch {
            settingsRepository.updateModel(model)
        }
    }

    fun selectAppTheme(theme: String) {
        viewModelScope.launch {
            settingsRepository.updateAppTheme(theme)
        }
    }

    fun selectAppLanguage(lang: String) {
        viewModelScope.launch {
            settingsRepository.updateAppLanguage(lang)
        }
    }
}
