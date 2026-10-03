/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.afrouzi.hamava.MainActivity
import com.afrouzi.hamava.R
import com.afrouzi.hamava.data.model.AudioSourceType
import com.afrouzi.hamava.domain.repository.SettingsRepositoryInterface
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SystemAudioTileService : TileService() {

    @Inject
    lateinit var settingsRepository: SettingsRepositoryInterface

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        val isRunning = DubForegroundService.isServiceRunning.value
        val settings = settingsRepository.getSettingsSnapshot()

        if (isRunning && settings.audioSource == AudioSourceType.SYSTEM) {
            val stopIntent = Intent(this, DubForegroundService::class.java).apply {
                action = DubForegroundService.ACTION_STOP
            }
            startService(stopIntent)
        } else {
            // System audio requires MediaProjection permission prompt from Activity
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action_trigger_system_dubbing", true)
            }
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isRunning = DubForegroundService.isServiceRunning.value
        val settings = settingsRepository.getSettingsSnapshot()

        if (isRunning && settings.audioSource == AudioSourceType.SYSTEM) {
            tile.state = Tile.STATE_ACTIVE
            tile.subtitle = getString(R.string.status_active)
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.subtitle = getString(R.string.source_system)
        }
        tile.updateTile()
    }
}
