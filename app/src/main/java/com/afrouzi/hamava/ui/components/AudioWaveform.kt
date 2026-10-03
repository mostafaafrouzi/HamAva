/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.afrouzi.hamava.ui.theme.PrimaryPurple
import com.afrouzi.hamava.ui.theme.TealActive
import com.afrouzi.hamava.ui.theme.WaveformInactiveColor
import kotlin.random.Random

@Composable
fun AudioWaveform(
    isActive: Boolean,
    isSpeaking: Boolean,
    rmsLevel: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 36
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    val randomFactors = remember {
        FloatArray(barCount) { 0.4f + Random.nextFloat() * 0.6f }
    }

    val activeBrush = remember(isSpeaking) {
        if (isSpeaking) {
            Brush.verticalGradient(
                colors = listOf(TealActive, PrimaryPurple)
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(PrimaryPurple, TealActive)
            )
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barWidth = (totalWidth / barCount) * 0.55f
        val spacing = (totalWidth - (barWidth * barCount)) / (barCount - 1)

        val minHeight = 6.dp.toPx()
        val maxHeight = canvasHeight * 0.88f

        for (i in 0 until barCount) {
            val x = i * (barWidth + spacing)

            val calculatedHeight: Float = if (isActive) {
                val distanceToCenter = kotlin.math.abs(i - barCount / 2f) / (barCount / 2f)
                val curve = 1f - (distanceToCenter * 0.45f)
                val dynamicRms = (rmsLevel * 2.5f).coerceIn(0.12f, 1f)
                val waveOffset = kotlin.math.sin((i.toDouble() / barCount * 2 * Math.PI) + (phase * 2 * Math.PI)).toFloat() * 0.2f
                val h = (maxHeight * dynamicRms * curve * randomFactors[i] + waveOffset * maxHeight).coerceIn(minHeight, maxHeight)
                h
            } else {
                minHeight
            }

            val y = (canvasHeight - calculatedHeight) / 2f

            if (isActive) {
                drawRoundRect(
                    brush = activeBrush,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, calculatedHeight),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
            } else {
                drawRoundRect(
                    color = WaveformInactiveColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, calculatedHeight),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
            }
        }
    }
}
