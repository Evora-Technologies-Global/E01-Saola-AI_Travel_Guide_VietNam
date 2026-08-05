package com.duylt.trave.vietlensai.feature.camera.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow

/** The self-timer, counted down over the live frame so the pose can be adjusted. */
@Composable
internal fun CountdownOverlay(seconds: Int, modifier: Modifier = Modifier) {
    val pulse = remember { Animatable(COUNTDOWN_PULSE) }
    LaunchedEffect(seconds) {
        pulse.snapTo(COUNTDOWN_PULSE)
        pulse.animateTo(1f, tween(durationMillis = 400))
    }

    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = seconds.toString(),
            // Shadowed rather than scrimmed: the countdown is read against whatever
            // is being framed, and dimming the whole frame to make one digit legible
            // would hide the thing the traveller is still composing.
            style = MaterialTheme.typography.displayLarge.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.6f),
                    offset = Offset.Zero,
                    blurRadius = 24f,
                ),
            ),
            color = Color.White,
            modifier = Modifier.scale(pulse.value),
        )
    }
}

private const val COUNTDOWN_PULSE = 1.35f
