/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.core.gemini

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.Base64

class AudioChunkProcessor(
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
) {

    /**
     * Converts a raw PCM 16kHz mono audio buffer into a JSON string ready to be transmitted over WebSocket.
     */
    fun buildRealtimeInputJson(pcmBuffer: ByteArray, length: Int): String {
        val bytesToSend = if (length == pcmBuffer.size) pcmBuffer else pcmBuffer.copyOf(length)
        val base64Data = Base64.getEncoder().encodeToString(bytesToSend)
        val message = GeminiRealtimeInputMessage(
            realtimeInput = GeminiRealtimeInput(
                mediaChunks = listOf(
                    GeminiMediaChunk(
                        mimeType = "audio/pcm;rate=16000",
                        data = base64Data
                    )
                )
            )
        )
        return gson.toJson(message)
    }

    /**
     * Extracts raw PCM 24kHz audio bytes from incoming Gemini server JSON message.
     */
    fun extractAudioFromResponse(jsonText: String): List<ByteArray> {
        val audioChunks = mutableListOf<ByteArray>()
        try {
            val message = gson.fromJson(jsonText, GeminiServerMessage::class.java)
            val parts = message.serverContent?.modelTurn?.parts
            if (parts != null) {
                for (part in parts) {
                    val inlineData = part.inlineData
                    val rawData = inlineData?.data
                    if (!rawData.isNullOrBlank()) {
                        val decodedBytes = decodeBase64Safe(rawData)
                        if (decodedBytes.isNotEmpty()) {
                            audioChunks.add(decodedBytes)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Log warning on malformed JSON
        }
        return audioChunks
    }

    /**
     * Extracts text translation / transcription parts from incoming Gemini server JSON message.
     */
    fun extractTextFromResponse(jsonText: String): String? {
        try {
            val message = gson.fromJson(jsonText, GeminiServerMessage::class.java)
            val parts = message.serverContent?.modelTurn?.parts
            if (parts != null) {
                val sb = StringBuilder()
                for (part in parts) {
                    val txt = part.text
                    if (!txt.isNullOrBlank()) {
                        sb.append(txt).append(" ")
                    }
                }
                if (sb.isNotEmpty()) {
                    return sb.toString().trim()
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }

    private fun decodeBase64Safe(raw: String): ByteArray {
        val sanitized = raw.replace("\n", "").replace("\r", "").replace(" ", "").trim()
        return try {
            Base64.getDecoder().decode(sanitized)
        } catch (e1: Exception) {
            try {
                Base64.getMimeDecoder().decode(sanitized)
            } catch (e2: Exception) {
                try {
                    Base64.getUrlDecoder().decode(sanitized)
                } catch (e3: Exception) {
                    ByteArray(0)
                }
            }
        }
    }
}
