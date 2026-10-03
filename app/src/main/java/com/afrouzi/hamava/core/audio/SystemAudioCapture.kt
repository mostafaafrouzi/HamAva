/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.core.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
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
    private val context: Context? = null,
    private val onAudioChunkCaptured: (ByteArray, Int, Float) -> Unit,
    private val onError: (String) -> Unit
) {

    private var audioRecord: AudioRecord? = null
    private val isRecording = AtomicBoolean(false)
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private var activeSampleRate: Int = 48000
    private var activeChannels: Int = 2

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (isRecording.get()) return true

        try {
            val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val nativeSampleRateStr = audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            val nativeSampleRate = nativeSampleRateStr?.toIntOrNull() ?: 48000

            val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            data class Candidate(val rate: Int, val mask: Int, val channels: Int)
            val candidates = listOf(
                Candidate(nativeSampleRate, AudioFormat.CHANNEL_IN_STEREO, 2),
                Candidate(48000, AudioFormat.CHANNEL_IN_STEREO, 2),
                Candidate(44100, AudioFormat.CHANNEL_IN_STEREO, 2),
                Candidate(nativeSampleRate, AudioFormat.CHANNEL_IN_MONO, 1),
                Candidate(48000, AudioFormat.CHANNEL_IN_MONO, 1),
                Candidate(16000, AudioFormat.CHANNEL_IN_MONO, 1)
            ).distinct()

            var initializedRecord: AudioRecord? = null

            for (candidate in candidates) {
                try {
                    val audioFormat = AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(candidate.rate)
                        .setChannelMask(candidate.mask)
                        .build()

                    val minBufferSize = AudioRecord.getMinBufferSize(
                        candidate.rate,
                        candidate.mask,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    if (minBufferSize <= 0) continue

                    val rawChunkSize = (candidate.rate * candidate.channels * 2 * 160) / 1000
                    val bufferSize = (minBufferSize * 2).coerceAtLeast(rawChunkSize * 4)

                    val record = AudioRecord.Builder()
                        .setAudioPlaybackCaptureConfig(config)
                        .setAudioFormat(audioFormat)
                        .setBufferSizeInBytes(bufferSize)
                        .build()

                    if (record.state == AudioRecord.STATE_INITIALIZED) {
                        initializedRecord = record
                        activeSampleRate = candidate.rate
                        activeChannels = candidate.channels
                        Log.d("HamAva", "SystemAudioCapture configured with rate=$activeSampleRate, channels=$activeChannels")
                        break
                    } else {
                        record.release()
                    }
                } catch (e: Exception) {
                    Log.w("HamAva", "Candidate format ${candidate.rate}Hz ${candidate.channels}ch failed: ${e.localizedMessage}")
                }
            }

            if (initializedRecord == null) {
                Log.e("HamAva", "Failed to initialize system audio capture AudioRecord for all format candidates")
                onError("Failed to initialize system audio capture AudioRecord")
                return false
            }

            audioRecord = initializedRecord
            audioRecord?.startRecording()
            isRecording.set(true)
            Log.d("HamAva", "SystemAudioCapture: startRecording successful! Native rate=$activeSampleRate, channels=$activeChannels")

            recordingJob = scope.launch {
                val rawChunkSize = (activeSampleRate * activeChannels * 2 * 160) / 1000
                val rawBuffer = ByteArray(rawChunkSize)
                var chunkCount = 0L

                while (isActive && isRecording.get()) {
                    var bytesReadTotal = 0
                    while (bytesReadTotal < rawBuffer.size && isActive && isRecording.get()) {
                        val read = audioRecord?.read(
                            rawBuffer,
                            bytesReadTotal,
                            rawBuffer.size - bytesReadTotal
                        ) ?: -1

                        if (read > 0) {
                            bytesReadTotal += read
                        } else if (read < 0) {
                            Log.w("HamAva", "SystemAudioCapture AudioRecord.read error: $read")
                            break
                        }
                    }

                    if (bytesReadTotal > 0) {
                        val mono16k = AudioUtils.resampleTo16kMono(
                            rawBuffer,
                            bytesReadTotal,
                            activeSampleRate,
                            activeChannels
                        )
                        if (mono16k.isNotEmpty()) {
                            val rms = AudioUtils.calculateRms(mono16k, mono16k.size)
                            chunkCount++
                            if (chunkCount % 30L == 1L) {
                                Log.d("HamAva", "SystemAudioCapture chunk #$chunkCount (raw=$bytesReadTotal -> 16k=${mono16k.size} bytes, rms=$rms)")
                            }
                            onAudioChunkCaptured(mono16k, mono16k.size, rms)
                        }
                    }
                }
            }

            return true
        } catch (e: Exception) {
            Log.e("HamAva", "SystemAudioCapture exception: ${e.localizedMessage}", e)
            onError("System audio capture error: ${e.localizedMessage}")
            stop()
            return false
        }
    }

    fun stop() {
        Log.d("HamAva", "SystemAudioCapture stop called")
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
