/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.afrouzi.hamava.HamAvaApplication
import com.afrouzi.hamava.MainActivity
import com.afrouzi.hamava.R
import com.afrouzi.hamava.core.audio.AudioPipeline
import com.afrouzi.hamava.data.model.AudioSourceType
import com.afrouzi.hamava.data.model.DubError
import com.afrouzi.hamava.data.model.DubSettings
import com.afrouzi.hamava.data.model.DubStatus
import com.afrouzi.hamava.domain.repository.SettingsRepositoryInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DubForegroundService : Service() {

    @Inject
    lateinit var settingsRepository: SettingsRepositoryInterface

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var audioPipeline: AudioPipeline? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HamAva::DubWakeLock")?.apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val settings = settingsRepository.getSettingsSnapshot()
                startDubbingSession(settings)
            }
            ACTION_STOP -> {
                stopDubbingSession()
                stopSelf()
            }
            ACTION_TOGGLE -> {
                if (_isServiceRunning.value) {
                    stopDubbingSession()
                    stopSelf()
                } else {
                    val settings = settingsRepository.getSettingsSnapshot()
                    startDubbingSession(settings)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startDubbingSession(settings: DubSettings) {
        wakeLock?.acquire(3 * 60 * 60 * 1000L) // Max 3 hours safeguard

        val notification = createNotification(
            statusText = getString(R.string.status_connecting),
            targetLang = settings.targetLanguage.nameFa,
            source = if (settings.audioSource == AudioSourceType.MIC) getString(R.string.source_mic) else getString(R.string.source_system)
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (settings.audioSource == AudioSourceType.SYSTEM) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            }
            startForeground(HamAvaApplication.NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(HamAvaApplication.NOTIFICATION_ID, notification)
        }

        _isServiceRunning.value = true

        audioPipeline?.stop()
        audioPipeline = AudioPipeline(settings, activeMediaProjection)

        serviceScope.launch {
            audioPipeline?.status?.collect { status ->
                _statusFlow.value = status
                updateNotificationForStatus(status, settings)
            }
        }

        serviceScope.launch {
            audioPipeline?.latency?.collect { lat ->
                _latencyFlow.value = lat
            }
        }

        serviceScope.launch {
            audioPipeline?.inputRms?.collect { rms ->
                _inputRmsFlow.value = rms
            }
        }

        serviceScope.launch {
            audioPipeline?.outputRms?.collect { rms ->
                _outputRmsFlow.value = rms
            }
        }

        serviceScope.launch {
            audioPipeline?.errors?.collect { error ->
                _errorFlow.emit(error)
            }
        }

        audioPipeline?.start()
    }

    private fun updateNotificationForStatus(status: DubStatus, settings: DubSettings) {
        val statusText = when (status) {
            DubStatus.CONNECTING -> getString(R.string.status_connecting)
            DubStatus.ACTIVE_LISTENING -> getString(R.string.status_listening)
            DubStatus.ACTIVE_SPEAKING -> getString(R.string.status_speaking)
            DubStatus.ERROR -> getString(R.string.status_error)
            DubStatus.PAUSED -> getString(R.string.status_paused)
            DubStatus.IDLE -> getString(R.string.status_ready)
        }

        val notification = createNotification(
            statusText = statusText,
            targetLang = settings.targetLanguage.nameFa,
            source = if (settings.audioSource == AudioSourceType.MIC) getString(R.string.source_mic) else getString(R.string.source_system)
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(HamAvaApplication.NOTIFICATION_ID, notification)
    }

    private fun stopDubbingSession() {
        audioPipeline?.stop()
        audioPipeline = null
        _isServiceRunning.value = false
        _statusFlow.value = DubStatus.IDLE
        _inputRmsFlow.value = 0f
        _outputRmsFlow.value = 0f

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotification(statusText: String, targetLang: String, source: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, DubForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = "$statusText • $source ➔ $targetLang"

        return NotificationCompat.Builder(this, HamAvaApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_mic)
            .setContentTitle(getString(R.string.notification_title_active))
            .setContentText(contentText)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_tile_mic,
                getString(R.string.action_stop),
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        stopDubbingSession()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.afrouzi.hamava.action.START"
        const val ACTION_STOP = "com.afrouzi.hamava.action.STOP"
        const val ACTION_TOGGLE = "com.afrouzi.hamava.action.TOGGLE"

        var activeMediaProjection: MediaProjection? = null

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _statusFlow = MutableStateFlow(DubStatus.IDLE)
        val statusFlow: StateFlow<DubStatus> = _statusFlow.asStateFlow()

        private val _latencyFlow = MutableStateFlow(0L)
        val latencyFlow: StateFlow<Long> = _latencyFlow.asStateFlow()

        private val _inputRmsFlow = MutableStateFlow(0f)
        val inputRmsFlow: StateFlow<Float> = _inputRmsFlow.asStateFlow()

        private val _outputRmsFlow = MutableStateFlow(0f)
        val outputRmsFlow: StateFlow<Float> = _outputRmsFlow.asStateFlow()

        private val _errorFlow = MutableSharedFlow<DubError>(replay = 1)
        val errorFlow: SharedFlow<DubError> = _errorFlow.asSharedFlow()
    }
}
