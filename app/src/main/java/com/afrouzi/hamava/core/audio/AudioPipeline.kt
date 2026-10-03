/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.core.audio

import android.content.Context
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import com.afrouzi.hamava.core.gemini.GeminiLiveSession
import com.afrouzi.hamava.data.model.AudioSourceType
import com.afrouzi.hamava.data.model.DubError
import com.afrouzi.hamava.data.model.DubSettings
import com.afrouzi.hamava.data.model.DubStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class AudioPipeline(
    private var settings: DubSettings,
    private val mediaProjection: MediaProjection? = null,
    private val context: Context? = null
) {

    private val pipelineScope = CoroutineScope(Dispatchers.IO)

    private val _status = MutableStateFlow(DubStatus.IDLE)
    val status: StateFlow<DubStatus> = _status.asStateFlow()

    private val _latency = MutableStateFlow(0L)
    val latency: StateFlow<Long> = _latency.asStateFlow()

    private val _inputRms = MutableStateFlow(0f)
    val inputRms: StateFlow<Float> = _inputRms.asStateFlow()

    private val _outputRms = MutableStateFlow(0f)
    val outputRms: StateFlow<Float> = _outputRms.asStateFlow()

    private val _errors = MutableSharedFlow<DubError>(replay = 1)
    val errors: SharedFlow<DubError> = _errors.asSharedFlow()

    private var geminiSession: GeminiLiveSession? = null
    private var micCapture: AudioCapture? = null
    private var systemCapture: SystemAudioCapture? = null
    private var player: AudioPlayer? = null

    private val isRunning = AtomicBoolean(false)

    fun start(): Boolean {
        if (isRunning.get()) return true

        Log.d("HamAva", "AudioPipeline starting: source=${settings.audioSource}, targetLang=${settings.targetLanguage.code}")

        if (settings.apiKey.isBlank()) {
            Log.e("HamAva", "AudioPipeline: ApiKey is blank!")
            emitError(DubError.ApiKeyMissing)
            return false
        }

        // 1. Initialize Player
        player = AudioPlayer(
            context = context,
            onOutputRmsCalculated = { rms ->
                _outputRms.value = rms
            },
            onError = { msg ->
                Log.e("HamAva", "AudioPlayer error: $msg")
                emitError(DubError.AudioTrackError(msg))
            }
        )
        if (player?.start() != true) {
            Log.e("HamAva", "AudioPlayer failed to start!")
            emitError(DubError.AudioTrackError("Playback initialization failed"))
            stop()
            return false
        }
        player?.setVolume(settings.dubVolumeRatio)

        // 2. Initialize Gemini Live Session
        geminiSession = GeminiLiveSession(
            settings = settings,
            onAudioReceived = { chunk24k ->
                player?.playChunk(chunk24k)
            },
            onStatusChanged = { status ->
                _status.value = status
            },
            onLatencyUpdated = { lat ->
                _latency.value = lat
            },
            onError = { error ->
                emitError(error)
            }
        )
        geminiSession?.connect()

        // 3. Initialize Audio Capture (Microphone or System Audio)
        if (settings.audioSource == AudioSourceType.SYSTEM) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null) {
                Log.d("HamAva", "AudioPipeline: Initializing SystemAudioCapture with MediaProjection")
                systemCapture = SystemAudioCapture(
                    mediaProjection = mediaProjection,
                    context = context,
                    onAudioChunkCaptured = { chunk, length, rms ->
                        _inputRms.value = rms
                        geminiSession?.sendAudioChunk(chunk, length, rms)
                    },
                    onError = { msg ->
                        Log.e("HamAva", "SystemAudioCapture error: $msg")
                        emitError(DubError.AudioRecordError(msg))
                    }
                )
                if (systemCapture?.start() != true) {
                    emitError(DubError.AudioRecordError("System audio capture failed to start"))
                    stop()
                    return false
                }
            } else {
                emitError(DubError.MediaProjectionError("MediaProjection is required for system audio"))
                stop()
                return false
            }
        } else {
            micCapture = AudioCapture(
                onAudioChunkCaptured = { chunk, length, rms ->
                    _inputRms.value = rms
                    geminiSession?.sendAudioChunk(chunk, length, rms)
                },
                onError = { msg ->
                    emitError(DubError.AudioRecordError(msg))
                }
            )
            if (micCapture?.start() != true) {
                emitError(DubError.AudioRecordError("Microphone capture failed to start"))
                stop()
                return false
            }
        }

        isRunning.set(true)
        return true
    }

    fun setVolume(volume: Float) {
        player?.setVolume(volume)
    }

    private fun emitError(error: DubError) {
        pipelineScope.launch {
            _errors.emit(error)
        }
    }

    fun stop() {
        isRunning.set(false)
        micCapture?.stop()
        micCapture = null

        systemCapture?.stop()
        systemCapture = null

        geminiSession?.disconnect()
        geminiSession = null

        player?.stop()
        player = null

        _status.value = DubStatus.IDLE
        _inputRms.value = 0f
        _outputRms.value = 0f
    }

    fun isActive(): Boolean = isRunning.get()
}
