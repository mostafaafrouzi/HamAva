/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioUtilsTest {

    @Test
    fun testRmsWithSilentBuffer() {
        val silentBuffer = ByteArray(1024) { 0 }
        val rms = AudioUtils.calculateRms(silentBuffer, silentBuffer.size)
        assertEquals(0f, rms, 0.001f)
    }

    @Test
    fun testRmsWithMaxSignal() {
        val maxBuffer = ByteArray(1024)
        for (i in maxBuffer.indices step 2) {
            // Little endian 32767 = 0xFF, 0x7F
            maxBuffer[i] = 0xFF.toByte()
            maxBuffer[i + 1] = 0x7F.toByte()
        }
        val rms = AudioUtils.calculateRms(maxBuffer, maxBuffer.size)
        assertTrue("RMS should be close to 1.0 for max signal", rms > 0.99f)
    }

    @Test
    fun testApplyGainZero() {
        val buffer = byteArrayOf(0x00, 0x10, 0x20, 0x30)
        val muted = AudioUtils.applyGain(buffer, buffer.size, 0.0f)
        for (b in muted) {
            assertEquals(0.toByte(), b)
        }
    }
}
