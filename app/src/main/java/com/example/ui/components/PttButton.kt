package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.RadioAmber
import com.example.ui.theme.RadioCyan
import com.example.ui.theme.RadioGreen
import com.example.ui.theme.RadioRed
import com.example.ui.theme.RadioSurface
import com.example.ui.theme.RadioSurfaceElevated
import com.example.ui.theme.RadioSurfaceVariant

@Composable
fun PttButton(
    isTransmitting: Boolean,
    isReceiving: Boolean,
    isFloorBusy: Boolean,
    floorHolderName: String?,
    hasRecipients: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ptt_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    val activeColor = when {
        isTransmitting -> RadioAmber
        isReceiving -> RadioGreen
        isFloorBusy -> RadioRed
        hasRecipients -> RadioCyan
        else -> Color.Gray
    }

    val buttonGradient = when {
        isTransmitting -> Brush.radialGradient(
            colors = listOf(RadioAmber, RadioRed, Color(0xFF5A1400))
        )
        isReceiving -> Brush.radialGradient(
            colors = listOf(RadioGreen, Color(0xFF007E33), Color(0xFF003314))
        )
        isFloorBusy -> Brush.radialGradient(
            colors = listOf(RadioRed.copy(alpha = 0.7f), Color(0xFF3E1210))
        )
        else -> Brush.radialGradient(
            colors = listOf(RadioSurfaceElevated, RadioSurfaceVariant, RadioSurface)
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(240.dp)
            .testTag("ptt_container")
    ) {
        // Animated Radar/Sound Wave Rings when transmitting or receiving
        if (isTransmitting || isReceiving) {
            Canvas(modifier = Modifier.size(240.dp)) {
                val baseRadius = (160.dp.toPx() / 2f)
                val currentRadius = baseRadius * pulseScale
                drawCircle(
                    color = activeColor.copy(alpha = pulseAlpha),
                    radius = currentRadius,
                    center = center
                )
                drawCircle(
                    color = activeColor.copy(alpha = pulseAlpha * 0.5f),
                    radius = currentRadius * 0.85f,
                    center = center
                )
            }
        }

        // Tactical Outer Metal Bezel
        Box(
            modifier = Modifier
                .size(176.dp)
                .clip(CircleShape)
                .background(RadioSurfaceVariant)
                .border(3.dp, activeColor.copy(alpha = if (isTransmitting) 1f else 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Main Interactive PTT Core
            Box(
                modifier = Modifier
                    .size(152.dp)
                    .shadow(elevation = if (isTransmitting) 16.dp else 6.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(buttonGradient)
                    .border(2.dp, activeColor, CircleShape)
                    .testTag("ptt_main_button")
                    .pointerInput(isFloorBusy) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            onPress()
                            waitForUpOrCancellation()
                            onRelease()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isReceiving) Icons.Default.VolumeUp else Icons.Default.Mic,
                        contentDescription = "Push To Talk",
                        tint = if (isTransmitting || isReceiving) Color.White else activeColor,
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            isTransmitting -> stringResource(R.string.status_transmitting)
                            isReceiving -> "INCOMING"
                            isFloorBusy -> "FLOOR BUSY"
                            !hasRecipients -> "NO PEERS"
                            else -> stringResource(R.string.hold_to_talk)
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = if (isTransmitting || isReceiving) Color.White else activeColor
                    )
                    if (isFloorBusy && floorHolderName != null) {
                        Text(
                            text = floorHolderName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}
