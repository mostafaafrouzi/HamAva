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
                    if (inlineData?.data != null) {
                        val decodedBytes = Base64.getDecoder().decode(inlineData.data)
                        if (decodedBytes.isNotEmpty()) {
                            audioChunks.add(decodedBytes)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Silently ignore or pass along malformed frame
        }
        return audioChunks
    }
}
