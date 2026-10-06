/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
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
import com.afrouzi.hamava.ui.overlay.FloatingOverlayManager
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
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class DubForegroundService : Service() {

    @Inject
    lateinit var settingsRepository: SettingsRepositoryInterface

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var audioPipeline: AudioPipeline? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var floatingOverlay: FloatingOverlayManager? = null

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
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                startDubbingSession(settings, resultCode, resultData)
            }
            ACTION_STOP -> {
                stopDubbingSession()
                stopSelf()
            }
            ACTION_PAUSE -> {
                pauseDubbingSession()
            }
            ACTION_RESUME -> {
                resumeDubbingSession()
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

    private fun startDubbingSession(
        settings: DubSettings,
        resultCode: Int = Activity.RESULT_CANCELED,
        resultData: Intent? = null
    ) {
        wakeLock?.acquire(3 * 60 * 60 * 1000L) // Max 3 hours safeguard

        val res = getLocalizedResources(settings)
        val targetLangName = if (settings.appLanguage == "fa") settings.targetLanguage.nameFa else settings.targetLanguage.nameEn
        val notification = createNotification(
            settings = settings,
            statusText = res.getString(R.string.status_connecting),
            targetLang = targetLangName,
            source = if (settings.audioSource == AudioSourceType.MIC) res.getString(R.string.source_mic) else res.getString(R.string.source_system)
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
        Log.d("HamAva", "startDubbingSession: source=${settings.audioSource}, targetLang=${settings.targetLanguage.code}, voice=${settings.voice.id}, model=${settings.model}")

        // Initialize Floating Overlay if enabled
        if (settings.enableFloatingOverlay) {
            try {
                floatingOverlay?.hide()
                floatingOverlay = FloatingOverlayManager(
                    context = applicationContext,
                    onTogglePause = {
                        if (_statusFlow.value == DubStatus.PAUSED) {
                            resumeDubbingSession()
                        } else {
                            pauseDubbingSession()
                        }
                    },
                    onStopSession = {
                        stopDubbingSession()
                        stopSelf()
                    },
                    onOriginalVolumeChanged = { volume ->
                        audioPipeline?.setOriginalVolume(volume)
                        serviceScope.launch {
                            settingsRepository.updateOriginalAudioVolume(volume)
                        }
                    }
                )
                floatingOverlay?.show(settings)
            } catch (e: Exception) {
                Log.w("HamAva", "Could not initialize floating overlay: ${e.localizedMessage}")
            }
        }

        // On Android 10+ (Q+), instantiate MediaProjection AFTER startForeground with MEDIA_PROJECTION type
        if (settings.audioSource == AudioSourceType.SYSTEM) {
            if (resultCode == Activity.RESULT_OK && resultData != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        val projection = projectionManager.getMediaProjection(resultCode, resultData)
                        Log.d("HamAva", "MediaProjection successfully acquired!")
                        projection.registerCallback(object : MediaProjection.Callback() {
                            override fun onStop() {
                                super.onStop()
                                Log.d("HamAva", "MediaProjection.Callback onStop triggered")
                                activeMediaProjection = null
                                stopDubbingSession()
                                stopSelf()
                            }
                        }, null)
                        activeMediaProjection = projection
                    } catch (e: Exception) {
                        Log.e("HamAva", "MediaProjection retrieval failed: ${e.localizedMessage}", e)
                        serviceScope.launch {
                            _errorFlow.emit(DubError.MediaProjectionError("Media projection failed: ${e.localizedMessage}"))
                        }
                        stopDubbingSession()
                        stopSelf()
                        return
                    }
                }
            } else if (activeMediaProjection == null) {
                Log.e("HamAva", "MediaProjection permission required but not provided!")
                serviceScope.launch {
                    _errorFlow.emit(DubError.MediaProjectionError("Media projection permission is required"))
                }
                stopDubbingSession()
                stopSelf()
                return
            }
        }

        audioPipeline?.stop()
        audioPipeline = AudioPipeline(settings, activeMediaProjection, applicationContext)

        serviceScope.launch {
            audioPipeline?.status?.collect { status ->
                Log.d("HamAva", "DubStatus updated: $status")
                _statusFlow.value = status
                floatingOverlay?.updateStatus(status)
                updateNotificationForStatus(status, settings)
            }
        }

        serviceScope.launch {
            audioPipeline?.latency?.collect { lat ->
                _latencyFlow.value = lat
                floatingOverlay?.updateLatency(lat)
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
            audioPipeline?.subtitle?.collect { text ->
                _subtitleFlow.value = text
                floatingOverlay?.updateSubtitle(text)
            }
        }

        serviceScope.launch {
            audioPipeline?.errors?.collect { error ->
                Log.e("HamAva", "AudioPipeline error received: ${error.getUserMessage(false)}")
                _errorFlow.emit(error)
            }
        }

        audioPipeline?.start()
    }

    private fun pauseDubbingSession() {
        Log.d("HamAva", "pauseDubbingSession called")
        audioPipeline?.pause()
        _statusFlow.value = DubStatus.PAUSED
        floatingOverlay?.updateStatus(DubStatus.PAUSED)
        val settings = settingsRepository.getSettingsSnapshot()
        updateNotificationForStatus(DubStatus.PAUSED, settings)
    }

    private fun resumeDubbingSession() {
        Log.d("HamAva", "resumeDubbingSession called")
        audioPipeline?.resume()
        _statusFlow.value = DubStatus.ACTIVE_LISTENING
        floatingOverlay?.updateStatus(DubStatus.ACTIVE_LISTENING)
        val settings = settingsRepository.getSettingsSnapshot()
        updateNotificationForStatus(DubStatus.ACTIVE_LISTENING, settings)
    }

    private fun stopDubbingSession() {
        Log.d("HamAva", "stopDubbingSession called")
        audioPipeline?.stop()
        audioPipeline = null
        try {
            activeMediaProjection?.stop()
        } catch (e: Exception) {
            // Ignore
        }
        activeMediaProjection = null

        floatingOverlay?.hide()
        floatingOverlay = null

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        _isServiceRunning.value = false
        _statusFlow.value = DubStatus.IDLE
        _inputRmsFlow.value = 0f
        _outputRmsFlow.value = 0f
        _latencyFlow.value = 0L
        _subtitleFlow.value = ""

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun getLocalizedResources(settings: DubSettings): Resources {
        val locale = if (settings.appLanguage == "fa") Locale("fa") else Locale.ENGLISH
        val config = Configuration(resources.configuration).apply {
            setLocale(locale)
        }
        return createConfigurationContext(config).resources
    }

    private fun updateNotificationForStatus(status: DubStatus, settings: DubSettings) {
        val res = getLocalizedResources(settings)
        val statusText = when (status) {
            DubStatus.IDLE -> res.getString(R.string.status_ready)
            DubStatus.CONNECTING -> res.getString(R.string.status_connecting)
            DubStatus.ACTIVE_LISTENING -> res.getString(R.string.status_listening)
            DubStatus.ACTIVE_SPEAKING -> res.getString(R.string.status_speaking)
            DubStatus.ERROR -> res.getString(R.string.status_error)
            DubStatus.PAUSED -> res.getString(R.string.status_paused)
        }
        val source = if (settings.audioSource == AudioSourceType.MIC) res.getString(R.string.source_mic) else res.getString(R.string.source_system)
        val targetLangName = if (settings.appLanguage == "fa") settings.targetLanguage.nameFa else settings.targetLanguage.nameEn
        val notification = createNotification(settings, statusText, targetLangName, source, isPaused = (status == DubStatus.PAUSED))
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(HamAvaApplication.NOTIFICATION_ID, notification)
    }

    private fun createNotification(
        settings: DubSettings,
        statusText: String,
        targetLang: String,
        source: String,
        isPaused: Boolean = false
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val res = getLocalizedResources(settings)
        val title = res.getString(R.string.notification_title_active)
        val contentText = String.format(res.getString(R.string.notification_text_active), targetLang, source) + " • " + statusText

        val builder = NotificationCompat.Builder(this, HamAvaApplication.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_tile_mic)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(!isPaused)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (isPaused) {
            val resumeIntent = Intent(this, DubForegroundService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePendingIntent = PendingIntent.getService(
                this,
                2,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, res.getString(R.string.resume_dubbing), resumePendingIntent)
        } else {
            val pauseIntent = Intent(this, DubForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getService(
                this,
                3,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, res.getString(R.string.pause_dubbing), pausePendingIntent)
        }

        val stopIntent = Intent(this, DubForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, res.getString(R.string.exit_dubbing), stopPendingIntent)

        return builder.build()
    }

    override fun onDestroy() {
        stopDubbingSession()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.afrouzi.hamava.action.START"
        const val ACTION_STOP = "com.afrouzi.hamava.action.STOP"
        const val ACTION_PAUSE = "com.afrouzi.hamava.action.PAUSE"
        const val ACTION_RESUME = "com.afrouzi.hamava.action.RESUME"
        const val ACTION_TOGGLE = "com.afrouzi.hamava.action.TOGGLE"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

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

        private val _subtitleFlow = MutableStateFlow("")
        val subtitleFlow: StateFlow<String> = _subtitleFlow.asStateFlow()

        private val _errorFlow = MutableSharedFlow<DubError>(replay = 1)
        val errorFlow: SharedFlow<DubError> = _errorFlow.asSharedFlow()
    }
}
