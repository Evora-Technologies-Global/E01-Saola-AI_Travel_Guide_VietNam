package com.duylt.trave.vietlensai.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * Placeholder shown while something that will occupy this exact rectangle loads.
 *
 * A band of light sweeping across a filled shape rather than a spinner in a hole: the
 * page keeps its final geometry, so nothing jumps when the real content lands, and a
 * sweep that travels reads as progress in a way a pulsing rectangle does not.
 *
 * [shape] is the caller's, not this component's, and that is the point — the placeholder
 * for a photograph has to carry the same corners and sit inside the same border as the
 * photograph, or the moment of arrival is a change of outline rather than a change of
 * content.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    baseColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            // Linear and restarting: the band is meant to cross the frame and come round
            // again, and an eased sweep reads as something hesitating rather than working.
            animation = tween(durationMillis = SHIMMER_SWEEP_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerSweep",
    )
    val highlight = shimmerHighlight(baseColor)
    Box(
        modifier = modifier
            .clip(shape)
            // Drawn rather than composed: `progress` is read here and nowhere else, so a
            // shimmering tile invalidates its own draw each frame without recomposing —
            // which matters when six of them are sweeping inside a scrolling row.
            .drawBehind {
                drawRect(baseColor)
                // Opens with the band's centre on the left edge and travels until it is
                // clear of the right one. It deliberately does not begin off-screen and
                // sweep in: measured on a device, a photograph read from local storage
                // lands in about 150ms, and an approach long enough to be seen as an
                // approach is longer than the entire wait it exists to cover — the
                // highlight would arrive after the photograph it stood in for, leaving
                // the traveller a grey rectangle every time. Half the band is off the
                // left edge on the first frame, which reads as light already crossing.
                val head = size.width * progress * 2f
                drawRect(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, highlight, Color.Transparent),
                        startX = head - size.width * SHIMMER_BAND,
                        endX = head + size.width * SHIMMER_BAND,
                    ),
                )
            },
    )
}

/**
 * The sweep is white on both schemes — it is a highlight, and a highlight is light
 * hitting a surface — but a light surface has far less headroom above it, so the same
 * alpha that is barely visible on ink would blow sand out to paper.
 */
private fun shimmerHighlight(baseColor: Color): Color =
    if (isLightSurface(baseColor)) {
        Color.White.copy(alpha = 0.55f)
    } else {
        Color.White.copy(alpha = 0.16f)
    }

/** How long the band takes to cross the frame once. */
private const val SHIMMER_SWEEP_MILLIS = 1400

/** Half the width of the band, as a fraction of the frame. */
private const val SHIMMER_BAND = 0.45f
