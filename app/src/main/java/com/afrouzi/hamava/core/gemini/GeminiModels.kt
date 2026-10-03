/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.core.gemini

import com.google.gson.annotations.SerializedName

// --- Client Setup Messages ---

data class GeminiLiveSetupMessage(
    @SerializedName("setup")
    val setup: GeminiSetupConfig
)

data class GeminiSetupConfig(
    @SerializedName("model")
    val model: String,
    @SerializedName("generationConfig")
    val generationConfig: GeminiGenerationConfig,
    @SerializedName("systemInstruction")
    val systemInstruction: GeminiSystemInstruction
)

data class GeminiGenerationConfig(
    @SerializedName("responseModalities")
    val responseModalities: List<String> = listOf("AUDIO"),
    @SerializedName("speechConfig")
    val speechConfig: GeminiSpeechConfig
)

data class GeminiSpeechConfig(
    @SerializedName("voiceConfig")
    val voiceConfig: GeminiVoiceConfig
)

data class GeminiVoiceConfig(
    @SerializedName("prebuiltVoiceConfig")
    val prebuiltVoiceConfig: GeminiPrebuiltVoiceConfig
)

data class GeminiPrebuiltVoiceConfig(
    @SerializedName("voiceName")
    val voiceName: String
)

data class GeminiSystemInstruction(
    @SerializedName("parts")
    val parts: List<GeminiPartText>
)

data class GeminiPartText(
    @SerializedName("text")
    val text: String
)

// --- Client Realtime Input Message ---

data class GeminiRealtimeInputMessage(
    @SerializedName("realtimeInput")
    val realtimeInput: GeminiRealtimeInput
)

data class GeminiRealtimeInput(
    @SerializedName("mediaChunks")
    val mediaChunks: List<GeminiMediaChunk>
)

data class GeminiMediaChunk(
    @SerializedName("mimeType")
    val mimeType: String = "audio/pcm;rate=16000",
    @SerializedName("data")
    val data: String // Base64 PCM
)

// --- Server Incoming Messages ---

data class GeminiServerMessage(
    @SerializedName("setupComplete")
    val setupComplete: Any? = null,
    @SerializedName("serverContent")
    val serverContent: GeminiServerContent? = null,
    @SerializedName("toolCall")
    val toolCall: Any? = null,
    @SerializedName("error")
    val error: GeminiServerError? = null
)

data class GeminiServerContent(
    @SerializedName("modelTurn")
    val modelTurn: GeminiModelTurn? = null,
    @SerializedName("turnComplete")
    val turnComplete: Boolean? = null,
    @SerializedName("interrupted")
    val interrupted: Boolean? = null
)

data class GeminiModelTurn(
    @SerializedName("parts")
    val parts: List<GeminiServerPart>? = null
)

data class GeminiServerPart(
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("inlineData")
    val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    @SerializedName("mimeType")
    val mimeType: String? = null,
    @SerializedName("data")
    val data: String? = null // Base64 PCM 24kHz
)

data class GeminiServerError(
    @SerializedName("code")
    val code: Int? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("status")
    val status: String? = null
)
