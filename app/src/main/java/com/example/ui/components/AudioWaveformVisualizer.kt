package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.RadioAmber
import com.example.ui.theme.RadioCyan
import com.example.ui.theme.RadioGreen
import com.example.ui.theme.RadioRed
import kotlin.random.Random

@Composable
fun AudioWaveformVisualizer(
    audioLevel: Float,
    isTransmitting: Boolean,
    isReceiving: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 28
    val barHeights = remember { List(barCount) { Animatable(0.08f) } }

    LaunchedEffect(audioLevel, isTransmitting, isReceiving) {
        val baseColorActive = isTransmitting || isReceiving
        for (i in 0 until barCount) {
            val target = if (baseColorActive) {
                val distanceFactor = 1f - kotlin.math.abs(i - barCount / 2f) / (barCount / 2f)
                val jitter = (Random.nextFloat() * 0.35f)
                (audioLevel * 1.2f * distanceFactor + jitter * audioLevel).coerceIn(0.1f, 1.0f)
            } else {
                0.08f
            }
            barHeights[i].animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 60, easing = FastOutSlowInEasing)
            )
        }
    }

    val primaryColor = when {
        isTransmitting -> RadioAmber
        isReceiving -> RadioGreen
        else -> RadioCyan.copy(alpha = 0.4f)
    }

    val secondaryColor = when {
        isTransmitting -> RadioRed
        isReceiving -> RadioCyan
        else -> RadioCyan.copy(alpha = 0.2f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barSpacing = 4.dp.toPx()
        val totalSpacing = barSpacing * (barCount - 1)
        val barWidth = (totalWidth - totalSpacing) / barCount
        val centerY = canvasHeight / 2f

        for (i in 0 until barCount) {
            val h = (barHeights[i].value * canvasHeight * 0.9f).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidth + barSpacing)
            val y = centerY - h / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor, secondaryColor),
                    startY = y,
                    endY = y + h
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
