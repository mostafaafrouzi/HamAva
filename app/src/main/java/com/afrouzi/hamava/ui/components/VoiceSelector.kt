/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.afrouzi.hamava.data.model.GeminiVoice
import com.afrouzi.hamava.ui.theme.TealActive

@Composable
fun VoiceSelectorRow(
    selectedVoice: GeminiVoice,
    onVoiceSelected: (GeminiVoice) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GeminiVoice.ALL_VOICES.forEach { voice ->
            val isSelected = voice.id == selectedVoice.id
            FilterChip(
                selected = isSelected,
                onClick = { onVoiceSelected(voice) },
                label = {
                    Text(
                        text = if (isRtl) voice.nameFa else voice.displayName,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TealActive.copy(alpha = 0.2f),
                    selectedLabelColor = TealActive,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    selectedBorderColor = TealActive,
                    borderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}
