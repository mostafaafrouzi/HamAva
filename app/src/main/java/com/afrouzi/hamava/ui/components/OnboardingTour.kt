/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.afrouzi.hamava.ui.theme.PrimaryPurple
import com.afrouzi.hamava.ui.theme.TealActive

private data class TourStep(
    val icon: ImageVector,
    val iconTint: Color,
    val titleFa: String,
    val titleEn: String,
    val descFa: String,
    val descEn: String
)

private val tourSteps = listOf(
    TourStep(
        icon = Icons.Default.Mic,
        iconTint = TealActive,
        titleFa = "منبع صدا را انتخاب کنید",
        titleEn = "Choose Your Audio Source",
        descFa = "از میکروفون برای دوبله مستقیم گفتار یا از صدای سیستم برای دوبله فیلم و یوتیوب استفاده کنید. پیش‌فرض روی صدای سیستم است.",
        descEn = "Use microphone for direct speech dubbing, or System Audio to dub videos, YouTube, and other apps. System Audio is the default."
    ),
    TourStep(
        icon = Icons.Default.Language,
        iconTint = Color(0xFF0A84FF),
        titleFa = "زبان مقصد را تنظیم کنید",
        titleEn = "Set Target Language",
        descFa = "زبانی که می‌خواهید صدا به آن دوبله شود را انتخاب کنید. روی کارت زبان ضربه بزنید تا لیست کامل زبان‌ها نمایش داده شود.",
        descEn = "Select the language you want audio dubbed into. Tap the language card to see all supported languages."
    ),
    TourStep(
        icon = Icons.Default.RecordVoiceOver,
        iconTint = Color(0xFFFF9F0A),
        titleFa = "صدای گوینده و لحن را انتخاب کنید",
        titleEn = "Pick Voice & Tone",
        descFa = "صدای دوبلور و لحن ترجمه (محاوره‌ای، رسمی، فنی) را متناسب با محتوایتان تنظیم کنید.",
        descEn = "Choose your dubbing voice persona and translation tone (casual, formal, technical) to match your content style."
    ),
    TourStep(
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        iconTint = Color(0xFFFF453A),
        titleFa = "صدای ویدیوی اصلی را تنظیم کنید",
        titleEn = "Adjust Original Volume",
        descFa = "با اسلایدر حجم ویدیوی اصلی، صدای اصلی را کاهش دهید تا دوبله فارسی واضح‌تر شنیده شود.",
        descEn = "Use the volume slider to lower the original video audio so the dubbed track comes through clearly."
    ),
    TourStep(
        icon = Icons.Default.PlayCircle,
        iconTint = Color(0xFF34C759),
        titleFa = "دکمه دوبله را فشار دهید!",
        titleEn = "Press Start to Begin!",
        descFa = "دکمه بزرگ مرکزی را برای شروع دوبله زنده بزنید. می‌توانید با دکمه شناور در بالای سایر اپ‌ها هم کنترل کنید.",
        descEn = "Tap the large center button to start live dubbing. You can also control it from the floating bubble overlay on top of other apps."
    )
)

@Composable
fun OnboardingTourOverlay(
    isPersian: Boolean,
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = tourSteps.size
    val progress by animateFloatAsState(
        targetValue = (currentStep + 1).toFloat() / totalSteps.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tourProgress"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 4 }) togetherWith
                        (fadeOut() + slideOutVertically { -it / 4 })
                },
                label = "tourContent"
            ) { step ->
                val tourStep = tourSteps[step]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF1C1C2E))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Step indicator dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 20.dp)
                    ) {
                        repeat(totalSteps) { idx ->
                            Box(
                                modifier = Modifier
                                    .size(if (idx == step) 20.dp else 8.dp, 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (idx == step) TealActive
                                        else if (idx < step) TealActive.copy(alpha = 0.4f)
                                        else Color(0xFF38383A)
                                    )
                            )
                        }
                    }

                    // Icon circle
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(tourStep.iconTint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tourStep.icon,
                            contentDescription = null,
                            tint = tourStep.iconTint,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Step counter
                    Text(
                        text = if (isPersian) "مرحله ${step + 1} از $totalSteps" else "Step ${step + 1} of $totalSteps",
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Title
                    Text(
                        text = if (isPersian) tourStep.titleFa else tourStep.titleEn,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description
                    Text(
                        text = if (isPersian) tourStep.descFa else tourStep.descEn,
                        fontSize = 14.sp,
                        color = Color(0xFFB0B0C8),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = TealActive,
                        trackColor = Color(0xFF2C2C3E),
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Navigation buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (step == 0) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = if (isPersian) "رد کردن" else "Skip",
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = if (isPersian) "قبلی" else "Back",
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (step < totalSteps - 1) {
                                    currentStep++
                                } else {
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (step == totalSteps - 1) TealActive else PrimaryPurple
                            )
                        ) {
                            Text(
                                text = when {
                                    step == totalSteps - 1 -> if (isPersian) "شروع!" else "Let's Go!"
                                    else -> if (isPersian) "بعدی" else "Next"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
