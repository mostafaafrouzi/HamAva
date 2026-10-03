/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.core.gemini

import android.os.SystemClock
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
    private val isManuallyClosed = AtomicBoolean(false)
    private val reconnectAttempts = AtomicInteger(0)

    private var sessionScope = CoroutineScope(Dispatchers.IO + Job())
    private var lastSendTimestamp = 0L

    fun connect() {
        if (settings.apiKey.isBlank()) {
            onError(DubError.ApiKeyMissing)
            return
        }

        isManuallyClosed.set(false)
        onStatusChanged(DubStatus.CONNECTING)

        val wsUrl = "${GeminiConstants.LIVE_API_WS_URL}?key=${settings.apiKey}"
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected.set(true)
                reconnectAttempts.set(0)
                sendSetupMessage(webSocket)
                onStatusChanged(DubStatus.ACTIVE_LISTENING)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
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
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
                isConnected.set(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected.set(false)
                if (!isManuallyClosed.get()) {
                    handleReconnection()
                } else {
                    onStatusChanged(DubStatus.IDLE)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected.set(false)
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
        })
    }

    private fun sendSetupMessage(ws: WebSocket) {
        val prompt = "You are a professional real-time dubbing assistant. " +
                "You will hear live speech. Immediately translate and speak everything into " +
                "${settings.targetLanguage.nameEn} (${settings.targetLanguage.nameFa}). " +
                "Preserve the emotional tone, cadence, and human feel of the original speaker. " +
                "Speak ONLY the translated speech. Do not add comments, greetings, or explanations."

        val setupMsg = GeminiLiveSetupMessage(
            setup = GeminiSetupConfig(
                model = settings.model,
                generationConfig = GeminiGenerationConfig(
                    responseModalities = listOf("AUDIO"),
                    speechConfig = GeminiSpeechConfig(
                        voiceConfig = GeminiVoiceConfig(
                            prebuiltVoiceConfig = GeminiPrebuiltVoiceConfig(
                                voiceName = settings.voice.id
                            )
                        )
                    )
                ),
                systemInstruction = GeminiSystemInstruction(
                    parts = listOf(GeminiPartText(text = prompt))
                )
            )
        )
        val json = gson.toJson(setupMsg)
        ws.send(json)
    }

    fun sendAudioChunk(pcmData: ByteArray, length: Int) {
        if (!isConnected.get() || webSocket == null || length <= 0) return

        try {
            val json = chunkProcessor.buildRealtimeInputJson(pcmData, length)
            lastSendTimestamp = SystemClock.elapsedRealtime()
            webSocket?.send(json)
        } catch (e: Exception) {
            // Buffer overflow or socket error
        }
    }

    private fun handleReconnection(lastErrorMsg: String = "") {
        val attempts = reconnectAttempts.incrementAndGet()
        if (attempts <= MAX_RECONNECT_ATTEMPTS) {
            onStatusChanged(DubStatus.CONNECTING)
            val backoffMillis = (1000L * (1 shl (attempts - 1))).coerceAtMost(30000L)
            sessionScope.launch {
                delay(backoffMillis)
                if (!isManuallyClosed.get()) {
                    connect()
                }
            }
        } else {
            onError(DubError.NetworkError("Failed to reconnect after $MAX_RECONNECT_ATTEMPTS attempts. $lastErrorMsg"))
            onStatusChanged(DubStatus.ERROR)
        }
    }

    fun disconnect() {
        isManuallyClosed.set(true)
        isConnected.set(false)
        try {
            webSocket?.close(1000, "User stopped dubbing session")
        } catch (e: Exception) {
            // Socket already closed
        }
        webSocket = null
        onStatusChanged(DubStatus.IDLE)
    }

    companion object {
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }
}
