/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.afrouzi.hamava.core.utils.AudioUtils
import com.afrouzi.hamava.data.model.GeminiConstants
import java.util.concurrent.atomic.AtomicBoolean

class AudioPlayer(
    private val onOutputRmsCalculated: (Float) -> Unit,
    private val onError: (String) -> Unit
) {

    private var audioTrack: AudioTrack? = null
    private val isPlaying = AtomicBoolean(false)
    private var volumeRatio: Float = 0.85f

    fun start(): Boolean {
        if (isPlaying.get()) return true

        val sampleRate = GeminiConstants.SAMPLE_RATE_OUT // 24kHz
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize == AudioTrack.ERROR || minBufferSize == AudioTrack.ERROR_BAD_VALUE) {
            onError("Unsupported audio playback buffer configuration")
            return false
        }

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize * 4)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                onError("Failed to initialize AudioTrack")
                return false
            }

            audioTrack?.play()
            isPlaying.set(true)
            return true
        } catch (e: Exception) {
            onError("AudioTrack exception: ${e.localizedMessage}")
            stop()
            return false
        }
    }

    fun playChunk(pcm24kBytes: ByteArray) {
        if (!isPlaying.get() || audioTrack == null || pcm24kBytes.isEmpty()) return

        try {
            val processedBytes = AudioUtils.applyGain(pcm24kBytes, pcm24kBytes.size, volumeRatio)
            val rms = AudioUtils.calculateRms(processedBytes, processedBytes.size)
            onOutputRmsCalculated(rms)

            audioTrack?.write(processedBytes, 0, processedBytes.size, AudioTrack.WRITE_BLOCKING)
        } catch (e: Exception) {
            // Write failed or track released
        }
    }

    fun setVolume(volume: Float) {
        volumeRatio = volume.coerceIn(0f, 1.5f)
        try {
            audioTrack?.setVolume(volume.coerceIn(0f, 1f))
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun stop() {
        isPlaying.set(false)
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioTrack = null
    }

    fun isPlaying(): Boolean = isPlaying.get()
}
