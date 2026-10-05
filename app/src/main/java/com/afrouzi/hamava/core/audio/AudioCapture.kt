/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import com.afrouzi.hamava.core.utils.AudioUtils
import com.afrouzi.hamava.data.model.GeminiConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class AudioCapture(
    private val enableAec: Boolean = true,
    private val onAudioChunkCaptured: (ByteArray, Int, Float) -> Unit,
    private val onError: (String) -> Unit
) {

    private var audioRecord: AudioRecord? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    private val isRecording = AtomicBoolean(false)
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (isRecording.get()) return true

        val sampleRate = GeminiConstants.SAMPLE_RATE_IN
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            onError("Unsupported audio hardware buffer configuration")
            return false
        }

        val bufferSize = (minBufferSize * 2).coerceAtLeast(GeminiConstants.CHUNK_BYTES * 2)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                // Fallback to standard MIC source
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError("Failed to initialize AudioRecord")
                return false
            }

            val sessionId = audioRecord?.audioSessionId ?: 0
            if (sessionId != 0 && enableAec) {
                try {
                    if (AcousticEchoCanceler.isAvailable()) {
                        echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply {
                            enabled = true
                        }
                        Log.d("HamAva", "AcousticEchoCanceler enabled successfully for session $sessionId")
                    }
                    if (NoiseSuppressor.isAvailable()) {
                        noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply {
                            enabled = true
                        }
                        Log.d("HamAva", "NoiseSuppressor enabled successfully for session $sessionId")
                    }
                } catch (e: Exception) {
                    Log.w("HamAva", "Could not initialize hardware AEC/NS: ${e.localizedMessage}")
                }
            }

            audioRecord?.startRecording()
            isRecording.set(true)

            recordingJob = scope.launch {
                val chunkBuffer = ByteArray(GeminiConstants.CHUNK_BYTES)

                while (isActive && isRecording.get()) {
                    var bytesReadTotal = 0
                    while (bytesReadTotal < chunkBuffer.size && isActive && isRecording.get()) {
                        val read = audioRecord?.read(
                            chunkBuffer,
                            bytesReadTotal,
                            chunkBuffer.size - bytesReadTotal
                        ) ?: -1

                        if (read > 0) {
                            bytesReadTotal += read
                        } else if (read < 0) {
                            break
                        }
                    }

                    if (bytesReadTotal > 0) {
                        val rms = AudioUtils.calculateRms(chunkBuffer, bytesReadTotal)
                        onAudioChunkCaptured(chunkBuffer.copyOf(bytesReadTotal), bytesReadTotal, rms)
                    }
                }
            }

            return true
        } catch (e: Exception) {
            onError("AudioRecord initialization exception: ${e.localizedMessage}")
            stop()
            return false
        }
    }

    fun stop() {
        isRecording.set(false)
        recordingJob?.cancel()
        recordingJob = null

        try {
            echoCanceler?.release()
            echoCanceler = null
            noiseSuppressor?.release()
            noiseSuppressor = null

            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore on release
        }
        audioRecord = null
    }

    fun isRecording(): Boolean = isRecording.get()
}
