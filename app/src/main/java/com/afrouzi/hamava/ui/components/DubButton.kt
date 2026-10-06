/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afrouzi.hamava.R
import com.afrouzi.hamava.data.model.DubStatus
import com.afrouzi.hamava.ui.theme.PrimaryPurple
import com.afrouzi.hamava.ui.theme.PrimaryPurpleDark
import com.afrouzi.hamava.ui.theme.TealActive

@Composable
fun DubButton(
    status: DubStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = status == DubStatus.ACTIVE_LISTENING || status == DubStatus.ACTIVE_SPEAKING
    val isConnecting = status == DubStatus.CONNECTING
    val isPaused = status == DubStatus.PAUSED

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        ),
        label = "rotate_angle"
    )

    val buttonBgColor by animateColorAsState(
        targetValue = when {
            isPaused -> Color(0xFFFF9F0A)
            isActive -> TealActive
            isConnecting -> PrimaryPurpleDark
            else -> PrimaryPurple
        },
        animationSpec = tween(durationMillis = 350),
        label = "btn_color"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(170.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        ) {
            // Outer glowing pulse ring when active or paused
            if (isActive || isConnecting || isPaused) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(if (isActive) pulseScale else 1.05f)
                        .clip(CircleShape)
                        .background(
                            when {
                                isPaused -> Color(0xFFFF9F0A).copy(alpha = 0.22f)
                                isActive -> TealActive.copy(alpha = 0.22f)
                                else -> PrimaryPurple.copy(alpha = 0.25f)
                            }
                        )
                )
            }

            // Central button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(128.dp)
                    .shadow(
                        elevation = if (isActive || isPaused) 16.dp else 8.dp,
                        shape = CircleShape,
                        spotColor = buttonBgColor,
                        ambientColor = buttonBgColor
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = when {
                                isPaused -> listOf(Color(0xFFFF9F0A), Color(0xFFD47A00))
                                isActive -> listOf(TealActive, Color(0xFF00A887))
                                else -> listOf(PrimaryPurple, PrimaryPurpleDark)
                            }
                        )
                    )
            ) {
                when {
                    isConnecting -> {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = stringResource(R.string.status_connecting),
                            tint = Color.White,
                            modifier = Modifier
                                .size(48.dp)
                                .rotate(rotateAngle)
                        )
                    }
                    isPaused -> {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.resume_dubbing),
                            tint = Color(0xFF0F0F1A),
                            modifier = Modifier.size(54.dp)
                        )
                    }
                    isActive -> {
                        Icon(
                            imageVector = if (status == DubStatus.ACTIVE_SPEAKING) Icons.Default.GraphicEq else Icons.Default.Pause,
                            contentDescription = stringResource(R.string.pause_dubbing),
                            tint = Color(0xFF0F0F1A),
                            modifier = Modifier.size(50.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = stringResource(R.string.start_dubbing),
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when {
                isConnecting -> stringResource(R.string.status_connecting)
                isPaused -> stringResource(R.string.resume_dubbing)
                isActive -> stringResource(R.string.pause_dubbing)
                else -> stringResource(R.string.start_dubbing)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                isPaused -> Color(0xFFFF9F0A)
                isActive -> TealActive
                else -> MaterialTheme.colorScheme.onBackground
            },
            fontSize = 17.sp
        )
    }
}
