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
import com.afrouzi.hamava.data.model.DubTone
import com.afrouzi.hamava.data.model.GeminiConstants
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class GeminiLiveSession(
    private val settings: DubSettings,
    private val onAudioReceived: (ByteArray) -> Unit,
    private val onTextReceived: ((String) -> Unit)? = null,
    private val onStatusChanged: (DubStatus) -> Unit,
    private val onLatencyUpdated: (Long) -> Unit,
    private val onError: (DubError) -> Unit
) {

    private val gson = Gson()
    private val chunkProcessor = AudioChunkProcessor(gson)

    private val availableApiKeys = listOf(settings.apiKey) + settings.fallbackApiKeys.filter { it.isNotBlank() }
    private var currentKeyIndex = AtomicInteger(0)

    private val client: OkHttpClient = createHttpClient()

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

    private fun createHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .pingInterval(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        if (settings.proxyType != "NONE" && settings.proxyHost.isNotBlank() && settings.proxyPort > 0) {
            try {
                val proxyType = if (settings.proxyType == "SOCKS") Proxy.Type.SOCKS else Proxy.Type.HTTP
                val proxy = Proxy(proxyType, InetSocketAddress(settings.proxyHost, settings.proxyPort))
                builder.proxy(proxy)
                Log.d(TAG, "Configured ${settings.proxyType} proxy at ${settings.proxyHost}:${settings.proxyPort}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to configure proxy: ${e.localizedMessage}")
            }
        }
        return builder.build()
    }

    private fun getActiveApiKey(): String {
        val idx = currentKeyIndex.get().coerceIn(0, availableApiKeys.size - 1)
        return availableApiKeys[idx]
    }

    fun connect() {
        val activeKey = getActiveApiKey()
        if (activeKey.isBlank()) {
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
        Log.d(TAG, "Selected Gemini model: $effectiveModel via $effectiveWsUrl (Key #${currentKeyIndex.get() + 1}/${availableApiKeys.size})")
        connectDirectly()
    }

    private fun connectDirectly() {
        val activeKey = getActiveApiKey()
        val wsUrl = "$effectiveWsUrl?key=$activeKey"
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

                // Extract text subtitles if present
                val subText = chunkProcessor.extractTextFromResponse(text)
                if (!subText.isNullOrBlank()) {
                    onTextReceived?.invoke(subText)
                }

                // Extract audio chunks
                val chunks = chunkProcessor.extractAudioFromResponse(text)
                if (chunks.isNotEmpty()) {
                    if (lastSendTimestamp > 0) {
                        val latency = SystemClock.elapsedRealtime() - lastSendTimestamp
                        onLatencyUpdated(latency)
                    }
                    onStatusChanged(DubStatus.ACTIVE_SPEAKING)
                    for (chunk in chunks) {
                        onAudioReceived(chunk)
                    }
                }

                if (text.contains("\"turnComplete\":true")) {
                    Log.d(TAG, "TurnComplete received from Gemini Live API")
                    sessionScope.launch {
                        delay(400)
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
                    if (code == 400 || code == 403 || code == 429) {
                        if (tryFallbackToNextApiKey()) {
                            Log.i(TAG, "Switched to backup API key #${currentKeyIndex.get() + 1}")
                            connectDirectly()
                            return
                        }
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

    private fun tryFallbackToNextApiKey(): Boolean {
        val nextIdx = currentKeyIndex.incrementAndGet()
        if (nextIdx < availableApiKeys.size) {
            return true
        }
        currentKeyIndex.set(0) // Wrap around
        return false
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

    private fun handleReconnection(reason: String = "") {
        val attempt = reconnectAttempts.incrementAndGet()
        if (attempt <= MAX_RECONNECT_ATTEMPTS) {
            val delayMs = (attempt * 1000L).coerceAtMost(5000L)
            Log.d(TAG, "Reconnecting attempt #$attempt in ${delayMs}ms (reason: $reason)")
            onStatusChanged(DubStatus.CONNECTING)
            sessionScope.launch {
                delay(delayMs)
                if (!isManuallyClosed.get()) {
                    connectDirectly()
                }
            }
        } else {
            Log.e(TAG, "Max reconnect attempts ($MAX_RECONNECT_ATTEMPTS) reached")
            onError(DubError.NetworkError("Failed to reconnect after $MAX_RECONNECT_ATTEMPTS attempts: $reason"))
            onStatusChanged(DubStatus.ERROR)
        }
    }

    private fun sendSetupMessage(ws: WebSocket) {
        val toneInstruction = when (settings.dubTone) {
            DubTone.COLLOQUIAL -> "Use natural, conversational, and everyday colloquial Persian (فارسی محاوره‌ای و عامیانه). Translate slang and casual phrases naturally into everyday spoken Persian. Match the emotion and energy."
            DubTone.FORMAL -> "Use formal, academic, and literary Persian (فارسی رسمی و فصیح). Ideal for educational courses, conferences, and news broadcasts. Maintain eloquent vocabulary and correct grammatical structure."
            DubTone.TECHNICAL -> "Use professional technical Persian. Keep key technical, engineering, software, and AI terms in English or industry standard terms. Do not over-translate specialized terms."
        }

        val lowLatencyInstruction = if (settings.lowLatencyMode) {
            " CRITICAL REAL-TIME STREAMING: Translate incrementally with the lowest possible latency. Do not wait for complete sentences; stream short translated phrases continuously as the speech arrives. Speak briskly and fluidly with zero hesitation."
        } else ""

        val prompt = "You are a professional real-time dubbing assistant. " +
                "You will hear live speech. Immediately translate and speak everything into " +
                "${settings.targetLanguage.nameEn} (${settings.targetLanguage.nameFa}). " +
                "$toneInstruction$lowLatencyInstruction " +
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

    fun disconnect() {
        Log.d(TAG, "Disconnecting Gemini Live session")
        isManuallyClosed.set(true)
        isConnected.set(false)
        isSetupComplete.set(false)
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (e: Exception) {
            // Ignore
        }
        webSocket = null
        onStatusChanged(DubStatus.IDLE)
    }

    fun isSessionActive(): Boolean = isConnected.get() && isSetupComplete.get()

    companion object {
        private const val TAG = "HamAva"
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }
}
