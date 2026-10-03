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

    @Test
    fun testResample48kStereoTo16kMono() {
        // 30720 bytes in 48kHz stereo (160ms) -> should produce exactly 5120 bytes in 16kHz mono
        val input48kStereo = ByteArray(30720) { (it % 120).toByte() }
        val output = AudioUtils.resampleTo16kMono(input48kStereo, input48kStereo.size, 48000, 2)
        assertEquals(5120, output.size)
    }

    @Test
    fun testResample48kMonoTo16kMono() {
        // 15360 bytes in 48kHz mono -> 5120 bytes in 16kHz mono
        val input48kMono = ByteArray(15360) { (it % 100).toByte() }
        val output = AudioUtils.resampleTo16kMono(input48kMono, input48kMono.size, 48000, 1)
        assertEquals(5120, output.size)
    }

    @Test
    fun testResample16kStereoTo16kMono() {
        val input16kStereo = ByteArray(1024) { 10 }
        val output = AudioUtils.resampleTo16kMono(input16kStereo, input16kStereo.size, 16000, 2)
        assertEquals(512, output.size)
    }

    @Test
    fun testResample44kStereoTo16kMono() {
        // ~100ms in 44.1kHz stereo: 44100 * 2 * 2 * 0.1 = 17640 bytes
        val input44kStereo = ByteArray(17640) { 15 }
        val output = AudioUtils.resampleTo16kMono(input44kStereo, input44kStereo.size, 44100, 2)
        // 4410 frames -> at 16000 rate, output frames ~ 1600 -> bytes ~ 3200
        assertTrue("Output should be ~3200 bytes", output.size in 3190..3210)
    }
}
