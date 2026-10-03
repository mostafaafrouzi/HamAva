/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import com.afrouzi.hamava.domain.repository.SettingsRepositoryInterface
import com.afrouzi.hamava.service.DubForegroundService
import com.afrouzi.hamava.ui.navigation.HamAvaNavHost
import com.afrouzi.hamava.ui.theme.HamAvaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepositoryInterface

    private val mediaProjectionManager by lazy {
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val mediaProjection = mediaProjectionManager.getMediaProjection(
                result.resultCode,
                result.data!!
            )
            DubForegroundService.activeMediaProjection = mediaProjection
            val intent = Intent(this, DubForegroundService::class.java).apply {
                action = DubForegroundService.ACTION_START
            }
            ContextCompat.startForegroundService(this, intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle Quick Tile trigger if applicable
        if (intent?.getBooleanExtra("action_trigger_system_dubbing", false) == true) {
            requestMediaProjection()
        }

        setContent {
            val settings by settingsRepository.settingsFlow.collectAsState(
                initial = settingsRepository.getSettingsSnapshot()
            )

            val isDarkTheme = when (settings.appTheme) {
                "light" -> false
                "dark" -> true
                else -> true // Dark default
            }

            val isRtl = settings.appLanguage == "fa"
            val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                HamAvaTheme(darkTheme = isDarkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        HamAvaNavHost(
                            onRequestMediaProjection = { requestMediaProjection() }
                        )
                    }
                }
            }
        }
    }

    private fun requestMediaProjection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
        }
    }
}
