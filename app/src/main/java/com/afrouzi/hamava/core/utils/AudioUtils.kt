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
}
