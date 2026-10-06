/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.ui.screens.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afrouzi.hamava.data.model.AudioSourceType
import com.afrouzi.hamava.data.model.DubError
import com.afrouzi.hamava.data.model.DubLanguage
import com.afrouzi.hamava.data.model.DubStatus
import com.afrouzi.hamava.data.model.DubTone
import com.afrouzi.hamava.data.model.DubUiState
import com.afrouzi.hamava.data.model.GeminiVoice
import com.afrouzi.hamava.domain.repository.SettingsRepositoryInterface
import com.afrouzi.hamava.domain.usecase.StartDubbingResult
import com.afrouzi.hamava.domain.usecase.StartDubbingUseCase
import com.afrouzi.hamava.domain.usecase.StopDubbingUseCase
import com.afrouzi.hamava.service.DubForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepositoryInterface,
    private val startDubbingUseCase: StartDubbingUseCase,
    private val stopDubbingUseCase: StopDubbingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DubUiState())
    val uiState: StateFlow<DubUiState> = _uiState.asStateFlow()

    private val _errorEvents = MutableSharedFlow<DubError>(extraBufferCapacity = 1)
    val errorEvents: SharedFlow<DubError> = _errorEvents.asSharedFlow()

    init {
        // Observe Settings
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(currentSettings = settings) }
            }
        }

        // Observe Service Status & Latency & RMS
        viewModelScope.launch {
            combine(
                DubForegroundService.statusFlow,
                DubForegroundService.latencyFlow,
                DubForegroundService.inputRmsFlow,
                DubForegroundService.outputRmsFlow
            ) { status, latency, inputRms, outputRms ->
                Quad(status, latency, inputRms, outputRms)
            }.collect { quad ->
                _uiState.update { current ->
                    current.copy(
                        status = quad.first,
                        latencyMs = quad.second,
                        inputRms = quad.third,
                        outputRms = quad.fourth
                    )
                }
            }
        }

        // Observe Subtitles
        viewModelScope.launch {
            DubForegroundService.subtitleFlow.collect { sub ->
                _uiState.update { it.copy(subtitle = sub) }
            }
        }

        // Observe Errors
        viewModelScope.launch {
            DubForegroundService.errorFlow.collect { err ->
                _uiState.update { it.copy(error = err, status = DubStatus.ERROR) }
            }
        }
    }

    fun toggleDubbing() {
        val currentStatus = _uiState.value.status
        when (currentStatus) {
            DubStatus.ACTIVE_LISTENING, DubStatus.ACTIVE_SPEAKING -> {
                pauseDubbing()
            }
            DubStatus.PAUSED -> {
                resumeDubbing()
            }
            DubStatus.CONNECTING -> {
                stopDubbing()
            }
            else -> {
                val result = startDubbingUseCase()
                if (result is StartDubbingResult.Failure) {
                    _errorEvents.tryEmit(result.error)
                    _uiState.update { it.copy(error = result.error, status = DubStatus.ERROR) }
                }
            }
        }
    }

    fun pauseDubbing() {
        val intent = Intent(context, DubForegroundService::class.java).apply {
            action = DubForegroundService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun resumeDubbing() {
        val intent = Intent(context, DubForegroundService::class.java).apply {
            action = DubForegroundService.ACTION_RESUME
        }
        context.startService(intent)
    }

    fun stopDubbing() {
        stopDubbingUseCase()
    }

    fun selectLanguage(language: DubLanguage) {
        viewModelScope.launch {
            settingsRepository.updateTargetLanguage(language)
        }
    }

    fun selectAudioSource(source: AudioSourceType) {
        viewModelScope.launch {
            settingsRepository.updateAudioSource(source)
        }
    }

    fun selectVoice(voice: GeminiVoice) {
        viewModelScope.launch {
            settingsRepository.updateVoice(voice)
        }
    }

    fun selectDubTone(tone: DubTone) {
        viewModelScope.launch {
            settingsRepository.updateDubTone(tone)
        }
    }

    fun toggleFloatingOverlay(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEnableFloatingOverlay(enabled)
        }
    }

    fun updateOriginalVolume(volume: Float) {
        viewModelScope.launch {
            settingsRepository.updateOriginalAudioVolume(volume)
        }
    }

    fun updateVolume(volume: Float) {
        viewModelScope.launch {
            settingsRepository.updateDubVolumeRatio(volume)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null, status = DubStatus.IDLE) }
    }

    private data class Quad<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}
