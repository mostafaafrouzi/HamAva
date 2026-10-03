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
import com.afrouzi.hamava.core.utils.AudioUtils
import com.afrouzi.hamava.data.model.GeminiConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class AudioCapture(
    private val onAudioChunkCaptured: (ByteArray, Int, Float) -> Unit,
    private val onError: (String) -> Unit
) {

    private var audioRecord: AudioRecord? = null
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
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore on release
        }
        audioRecord = null
    }

    fun isCapturing(): Boolean = isRecording.get()
}
