package com.flashshare.p2p.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.flashshare.p2p.ui.theme.BrandBlue

/**
 * Subtle and clean radar wave component for device discovery.
 */
@Composable
fun PeerRadarComponent(
    modifier: Modifier = Modifier,
    radarColor: Color = BrandBlue
) {
    val transition = rememberInfiniteTransition(label = "subtleRadar")

    val wave1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )

    val wave2 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    Box(
        modifier = modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(150.dp)) {
            val maxRadius = size.minDimension / 2

            // Static inner subtle ring
            drawCircle(
                color = radarColor.copy(alpha = 0.15f),
                radius = maxRadius * 0.35f,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Animated subtle rings
            listOf(wave1, wave2).forEach { wave ->
                val alpha = (1.0f - wave).coerceIn(0f, 1f) * 0.4f
                drawCircle(
                    color = radarColor.copy(alpha = alpha),
                    radius = maxRadius * wave,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        Icon(
            imageVector = Icons.Default.NearMe,
            contentDescription = null,
            tint = radarColor,
            modifier = Modifier.size(28.dp)
        )
    }
}
