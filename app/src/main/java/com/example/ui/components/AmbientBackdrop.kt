package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IndigoOrb
import com.example.ui.theme.ObsidianBackground
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AmbientBackdrop(
    modifier: Modifier = Modifier,
    accentOverride: Color = EmeraldPrimary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambientOrbs")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbPhase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbPhase2"
    )

    Canvas(modifier = modifier) {
        drawRect(color = ObsidianBackground)

        val w = size.width
        val h = size.height
        val minDim = minOf(w, h).coerceAtLeast(1f)

        // Top-left drifting emerald/mode orb
        val orb1Center = Offset(
            x = w * 0.22f + cos(phase1) * w * 0.08f,
            y = h * 0.16f + sin(phase1) * h * 0.06f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentOverride.copy(alpha = 0.16f),
                    accentOverride.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                center = orb1Center,
                radius = minDim * 0.65f
            ),
            radius = minDim * 0.65f,
            center = orb1Center
        )

        // Bottom-right drifting cyan/indigo orb
        val orb2Center = Offset(
            x = w * 0.80f + sin(phase2) * w * 0.09f,
            y = h * 0.72f + cos(phase2) * h * 0.07f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    CyanAccent.copy(alpha = 0.12f),
                    IndigoOrb.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                center = orb2Center,
                radius = minDim * 0.7f
            ),
            radius = minDim * 0.7f,
            center = orb2Center
        )

        // Center-top soft indigo accent
        val orb3Center = Offset(
            x = w * 0.58f + cos(phase2 * 0.7f) * w * 0.06f,
            y = h * 0.34f + sin(phase1 * 0.8f) * h * 0.05f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    IndigoOrb.copy(alpha = 0.09f),
                    Color.Transparent
                ),
                center = orb3Center,
                radius = minDim * 0.5f
            ),
            radius = minDim * 0.5f,
            center = orb3Center
        )
    }
}
