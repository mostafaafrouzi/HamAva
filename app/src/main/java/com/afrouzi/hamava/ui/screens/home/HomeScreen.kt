/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.ui.screens.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.afrouzi.hamava.data.model.DubStatus
import com.afrouzi.hamava.service.DubForegroundService
import com.afrouzi.hamava.ui.components.AudioWaveform
import com.afrouzi.hamava.ui.components.DubButton
import com.afrouzi.hamava.ui.components.LanguageSelectorBottomSheet
import com.afrouzi.hamava.ui.components.OnboardingTourOverlay
import com.afrouzi.hamava.ui.components.StatusCard
import com.afrouzi.hamava.ui.components.ToneSelectorBottomSheet
import com.afrouzi.hamava.ui.components.VoiceSelectorBottomSheet
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.style.TextOverflow
import com.afrouzi.hamava.ui.theme.ErrorColor
import com.afrouzi.hamava.ui.theme.PrimaryPurple
import com.afrouzi.hamava.ui.theme.TealActive

// iOS Dark Palette
private val IosCardBg = Color(0xFF1C1C1E)
private val IosControlBg = Color(0xFF2C2C2E)
private val IosSelectedSegment = Color(0xFF3A3A3C)
private val IosBorder = Color(0xFF38383A)
private val IosOrange = Color(0xFFFF9F0A)
private val IosGreen = Color(0xFF34C759)
private val IosRed = Color(0xFFFF453A)
private val IosBlue = Color(0xFF0A84FF)

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
    var showVoiceSheet by remember { mutableStateOf(false) }
    var showToneSheet by remember { mutableStateOf(false) }
    var showTour by remember { mutableStateOf(false) }

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
        if (!viewModel.hasSeenTour()) {
            showTour = true
        }
    }

    val isPersian = uiState.currentSettings.appLanguage == "fa"
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (uiState.currentSettings.appTheme) {
        "light" -> false
        "dark" -> true
        else -> isSystemDark
    }

    val cardBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val controlBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    val selectedSegment = if (isDark) Color(0xFF3A3A3C) else Color(0xFFFFFFFF)
    val borderColor = if (isDark) Color(0xFF38383A) else Color(0xFFE5E5EA)
    val primaryTextColor = if (isDark) Color.White else Color(0xFF1C1C1E)
    val secondaryTextColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF6C6C70)

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { err ->
            snackbarHostState.showSnackbar(err.getUserMessage(isPersian))
        }
    }

    if (showLanguageSheet) {
        LanguageSelectorBottomSheet(
            selectedLanguage = uiState.currentSettings.targetLanguage,
            onLanguageSelected = { lang -> viewModel.selectLanguage(lang) },
            onDismissRequest = { showLanguageSheet = false }
        )
    }
    if (showVoiceSheet) {
        VoiceSelectorBottomSheet(
            selectedVoice = uiState.currentSettings.voice,
            onVoiceSelected = { voice -> viewModel.selectVoice(voice) },
            isPersian = isPersian,
            onDismissRequest = { showVoiceSheet = false }
        )
    }
    if (showToneSheet) {
        ToneSelectorBottomSheet(
            selectedTone = uiState.currentSettings.dubTone,
            onToneSelected = { tone -> viewModel.selectDubTone(tone) },
            isPersian = isPersian,
            onDismissRequest = { showToneSheet = false }
        )
    }
    if (showTour) {
        OnboardingTourOverlay(
            isPersian = isPersian,
            onDismiss = {
                showTour = false
                viewModel.markTourSeen()
            }
        )
    }

    val canDrawOverlays = remember(uiState.currentSettings.enableFloatingOverlay) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    val isSessionActiveOrPaused = uiState.status == DubStatus.ACTIVE_LISTENING ||
            uiState.status == DubStatus.ACTIVE_SPEAKING ||
            uiState.status == DubStatus.PAUSED

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
                            text = if (isPersian) "• دوبله زنده" else "• Live Dub",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Theme toggle
                    IconButton(onClick = {
                        val newTheme = if (isDark) "light" else "dark"
                        viewModel.updateTheme(newTheme)
                    }) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
            Spacer(modifier = Modifier.height(6.dp))

            // iOS Dynamic Island Status Capsule
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(controlBg)
                    .border(1.dp, borderColor, RoundedCornerShape(26.dp))
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusDotColor by animateColorAsState(
                            targetValue = when (uiState.status) {
                                DubStatus.ACTIVE_SPEAKING -> IosGreen
                                DubStatus.ACTIVE_LISTENING -> TealActive
                                DubStatus.PAUSED -> IosOrange
                                DubStatus.ERROR -> IosRed
                                DubStatus.CONNECTING -> IosBlue
                                else -> Color(0xFF8E8E93)
                            },
                            label = "statusDotColor"
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusDotColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when (uiState.status) {
                                DubStatus.ACTIVE_SPEAKING -> stringResource(R.string.status_speaking)
                                DubStatus.ACTIVE_LISTENING -> stringResource(R.string.status_listening)
                                DubStatus.PAUSED -> stringResource(R.string.status_paused)
                                DubStatus.CONNECTING -> stringResource(R.string.status_connecting)
                                DubStatus.ERROR -> stringResource(R.string.status_error)
                                else -> stringResource(R.string.status_ready)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryTextColor
                        )
                    }

                    if (uiState.latencyMs > 0 && isSessionActiveOrPaused) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF141416))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${uiState.latencyMs} ms",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TealActive
                            )
                        }
                    }
                }
            }

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
                    shape = RoundedCornerShape(16.dp),
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

            // Floating Overlay Permission Banner
            AnimatedVisibility(
                visible = uiState.currentSettings.enableFloatingOverlay && !canDrawOverlays && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryPurple.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = TealActive,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.overlay_banner),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealActive)
                        ) {
                            Text(stringResource(R.string.overlay_grant_btn), color = Color.Black, fontSize = 11.sp)
                        }
                    }
                }
            }

            // iOS-Style Segmented Control for Audio Source (System Audio first & default vs Mic)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(controlBg)
                    .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Segment 1: System Audio (FIRST & DEFAULT)
                    val isSystem = uiState.currentSettings.audioSource == AudioSourceType.SYSTEM
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isSystem) selectedSegment else Color.Transparent)
                            .clickable { viewModel.selectAudioSource(AudioSourceType.SYSTEM) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = if (isSystem) TealActive else secondaryTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.source_system),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSystem) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSystem) primaryTextColor else secondaryTextColor
                            )
                        }
                    }

                    // Segment 2: Microphone
                    val isMic = uiState.currentSettings.audioSource == AudioSourceType.MIC
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isMic) selectedSegment else Color.Transparent)
                            .clickable { viewModel.selectAudioSource(AudioSourceType.MIC) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isMic) TealActive else secondaryTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.source_mic),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isMic) FontWeight.Bold else FontWeight.Normal,
                                color = if (isMic) primaryTextColor else secondaryTextColor
                            )
                        }
                    }
                }
            }

            // ── THREE SELECTOR CARDS: Language | Voice | Tone ─────────────────
            val toneShortLabel = when (uiState.currentSettings.dubTone) {
                com.afrouzi.hamava.data.model.DubTone.COLLOQUIAL -> if (isPersian) "محاوره‌ای" else "Casual"
                com.afrouzi.hamava.data.model.DubTone.FORMAL -> if (isPersian) "رسمی" else "Formal"
                com.afrouzi.hamava.data.model.DubTone.TECHNICAL -> if (isPersian) "تخصصی" else "Technical"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Language selector card
                SelectorCard(
                    modifier = Modifier.weight(1f),
                    label = if (isPersian) "زبان" else "Language",
                    value = uiState.currentSettings.targetLanguage.flag + " " +
                            (if (isPersian) uiState.currentSettings.targetLanguage.nameFa
                             else uiState.currentSettings.targetLanguage.nameEn),
                    onClick = { showLanguageSheet = true },
                    containerColor = cardBg,
                    borderColor = borderColor,
                    labelColor = secondaryTextColor,
                    valueColor = primaryTextColor
                )
                // Voice selector card
                SelectorCard(
                    modifier = Modifier.weight(1f),
                    label = if (isPersian) "صدا" else "Voice",
                    value = if (isPersian) uiState.currentSettings.voice.nameFa
                            else uiState.currentSettings.voice.displayName,
                    onClick = { showVoiceSheet = true },
                    containerColor = cardBg,
                    borderColor = borderColor,
                    labelColor = secondaryTextColor,
                    valueColor = primaryTextColor
                )
                // Tone selector card
                SelectorCard(
                    modifier = Modifier.weight(1f),
                    label = if (isPersian) "لحن" else "Tone",
                    value = toneShortLabel,
                    onClick = { showToneSheet = true },
                    containerColor = cardBg,
                    borderColor = borderColor,
                    labelColor = secondaryTextColor,
                    valueColor = primaryTextColor
                )
            }

            // Audio Waveform Visualizer
            AudioWaveform(
                isActive = uiState.status == DubStatus.ACTIVE_LISTENING || uiState.status == DubStatus.ACTIVE_SPEAKING,
                isSpeaking = uiState.status == DubStatus.ACTIVE_SPEAKING,
                rmsLevel = if (uiState.status == DubStatus.ACTIVE_SPEAKING) uiState.outputRms else uiState.inputRms,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            // Live Subtitles Card
            AnimatedVisibility(
                visible = uiState.currentSettings.enableSubtitles && (uiState.status == DubStatus.ACTIVE_SPEAKING || uiState.status == DubStatus.ACTIVE_LISTENING || uiState.subtitle.isNotBlank()),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderColor))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = null,
                                tint = TealActive,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.live_subtitles_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = TealActive,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (uiState.status == DubStatus.ACTIVE_SPEAKING) {
                                Text(
                                    text = "● " + stringResource(R.string.status_speaking),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IosGreen,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.subtitle.isNotBlank()) uiState.subtitle else (if (isPersian) "در انتظار شنیدن و ترجمه گفتار…" else "Listening for speech to translate…"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = primaryTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Dubbing Button (Central Controller)
            DubButton(
                status = uiState.status,
                onClick = {
                    when (uiState.status) {
                        DubStatus.ACTIVE_LISTENING, DubStatus.ACTIVE_SPEAKING -> {
                            viewModel.pauseDubbing()
                        }
                        DubStatus.PAUSED -> {
                            viewModel.resumeDubbing()
                        }
                        DubStatus.CONNECTING -> {
                            viewModel.stopDubbing()
                        }
                        else -> {
                            if (uiState.currentSettings.audioSource == AudioSourceType.SYSTEM) {
                                if (DubForegroundService.activeMediaProjection != null) {
                                    viewModel.toggleDubbing()
                                } else {
                                    onRequestMediaProjection()
                                }
                            } else {
                                if (PermissionUtils.hasRecordAudioPermission(context)) {
                                    viewModel.toggleDubbing()
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                    }
                }
            )

            // When Active or Paused, show Stop Session button
            AnimatedVisibility(
                visible = isSessionActiveOrPaused,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Button(
                        onClick = { viewModel.stopDubbing() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IosRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.exit_dubbing),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Original Video Background Volume Card (iOS Style)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderColor))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.original_volume_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryTextColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.original_volume_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryTextColor,
                                fontSize = 10.5.sp
                            )
                        }
                        Text(
                            text = "${(uiState.currentSettings.originalAudioVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IosBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                            contentDescription = null,
                            tint = secondaryTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = uiState.currentSettings.originalAudioVolume,
                            onValueChange = { viewModel.updateOriginalVolume(it) },
                            valueRange = 0.0f..1.0f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = IosBlue,
                                activeTrackColor = IosBlue,
                                inactiveTrackColor = controlBg
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = IosBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
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

// ── Reusable Selector Card ─────────────────────────────────────────────────────
@Composable
private fun SelectorCard(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF1C1C1E),
    borderColor: Color = Color(0xFF38383A),
    labelColor: Color = Color(0xFF8E8E93),
    valueColor: Color = Color.White
) {
    Card(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderColor))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = labelColor,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
