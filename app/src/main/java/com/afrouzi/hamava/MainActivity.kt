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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import com.afrouzi.hamava.domain.repository.SettingsRepositoryInterface
import com.afrouzi.hamava.service.DubForegroundService
import com.afrouzi.hamava.ui.navigation.HamAvaNavHost
import com.afrouzi.hamava.ui.theme.HamAvaTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
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
            val intent = Intent(this, DubForegroundService::class.java).apply {
                action = DubForegroundService.ACTION_START
                putExtra(DubForegroundService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(DubForegroundService.EXTRA_RESULT_DATA, result.data)
            }
            ContextCompat.startForegroundService(this, intent)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("hamava_general_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("app_language", "fa") ?: "fa"
        val locale = if (lang == "fa") Locale("fa") else Locale.ENGLISH
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
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

            var currentLanguage by remember { mutableStateOf(settings.appLanguage) }
            LaunchedEffect(settings.appLanguage) {
                if (currentLanguage != settings.appLanguage) {
                    currentLanguage = settings.appLanguage
                    recreate()
                }
            }

            val isDarkTheme = when (settings.appTheme) {
                "light" -> false
                "dark" -> true
                "system" -> isSystemInDarkTheme()
                else -> isSystemInDarkTheme()
            }

            val isPersian = settings.appLanguage == "fa"
            val layoutDirection = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection
            ) {
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
