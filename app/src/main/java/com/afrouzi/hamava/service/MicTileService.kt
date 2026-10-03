/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.afrouzi.hamava.MainActivity
import com.afrouzi.hamava.R
import com.afrouzi.hamava.core.utils.PermissionUtils
import com.afrouzi.hamava.data.model.AudioSourceType
import com.afrouzi.hamava.domain.repository.SettingsRepositoryInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MicTileService : TileService() {

    @Inject
    lateinit var settingsRepository: SettingsRepositoryInterface

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        val isRunning = DubForegroundService.isServiceRunning.value
        if (isRunning) {
            val stopIntent = Intent(this, DubForegroundService::class.java).apply {
                action = DubForegroundService.ACTION_STOP
            }
            startService(stopIntent)
        } else {
            // Check permission and API key
            val settings = settingsRepository.getSettingsSnapshot()
            if (settings.apiKey.isBlank() || !PermissionUtils.hasRecordAudioPermission(this)) {
                // Open MainActivity to configure
                val appIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivityAndCollapse(appIntent)
                return
            }

            serviceScope.launch {
                settingsRepository.updateAudioSource(AudioSourceType.MIC)
                val startIntent = Intent(this@MicTileService, DubForegroundService::class.java).apply {
                    action = DubForegroundService.ACTION_START
                }
                ContextCompat.startForegroundService(this@MicTileService, startIntent)
                updateTileState()
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isRunning = DubForegroundService.isServiceRunning.value
        val settings = settingsRepository.getSettingsSnapshot()

        if (isRunning && settings.audioSource == AudioSourceType.MIC) {
            tile.state = Tile.STATE_ACTIVE
            tile.subtitle = getString(R.string.status_active)
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.subtitle = getString(R.string.status_ready)
        }
        tile.updateTile()
    }
}
