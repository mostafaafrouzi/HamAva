/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afrouzi.hamava.R
import com.afrouzi.hamava.data.model.AudioSourceType
import com.afrouzi.hamava.data.model.DubSettings
import com.afrouzi.hamava.data.model.DubStatus
import com.afrouzi.hamava.ui.theme.ErrorColor
import com.afrouzi.hamava.ui.theme.PrimaryPurple
import com.afrouzi.hamava.ui.theme.TealActive

@Composable
fun StatusCard(
    status: DubStatus,
    latencyMs: Long,
    settings: DubSettings,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (status) {
            DubStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
            DubStatus.CONNECTING -> PrimaryPurple
            DubStatus.ACTIVE_LISTENING, DubStatus.ACTIVE_SPEAKING -> TealActive
            DubStatus.ERROR -> ErrorColor
            DubStatus.PAUSED -> Color(0xFFFFB74D)
        },
        label = "status_dot_color"
    )

    val statusText = when (status) {
        DubStatus.IDLE -> stringResource(R.string.status_ready)
        DubStatus.CONNECTING -> stringResource(R.string.status_connecting)
        DubStatus.ACTIVE_LISTENING -> stringResource(R.string.status_listening)
        DubStatus.ACTIVE_SPEAKING -> stringResource(R.string.status_speaking)
        DubStatus.ERROR -> stringResource(R.string.status_error)
        DubStatus.PAUSED -> stringResource(R.string.status_paused)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Status with Dot
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${if (settings.audioSource == AudioSourceType.MIC) stringResource(R.string.source_mic) else stringResource(R.string.source_system)} ➔ ${if (settings.appLanguage == "fa") settings.targetLanguage.nameFa else settings.targetLanguage.nameEn}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Latency indicator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = stringResource(R.string.latency_label),
                    tint = if (latencyMs > 0) TealActive else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (latencyMs > 0) stringResource(R.string.latency_ms, latencyMs) else "-- ms",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (latencyMs > 0) TealActive else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
