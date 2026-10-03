/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.core.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi
import com.afrouzi.hamava.core.utils.AudioUtils
import com.afrouzi.hamava.data.model.GeminiConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@RequiresApi(Build.VERSION_CODES.Q)
class SystemAudioCapture(
    private val mediaProjection: MediaProjection,
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

        try {
            val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(GeminiConstants.SAMPLE_RATE_IN)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build()

            val minBufferSize = AudioRecord.getMinBufferSize(
                GeminiConstants.SAMPLE_RATE_IN,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val bufferSize = (minBufferSize * 2).coerceAtLeast(GeminiConstants.CHUNK_BYTES * 2)

            audioRecord = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(config)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .build()

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError("Failed to initialize system audio capture AudioRecord")
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
            onError("System audio capture error: ${e.localizedMessage}")
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
            // Ignore
        }
        audioRecord = null
    }

    fun isCapturing(): Boolean = isRecording.get()
}
