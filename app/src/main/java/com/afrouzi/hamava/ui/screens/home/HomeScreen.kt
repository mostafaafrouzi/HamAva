/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afrouzi.hamava.R
import com.afrouzi.hamava.core.utils.PermissionUtils
import com.afrouzi.hamava.data.model.AudioSourceType
import com.afrouzi.hamava.data.model.DubError
import com.afrouzi.hamava.data.model.DubStatus
import com.afrouzi.hamava.ui.components.AudioWaveform
import com.afrouzi.hamava.ui.components.DubButton
import com.afrouzi.hamava.ui.components.LanguageSelectorBottomSheet
import com.afrouzi.hamava.ui.components.StatusCard
import com.afrouzi.hamava.ui.components.VoiceSelectorRow
import com.afrouzi.hamava.ui.theme.DarkSurface
import com.afrouzi.hamava.ui.theme.DarkSurfaceVariant
import com.afrouzi.hamava.ui.theme.ErrorColor
import com.afrouzi.hamava.ui.theme.PrimaryPurple
import com.afrouzi.hamava.ui.theme.TealActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onRequestMediaProjection: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showLanguageSheet by remember { mutableStateOf(false) }

    // Audio Permission Launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleDubbing()
        }
    }

    // Notification Permission Launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Optional */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val isPersian = uiState.currentSettings.appLanguage == "fa"

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { err ->
            snackbarHostState.showSnackbar(err.getUserMessage(isPersian))
        }
    }

    if (showLanguageSheet) {
        LanguageSelectorBottomSheet(
            selectedLanguage = uiState.currentSettings.targetLanguage,
            onLanguageSelected = { lang ->
                viewModel.selectLanguage(lang)
            },
            onDismissRequest = { showLanguageSheet = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TealActive
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• Live Dub",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_settings),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.nav_about),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // API Key Missing Warning Banner
            AnimatedVisibility(
                visible = uiState.currentSettings.apiKey.isBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { onNavigateToSettings() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorColor.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = ErrorColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.no_api_key_banner),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Audio Source Selector (Mic vs System)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = uiState.currentSettings.audioSource == AudioSourceType.MIC,
                    onClick = { viewModel.selectAudioSource(AudioSourceType.MIC) },
                    label = { Text(stringResource(R.string.source_mic)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryPurple.copy(alpha = 0.25f),
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = TealActive,
                        containerColor = DarkSurfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                FilterChip(
                    selected = uiState.currentSettings.audioSource == AudioSourceType.SYSTEM,
                    onClick = {
                        viewModel.selectAudioSource(AudioSourceType.SYSTEM)
                        onRequestMediaProjection()
                    },
                    label = { Text(stringResource(R.string.source_system)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryPurple.copy(alpha = 0.25f),
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = TealActive,
                        containerColor = DarkSurfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Target Language Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { showLanguageSheet = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.target_lang_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.currentSettings.targetLanguage.flag,
                                fontSize = 22.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "${uiState.currentSettings.targetLanguage.nameFa} (${uiState.currentSettings.targetLanguage.nameEn})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = stringResource(R.string.select_language),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Voice Selector Row
            Text(
                text = stringResource(R.string.voice_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
            VoiceSelectorRow(
                selectedVoice = uiState.currentSettings.voice,
                onVoiceSelected = { voice -> viewModel.selectVoice(voice) },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Audio Waveform Visualizer
            AudioWaveform(
                isActive = uiState.status == DubStatus.ACTIVE_LISTENING || uiState.status == DubStatus.ACTIVE_SPEAKING,
                isSpeaking = uiState.status == DubStatus.ACTIVE_SPEAKING,
                rmsLevel = if (uiState.status == DubStatus.ACTIVE_SPEAKING) uiState.outputRms else uiState.inputRms,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main Dubbing Button
            DubButton(
                status = uiState.status,
                onClick = {
                    if (uiState.currentSettings.audioSource == AudioSourceType.SYSTEM) {
                        onRequestMediaProjection()
                    } else {
                        if (PermissionUtils.hasRecordAudioPermission(context)) {
                            viewModel.toggleDubbing()
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Dubbing Volume Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = uiState.currentSettings.dubVolumeRatio,
                    onValueChange = { viewModel.updateVolume(it) },
                    valueRange = 0.1f..1.5f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = TealActive,
                        activeTrackColor = TealActive,
                        inactiveTrackColor = DarkSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = TealActive,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Session Status Card
            StatusCard(
                status = uiState.status,
                latencyMs = uiState.latencyMs,
                settings = uiState.currentSettings,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}
