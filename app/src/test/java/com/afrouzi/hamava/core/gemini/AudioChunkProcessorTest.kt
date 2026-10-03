/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.core.gemini

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioChunkProcessorTest {

    private val processor = AudioChunkProcessor()

    @Test
    fun testBuildRealtimeInputJsonContainsExpectedFields() {
        val samplePcm = byteArrayOf(1, 2, 3, 4, 5, 6)
        val json = processor.buildRealtimeInputJson(samplePcm, samplePcm.size)

        assertTrue(json.contains("realtimeInput"))
        assertTrue(json.contains("mediaChunks"))
        assertTrue(json.contains("audio/pcm;rate=16000"))
        assertTrue(json.contains("data"))
    }

    @Test
    fun testExtractAudioFromResponse() {
        val jsonResponse = """
            {
                "serverContent": {
                    "modelTurn": {
                        "parts": [
                            {
                                "inlineData": {
                                    "mimeType": "audio/pcm;rate=24000",
                                    "data": "AQIDBA=="
                                }
                            }
                        ]
                    }
                }
            }
        """.trimIndent()

        val chunks = processor.extractAudioFromResponse(jsonResponse)
        assertEquals(1, chunks.size)
        assertEquals(4, chunks[0].size) // AQIDBA== decodes to 4 bytes: 1, 2, 3, 4
    }

    @Test
    fun testExtractAudioWithInvalidJsonReturnsEmptyList() {
        val chunks = processor.extractAudioFromResponse("invalid json")
        assertTrue(chunks.isEmpty())
    }
}
