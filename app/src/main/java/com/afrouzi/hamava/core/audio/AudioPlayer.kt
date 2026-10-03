/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import com.afrouzi.hamava.core.utils.AudioUtils
import com.afrouzi.hamava.data.model.GeminiConstants
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class AudioPlayer(
    private val context: Context? = null,
    private val onOutputRmsCalculated: (Float) -> Unit,
    private val onError: (String) -> Unit
) {

    private var audioTrack: AudioTrack? = null
    private val isPlaying = AtomicBoolean(false)
    private var volumeRatio: Float = 0.85f

    private val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var isFocusHeld = false

    private val playbackScope = CoroutineScope(Dispatchers.IO + Job())
    private var playbackJob: Job? = null
    private val playbackChannel = Channel<ByteArray>(Channel.UNLIMITED)

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
            requestDuckingFocus()
            startPlaybackWorker()
            Log.d("HamAva", "AudioPlayer started and requested AudioFocus ducking")
            return true
        } catch (e: Exception) {
            onError("AudioTrack exception: ${e.localizedMessage}")
            stop()
            return false
        }
    }

    private fun startPlaybackWorker() {
        playbackJob?.cancel()
        playbackJob = playbackScope.launch {
            for (chunk in playbackChannel) {
                if (!isPlaying.get() || audioTrack == null) break
                try {
                    val processedBytes = AudioUtils.applyGain(chunk, chunk.size, volumeRatio)
                    val rms = AudioUtils.calculateRms(processedBytes, processedBytes.size)
                    onOutputRmsCalculated(rms)
                    audioTrack?.write(processedBytes, 0, processedBytes.size, AudioTrack.WRITE_BLOCKING)
                } catch (e: Exception) {
                    // Ignore track error on teardown
                }
            }
        }
    }

    private fun requestDuckingFocus() {
        if (isFocusHeld) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (audioFocusRequest == null) {
                    audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        .setAcceptsDelayedFocusGain(true)
                        .setWillPauseWhenDucked(false)
                        .setOnAudioFocusChangeListener { }
                        .build()
                }
                val res = audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
                Log.d("HamAva", "AudioPlayer: requestAudioFocus (O+) result = $res")
                if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    isFocusHeld = true
                }
            } else {
                @Suppress("DEPRECATION")
                val res = audioManager?.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
                Log.d("HamAva", "AudioPlayer: requestAudioFocus legacy result = $res")
                if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    isFocusHeld = true
                }
            }
        } catch (e: Exception) {
            Log.w("HamAva", "AudioPlayer: requestAudioFocus exception: ${e.localizedMessage}")
        }
    }

    private fun abandonDuckingFocus() {
        if (!isFocusHeld) return
        try {
            Log.d("HamAva", "AudioPlayer: abandonAudioFocus")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            isFocusHeld = false
        }
    }

    fun playChunk(pcm24kBytes: ByteArray) {
        if (!isPlaying.get() || audioTrack == null || pcm24kBytes.isEmpty()) return

        requestDuckingFocus()
        Log.d("HamAva", "AudioPlayer: enqueuing chunk (${pcm24kBytes.size} bytes)")
        playbackChannel.trySend(pcm24kBytes)
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
        playbackJob?.cancel()
        playbackJob = null
        while (playbackChannel.tryReceive().isSuccess) {}
        abandonDuckingFocus()
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
