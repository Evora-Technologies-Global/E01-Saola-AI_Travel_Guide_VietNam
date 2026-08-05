package com.evora.technologies.saola.feature.camera.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.InkBrown
import com.evora.technologies.saola.core.designsystem.theme.Marigold
import com.evora.technologies.saola.core.designsystem.theme.Pill
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.StampType
import com.evora.technologies.saola.feature.camera.CameraCapabilities
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * The tap-to-focus reticle, with the exposure slider the stock camera puts beside it.
 *
 * It snaps in, holds, and fades out on its own — unless the focus is locked or the
 * traveller is still dragging the sun, either of which means they are not finished.
 */
@Composable
internal fun FocusReticle(
    position: Offset,
    locked: Boolean,
    capabilities: CameraCapabilities,
    frameSize: IntSize,
    onExposureIndex: (Int) -> Unit,
    onFinished: () -> Unit,
) {
    val density = LocalDensity.current
    val ringPx = with(density) { FOCUS_RING_SIZE.toPx() }
    val trackPx = with(density) { EV_TRACK_HEIGHT.toPx() }
    val trackWidthPx = with(density) { EV_TRACK_WIDTH.toPx() }

    val scale = remember { Animatable(FOCUS_RING_OVERSHOOT) }
    val fade = remember { Animatable(1f) }
    var adjusting by remember(position) { mutableStateOf(false) }
    var evAccumulator by remember(position) { mutableFloatStateOf(0f) }

    LaunchedEffect(position) {
        scale.snapTo(FOCUS_RING_OVERSHOOT)
        scale.animateTo(1f, tween(durationMillis = 200))
    }
    LaunchedEffect(position, locked, adjusting) {
        fade.snapTo(1f)
        if (locked || adjusting) return@LaunchedEffect
        delay(FOCUS_RETICLE_HOLD_MILLIS)
        fade.animateTo(0f, tween(durationMillis = 350))
        onFinished()
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (position.x - ringPx / 2f).roundToInt(),
                    y = (position.y - ringPx / 2f).roundToInt(),
                )
            }
            .size(FOCUS_RING_SIZE)
            .scale(scale.value)
            .alpha(fade.value)
            .border(
                width = if (locked) 2.5.dp else 1.5.dp,
                color = Marigold,
                shape = CircleShape,
            ),
    )

    if (!capabilities.canAdjustExposure) return

    // Beside the ring, and on whichever side has room — a slider half off the screen
    // cannot be dragged.
    val onRight = position.x + ringPx / 2f + trackWidthPx < frameSize.width
    val trackX = if (onRight) {
        position.x + ringPx / 2f + with(density) { 4.dp.toPx() }
    } else {
        position.x - ringPx / 2f - trackWidthPx - with(density) { 4.dp.toPx() }
    }
    val span = (capabilities.exposureRange.last - capabilities.exposureRange.first).toFloat()

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = trackX.roundToInt(),
                    y = (position.y - trackPx / 2f).roundToInt(),
                )
            }
            .size(width = EV_TRACK_WIDTH, height = EV_TRACK_HEIGHT)
            .alpha(fade.value)
            // Swallows taps so adjusting exposure never doubles as a refocus on a
            // point a few pixels from the one already metered.
            .pointerInput(Unit) { detectTapGestures { } }
            .pointerInput(capabilities.exposureRange, position) {
                if (span <= 0f) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { adjusting = true },
                    onDragEnd = { adjusting = false },
                    onDragCancel = { adjusting = false },
                ) { change, dragAmount ->
                    change.consume()
                    // Up is brighter, the way every camera and every lift button works.
                    evAccumulator = (evAccumulator - dragAmount * span / trackPx)
                        .coerceIn(
                            capabilities.exposureRange.first.toFloat(),
                            capabilities.exposureRange.last.toFloat(),
                        )
                    onExposureIndex(evAccumulator.roundToInt())
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        val progress = if (span <= 0f) {
            0.5f
        } else {
            (capabilities.exposureIndex - capabilities.exposureRange.first) / span
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val x = size.width / 2f
            drawLine(
                color = Color.White.copy(alpha = 0.55f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.dp.toPx(),
            )
        }

        Icon(
            imageVector = Icons.Filled.WbSunny,
            contentDescription = null,
            tint = Marigold,
            modifier = Modifier
                .offset { IntOffset(0, ((1f - progress) * (trackPx - trackWidthPx)).roundToInt()) }
                .size(EV_TRACK_WIDTH)
                .padding(Spacing.xxs),
        )

        if (capabilities.exposureIndex != 0) {
            Text(
                text = capabilities.exposureEv.formatEv(),
                style = StampType.ordinal,
                color = InkBrown,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // Allowed past the track's width, which is only as wide as the
                    // sun: "+1.0" folded into two lines otherwise.
                    .wrapContentWidth(unbounded = true)
                    .clip(Pill)
                    .background(Marigold)
                    .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
            )
        }
    }
}

private val FOCUS_RING_SIZE = 72.dp
private const val FOCUS_RING_OVERSHOOT = 1.4f

/** Long enough to dial in exposure, short enough not to sit over the shot. */
private const val FOCUS_RETICLE_HOLD_MILLIS = 2_600L

private val EV_TRACK_HEIGHT = 168.dp
private val EV_TRACK_WIDTH = 28.dp
