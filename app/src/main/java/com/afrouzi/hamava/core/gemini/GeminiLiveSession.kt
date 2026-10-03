/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.core.gemini

import android.os.SystemClock
import android.util.Log
import com.afrouzi.hamava.data.model.DubError
import com.afrouzi.hamava.data.model.DubSettings
import com.afrouzi.hamava.data.model.DubStatus
import com.afrouzi.hamava.data.model.GeminiConstants
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class GeminiLiveSession(
    private val settings: DubSettings,
    private val onAudioReceived: (ByteArray) -> Unit,
    private val onStatusChanged: (DubStatus) -> Unit,
    private val onLatencyUpdated: (Long) -> Unit,
    private val onError: (DubError) -> Unit
) {

    private val gson = Gson()
    private val chunkProcessor = AudioChunkProcessor(gson)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)
    private val isSetupComplete = AtomicBoolean(false)
    private val isManuallyClosed = AtomicBoolean(false)
    private val reconnectAttempts = AtomicInteger(0)
    private val fallbackAttempts = AtomicInteger(0)

    private var sessionScope = CoroutineScope(Dispatchers.IO + Job())
    private var lastSendTimestamp = 0L
    private var effectiveModel: String = settings.model
    private var effectiveWsUrl: String = GeminiConstants.LIVE_API_WS_URL

    fun connect() {
        if (settings.apiKey.isBlank()) {
            Log.e(TAG, "API key is blank, cannot connect")
            onError(DubError.ApiKeyMissing)
            return
        }

        isManuallyClosed.set(false)
        isSetupComplete.set(false)
        onStatusChanged(DubStatus.CONNECTING)

        sessionScope.launch {
            resolveModelAndConnect()
        }
    }

    private fun resolveModelAndConnect() {
        effectiveModel = if (settings.model.isNotBlank()) settings.model else GeminiConstants.DEFAULT_MODEL
        effectiveWsUrl = GeminiConstants.LIVE_API_WS_URL
        Log.d(TAG, "Selected Gemini model: $effectiveModel via $effectiveWsUrl")
        connectDirectly()
    }

    private fun connectDirectly() {
        val wsUrl = "$effectiveWsUrl?key=${settings.apiKey}"
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, createWebSocketListener())
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected successfully! Sending setup message...")
                isConnected.set(true)
                reconnectAttempts.set(0)
                fallbackAttempts.set(0)
                sendSetupMessage(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                handleIncomingMessage(bytes.utf8())
            }

            private fun handleIncomingMessage(text: String) {
                if (text.contains("\"setupComplete\"")) {
                    Log.d(TAG, "SetupComplete received from Gemini Live API! Ready to stream audio.")
                    isSetupComplete.set(true)
                    onStatusChanged(DubStatus.ACTIVE_LISTENING)
                    return
                }

                if (text.contains("\"error\"")) {
                    Log.e(TAG, "Gemini Live API returned error frame: $text")
                }

                val chunks = chunkProcessor.extractAudioFromResponse(text)
                if (chunks.isNotEmpty()) {
                    if (lastSendTimestamp > 0) {
                        val latency = SystemClock.elapsedRealtime() - lastSendTimestamp
                        onLatencyUpdated(latency)
                    }
                    onStatusChanged(DubStatus.ACTIVE_SPEAKING)
                    val totalBytes = chunks.sumOf { it.size }
                    Log.d(TAG, "Received ${chunks.size} audio chunk(s) from Gemini ($totalBytes bytes), playing dub audio...")
                    for (chunk in chunks) {
                        onAudioReceived(chunk)
                    }
                }

                if (text.contains("\"turnComplete\":true")) {
                    Log.d(TAG, "TurnComplete received from Gemini Live API")
                    sessionScope.launch {
                        delay(500)
                        if (isConnected.get() && isSetupComplete.get()) {
                            onStatusChanged(DubStatus.ACTIVE_LISTENING)
                        }
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket onClosing: code=$code, reason=$reason")
                webSocket.close(code, reason)
                isConnected.set(false)
                isSetupComplete.set(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket onClosed: code=$code, reason=$reason")
                isConnected.set(false)
                isSetupComplete.set(false)
                if (!isManuallyClosed.get()) {
                    if (code == 1008) {
                        Log.e(TAG, "Google closed session with 1008: $reason")
                        handleModelFallback(reason)
                    } else {
                        handleReconnection()
                    }
                } else {
                    onStatusChanged(DubStatus.IDLE)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket onFailure: ${t.localizedMessage}, responseCode=${response?.code}", t)
                isConnected.set(false)
                isSetupComplete.set(false)
                if (!isManuallyClosed.get()) {
                    val code = response?.code ?: 0
                    if (code == 400 || code == 403) {
                        onError(DubError.ApiKeyInvalid)
                        onStatusChanged(DubStatus.ERROR)
                    } else {
                        handleReconnection(t.localizedMessage ?: "WebSocket failure")
                    }
                } else {
                    onStatusChanged(DubStatus.IDLE)
                }
            }
        }
    }

    private fun handleModelFallback(reason: String) {
        val attempt = fallbackAttempts.incrementAndGet()
        val candidates = listOf(
            "models/gemini-3.5-live-translate-preview",
            "models/gemini-3.8-live",
            "models/gemini-3.1-flash-live-preview"
        )
        if (attempt <= candidates.size * 2) {
            val nextModel = candidates[(attempt - 1) % candidates.size]
            val useAlpha = (attempt > candidates.size)
            effectiveModel = nextModel
            effectiveWsUrl = if (useAlpha) {
                "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
            } else {
                "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
            }
            Log.d(TAG, "Fallback attempt #$attempt: trying model $effectiveModel on $effectiveWsUrl")
            sessionScope.launch {
                delay(800)
                if (!isManuallyClosed.get()) {
                    connectDirectly()
                }
            }
        } else {
            Log.e(TAG, "All fallback models exhausted for Live API: $reason")
            onError(DubError.NetworkError("Gemini Live API: $reason"))
            onStatusChanged(DubStatus.ERROR)
        }
    }


    private fun sendSetupMessage(ws: WebSocket) {
        val prompt = "You are a professional real-time dubbing assistant. " +
                "You will hear live speech. Immediately translate and speak everything into " +
                "${settings.targetLanguage.nameEn} (${settings.targetLanguage.nameFa}). " +
                "Preserve the emotional tone, cadence, and human feel of the original speaker. " +
                "Speak ONLY the translated speech. Do not add comments, greetings, or explanations."

        val setupMsg = GeminiLiveSetupMessage(
            setup = GeminiSetupConfig(
                model = effectiveModel,
                generationConfig = GeminiGenerationConfig(
                    responseModalities = listOf("AUDIO"),
                    speechConfig = GeminiSpeechConfig(
                        voiceConfig = GeminiVoiceConfig(
                            prebuiltVoiceConfig = GeminiPrebuiltVoiceConfig(
                                voiceName = settings.voice.id
                            )
                        )
                    ),
                    translationConfig = GeminiTranslationConfig(
                        targetLanguageCode = settings.targetLanguage.code
                    )
                ),
                systemInstruction = GeminiSystemInstruction(
                    parts = listOf(GeminiPartText(text = prompt))
                )
            )
        )
        val json = gson.toJson(setupMsg)
        Log.d(TAG, "Sending setup frame: $json")
        ws.send(json)
    }

    fun sendAudioChunk(pcmData: ByteArray, length: Int, rms: Float = 0f) {
        if (!isConnected.get() || !isSetupComplete.get() || webSocket == null || length <= 0) return

        try {
            val json = chunkProcessor.buildRealtimeInputJson(pcmData, length)
            lastSendTimestamp = SystemClock.elapsedRealtime()
            webSocket?.send(json)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send audio chunk: ${e.localizedMessage}")
        }
    }

    private fun handleReconnection(lastErrorMsg: String = "") {
        val attempts = reconnectAttempts.incrementAndGet()
        if (attempts <= MAX_RECONNECT_ATTEMPTS) {
            onStatusChanged(DubStatus.CONNECTING)
            val backoffMillis = (1000L * (1 shl (attempts - 1))).coerceAtMost(30000L)
            Log.d(TAG, "Scheduling reconnection attempt #$attempts in ${backoffMillis}ms")
            sessionScope.launch {
                delay(backoffMillis)
                if (!isManuallyClosed.get()) {
                    connect()
                }
            }
        } else {
            Log.e(TAG, "Exceeded max reconnect attempts: $lastErrorMsg")
            onError(DubError.NetworkError("Failed to reconnect after $MAX_RECONNECT_ATTEMPTS attempts. $lastErrorMsg"))
            onStatusChanged(DubStatus.ERROR)
        }
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting Gemini Live session")
        isManuallyClosed.set(true)
        isConnected.set(false)
        isSetupComplete.set(false)
        try {
            webSocket?.close(1000, "User stopped dubbing session")
        } catch (e: Exception) {
            // Socket already closed
        }
        webSocket = null
        onStatusChanged(DubStatus.IDLE)
    }

    companion object {
        private const val TAG = "HamAva"
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }
}
