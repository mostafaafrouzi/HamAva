/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afrouzi.hamava.data.model.DubSettings
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
    val validationStatus: ValidationStatus = ValidationStatus.IDLE,
    val validationMessage: String = ""
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
                        apiKeyInput = if (current.apiKeyInput.isBlank()) settings.apiKey else current.apiKeyInput
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

    fun saveApiKey() {
        val key = _uiState.value.apiKeyInput.trim()
        viewModelScope.launch {
            settingsRepository.updateApiKey(key)
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
