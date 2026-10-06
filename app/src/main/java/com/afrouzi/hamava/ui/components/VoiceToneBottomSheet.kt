/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afrouzi.hamava.data.model.DubTone
import com.afrouzi.hamava.data.model.GeminiVoice
import com.afrouzi.hamava.ui.theme.TealActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSelectorBottomSheet(
    selectedVoice: GeminiVoice,
    onVoiceSelected: (GeminiVoice) -> Unit,
    isPersian: Boolean,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val itemBg = MaterialTheme.colorScheme.surfaceVariant
    val titleColor = MaterialTheme.colorScheme.onSurface
    val descColor = MaterialTheme.colorScheme.onSurfaceVariant

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (isPersian) "انتخاب صدای گوینده" else "Select Dubbing Voice",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            GeminiVoice.ALL_VOICES.forEach { voice ->
                val isSelected = voice.id == selectedVoice.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) TealActive.copy(alpha = 0.14f) else itemBg)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) TealActive else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            onVoiceSelected(voice)
                            onDismissRequest()
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) TealActive.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = if (isSelected) TealActive else descColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPersian) voice.nameFa else voice.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) TealActive else titleColor
                        )
                        Text(
                            text = voice.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = descColor,
                            fontSize = 11.sp
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = TealActive,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToneSelectorBottomSheet(
    selectedTone: DubTone,
    onToneSelected: (DubTone) -> Unit,
    isPersian: Boolean,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val itemBg = MaterialTheme.colorScheme.surfaceVariant
    val titleColor = MaterialTheme.colorScheme.onSurface
    val descColor = MaterialTheme.colorScheme.onSurfaceVariant

    val toneColors = mapOf(
        DubTone.COLLOQUIAL to Color(0xFF34C759),
        DubTone.FORMAL to Color(0xFF0A84FF),
        DubTone.TECHNICAL to Color(0xFFFF9F0A)
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (isPersian) "لحن ترجمه" else "Translation Tone",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = if (isPersian) "سبک ترجمه را متناسب با محتوای ویدیو انتخاب کنید" else "Choose translation style to match the content type",
                style = MaterialTheme.typography.bodySmall,
                color = descColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            DubTone.entries.forEach { tone ->
                val isSelected = tone == selectedTone
                val accentColor = toneColors[tone] ?: TealActive

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) accentColor.copy(alpha = 0.14f) else itemBg)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) accentColor else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onToneSelected(tone)
                            onDismissRequest()
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPersian) tone.titleFa else tone.titleEn,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) accentColor else titleColor
                        )
                        Text(
                            text = tone.descriptionFa,
                            style = MaterialTheme.typography.bodySmall,
                            color = descColor,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
