/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.core.utils

import kotlin.math.sqrt

object AudioUtils {

    /**
     * Calculates the normalized Root Mean Square (RMS) volume from a 16-bit PCM byte array.
     * Returns a float value between 0.0f and 1.0f.
     */
    fun calculateRms(buffer: ByteArray, readBytes: Int): Float {
        if (readBytes <= 0) return 0f

        var sumSquare = 0.0
        val sampleCount = readBytes / 2

        var i = 0
        while (i < readBytes - 1) {
            // Little-endian 16-bit PCM
            val low = buffer[i].toInt() and 0xFF
            val high = buffer[i + 1].toInt()
            val sample = (high shl 8) or low
            sumSquare += (sample * sample).toDouble()
            i += 2
        }

        if (sampleCount == 0) return 0f
        val rms = sqrt(sumSquare / sampleCount)
        // Normalize against max short value (32767)
        val normalized = (rms / 32768.0).toFloat()
        return normalized.coerceIn(0f, 1f)
    }

    /**
     * Applies software digital volume gain to 16-bit PCM little-endian audio buffer.
     */
    fun applyGain(buffer: ByteArray, readBytes: Int, gainRatio: Float): ByteArray {
        if (gainRatio == 1.0f || readBytes <= 0) return buffer

        val result = buffer.copyOf(readBytes)
        var i = 0
        while (i < readBytes - 1) {
            val low = result[i].toInt() and 0xFF
            val high = result[i + 1].toInt()
            val sample = ((high shl 8) or low).toShort()

            val scaled = (sample * gainRatio).toInt().coerceIn(-32768, 32767)
            result[i] = (scaled and 0xFF).toByte()
            result[i + 1] = ((scaled shr 8) and 0xFF).toByte()
            i += 2
        }
        return result
    }

    /**
     * Resamples 16-bit PCM little-endian audio from an arbitrary input sample rate and channel count
     * to 16,000 Hz mono 16-bit PCM.
     */
    fun resampleTo16kMono(
        input: ByteArray,
        length: Int,
        sourceSampleRate: Int,
        sourceChannels: Int
    ): ByteArray {
        if (length <= 0) return ByteArray(0)
        if (sourceSampleRate == 16000 && sourceChannels == 1) {
            return input.copyOf(length)
        }

        // Fast path: 48000 Hz Stereo to 16000 Hz Mono (downsample by 3, mix 2 channels)
        if (sourceSampleRate == 48000 && sourceChannels == 2) {
            val totalStereoFrames = length / 4
            val outputSampleCount = totalStereoFrames / 3
            val out = ByteArray(outputSampleCount * 2)
            var inIdx = 0
            var outIdx = 0

            for (f in 0 until outputSampleCount) {
                val left = ((input[inIdx + 1].toInt() shl 8) or (input[inIdx].toInt() and 0xFF)).toShort()
                val right = ((input[inIdx + 3].toInt() shl 8) or (input[inIdx + 2].toInt() and 0xFF)).toShort()
                val mono = ((left.toInt() + right.toInt()) / 2).coerceIn(-32768, 32767)

                out[outIdx] = (mono and 0xFF).toByte()
                out[outIdx + 1] = ((mono shr 8) and 0xFF).toByte()
                outIdx += 2
                inIdx += 12
            }
            return out
        }

        // Fast path: 48000 Hz Mono to 16000 Hz Mono (downsample by 3)
        if (sourceSampleRate == 48000 && sourceChannels == 1) {
            val totalSamples = length / 2
            val outputSampleCount = totalSamples / 3
            val out = ByteArray(outputSampleCount * 2)
            var inIdx = 0
            var outIdx = 0

            for (f in 0 until outputSampleCount) {
                out[outIdx] = input[inIdx]
                out[outIdx + 1] = input[inIdx + 1]
                outIdx += 2
                inIdx += 6
            }
            return out
        }

        // Fast path: 16000 Hz Stereo to 16000 Hz Mono
        if (sourceSampleRate == 16000 && sourceChannels == 2) {
            val totalStereoFrames = length / 4
            val out = ByteArray(totalStereoFrames * 2)
            var inIdx = 0
            var outIdx = 0

            for (f in 0 until totalStereoFrames) {
                val left = ((input[inIdx + 1].toInt() shl 8) or (input[inIdx].toInt() and 0xFF)).toShort()
                val right = ((input[inIdx + 3].toInt() shl 8) or (input[inIdx + 2].toInt() and 0xFF)).toShort()
                val mono = ((left.toInt() + right.toInt()) / 2).coerceIn(-32768, 32767)

                out[outIdx] = (mono and 0xFF).toByte()
                out[outIdx + 1] = ((mono shr 8) and 0xFF).toByte()
                outIdx += 2
                inIdx += 4
            }
            return out
        }

        // General linear interpolation resampler for any sample rate (e.g. 44100 Hz stereo or mono)
        val bytesPerFrame = sourceChannels * 2
        val inputFrames = length / bytesPerFrame
        if (inputFrames <= 0) return ByteArray(0)

        val monoSamples = ShortArray(inputFrames)
        var srcByte = 0
        if (sourceChannels == 2) {
            for (i in 0 until inputFrames) {
                val l = ((input[srcByte + 1].toInt() shl 8) or (input[srcByte].toInt() and 0xFF)).toShort()
                val r = ((input[srcByte + 3].toInt() shl 8) or (input[srcByte + 2].toInt() and 0xFF)).toShort()
                monoSamples[i] = ((l.toInt() + r.toInt()) / 2).toShort()
                srcByte += 4
            }
        } else {
            for (i in 0 until inputFrames) {
                val s = ((input[srcByte + 1].toInt() shl 8) or (input[srcByte].toInt() and 0xFF)).toShort()
                monoSamples[i] = s
                srcByte += 2
            }
        }

        val ratio = sourceSampleRate.toDouble() / 16000.0
        val outputSampleCount = (inputFrames / ratio).toInt()
        val out = ByteArray(outputSampleCount * 2)
        var outIdx = 0

        for (i in 0 until outputSampleCount) {
            val srcPos = i * ratio
            val srcIndex = srcPos.toInt()
            val fraction = srcPos - srcIndex

            val s1 = monoSamples[srcIndex.coerceIn(0, inputFrames - 1)].toDouble()
            val s2 = monoSamples[(srcIndex + 1).coerceIn(0, inputFrames - 1)].toDouble()
            val interpolated = (s1 + fraction * (s2 - s1)).toInt().coerceIn(-32768, 32767)

            out[outIdx] = (interpolated and 0xFF).toByte()
            out[outIdx + 1] = ((interpolated shr 8) and 0xFF).toByte()
            outIdx += 2
        }
        return out
    }
}
