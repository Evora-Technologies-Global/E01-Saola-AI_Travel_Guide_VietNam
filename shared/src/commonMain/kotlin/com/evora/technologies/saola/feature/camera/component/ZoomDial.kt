package com.evora.technologies.saola.feature.camera.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.InkBrown
import com.evora.technologies.saola.core.designsystem.theme.Marigold
import com.evora.technologies.saola.core.designsystem.theme.Pill
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.StampType
import com.evora.technologies.saola.feature.camera.CameraCapabilities
import com.evora.technologies.saola.feature.camera.CameraController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Drives the zoom so a tap glides and a gesture does not.
 *
 * Jumping straight to 2× reads as a glitch rather than a zoom; ramping over a
 * quarter of a second is the whole difference. A finger already on the glass is a
 * different matter — it owns the ratio outright, so any glide still running is
 * abandoned rather than fought.
 */
@Composable
internal fun rememberZoomDriver(controller: CameraController): ZoomDriver {
    val scope = rememberCoroutineScope()
    return remember(controller, scope) { ZoomDriver(controller, scope) }
}

internal class ZoomDriver(
    private val controller: CameraController,
    private val scope: CoroutineScope,
) {
    private var glide: Job? = null

    fun glideTo(ratio: Float) {
        glide?.cancel()
        glide = scope.launch {
            animate(
                initialValue = controller.requestedZoom(),
                targetValue = ratio,
                animationSpec = tween(ZOOM_GLIDE_MILLIS, easing = FastOutSlowInEasing),
            ) { value, _ -> controller.setZoomRatio(value) }
        }
    }

    fun pinch(scale: Float) {
        glide?.cancel()
        controller.zoomBy(scale)
    }

    fun dial(progressDelta: Float) {
        glide?.cancel()
        controller.zoomByProgress(progressDelta)
    }
}

/**
 * A zoom dial, borrowed from the pro modes: an arc of ticks that turns under a
 * fixed pointer.
 *
 * It is the whole zoom UI — a row of preset chips beside it would have said the
 * same thing twice — so it has to answer all three questions at once: where the
 * zoom is (the number over the pointer), where it can go (the labelled ticks), and
 * how to get there (turn it, or tap a tick to glide).
 */
@Composable
internal fun ZoomDial(
    capabilities: CameraCapabilities,
    onZoomProgress: (Float) -> Unit,
    onZoomGlideTo: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()
    // Shadowed instead of panelled: with the dial down to a couple of hundred dp, a
    // backdrop behind it became a floating grey slab. Each mark carries its own
    // legibility over a bright wall instead.
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = Color.White,
        shadow = Shadow(color = Color.Black.copy(alpha = 0.7f), blurRadius = 8f),
    )
    val stops = remember(capabilities.minZoomRatio, capabilities.maxZoomRatio) {
        capabilities.zoomStops()
    }
    val progress = capabilities.zoomProgress()

    // Haptics per notch, so the dial can be turned by feel while the eye stays on
    // the subject — the reason a physical dial has detents at all.
    var lastNotch by remember { mutableIntStateOf(-1) }
    LaunchedEffect(progress) {
        val notch = (progress * DIAL_NOTCHES).roundToInt()
        if (lastNotch >= 0 && notch != lastNotch) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        lastNotch = notch
    }

    Box(
        modifier = modifier
            // Narrow rather than full-bleed: a rail running the whole width read as
            // chrome bolted to the screen. A drag that starts here keeps being
            // delivered past the edges, so the shorter dial costs no reach.
            .width(DIAL_WIDTH)
            .height(DIAL_HEIGHT)
            .pointerInput(capabilities.minZoomRatio, capabilities.maxZoomRatio) {
                val radius = size.width * DIAL_RADIUS_FACTOR
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    // Dragging the dial left brings the higher numbers round to the
                    // pointer, exactly as turning a physical ring would.
                    onZoomProgress(-dragAmount / (radius * DIAL_SWEEP_RADIANS))
                }
            }
            .pointerInput(capabilities.minZoomRatio, capabilities.maxZoomRatio, progress) {
                val radius = size.width * DIAL_RADIUS_FACTOR
                val centre = Offset(size.width / 2f, DIAL_ARC_TOP.toPx() + radius)
                detectTapGestures { tap ->
                    // Tapping a tick is the gesture the preset chips used to serve:
                    // land on it by gliding, so the frame travels rather than jumps.
                    val angle = atan2(tap.x - centre.x, centre.y - tap.y)
                    val target = (progress + angle / DIAL_SWEEP_RADIANS).coerceIn(0f, 1f)
                    onZoomGlideTo(capabilities.zoomRatioAt(target))
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.width * DIAL_RADIUS_FACTOR
            val centre = Offset(size.width / 2f, DIAL_ARC_TOP.toPx() + radius)

            fun directionAt(value: Float): Offset? {
                val angle = (value - progress) * DIAL_SWEEP_RADIANS
                if (abs(angle) > DIAL_VISIBLE_RADIANS) return null
                return Offset(sin(angle), -cos(angle))
            }

            fun fadeAt(value: Float): Float {
                val angle = (value - progress) * DIAL_SWEEP_RADIANS
                return (1f - (abs(angle) / DIAL_VISIBLE_RADIANS).pow(2)).coerceIn(0f, 1f)
            }

            /** Draws a mark twice: a dark, wider stroke first, so it reads over anything. */
            fun drawMark(direction: Offset, length: Dp, width: Dp, alpha: Float) {
                val start = centre + direction * radius
                val end = centre + direction * (radius - length.toPx())
                drawLine(
                    color = Color.Black.copy(alpha = 0.4f * alpha),
                    start = start,
                    end = end,
                    strokeWidth = width.toPx() + 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.White.copy(alpha = alpha),
                    start = start,
                    end = end,
                    strokeWidth = width.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            // The arc itself, drawn whether or not there are ticks over it. At 1× the
            // range runs out to the left of the pointer, and without a rail under it
            // the empty half reads as a UI that failed to load rather than a dial
            // wound to its stop.
            drawArc(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to Color.White.copy(alpha = 0.4f),
                    1f to Color.Transparent,
                ),
                startAngle = -90f - DIAL_VISIBLE_DEGREES,
                sweepAngle = DIAL_VISIBLE_DEGREES * 2f,
                useCenter = false,
                topLeft = Offset(centre.x - radius, centre.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = 1.dp.toPx()),
            )

            for (notch in 0..DIAL_NOTCHES) {
                val value = notch / DIAL_NOTCHES.toFloat()
                val direction = directionAt(value) ?: continue
                drawMark(direction, length = 9.dp, width = 1.5.dp, alpha = 0.8f * fadeAt(value))
            }

            stops.forEach { stop ->
                val value = capabilities.zoomProgressOf(stop)
                val direction = directionAt(value) ?: return@forEach
                val fade = fadeAt(value)
                drawMark(direction, length = 15.dp, width = 2.dp, alpha = 0.95f * fade)
                // The stop under the pointer is left unlabelled: the readout above it
                // already says the same number, live.
                if (abs((value - progress) * DIAL_SWEEP_RADIANS) < DIAL_LABEL_GAP_RADIANS) {
                    return@forEach
                }
                val layout = measurer.measure(AnnotatedString(stop.formatZoom()), labelStyle)
                // Just under its own tick rather than a third of the way down the
                // face: hung lower, the labels needed a band of canvas below the
                // ticks that was empty the rest of the time, and it held the whole
                // dial off the bottom of the frame.
                val anchor = centre + direction * (radius - 24.dp.toPx())
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = anchor.x - layout.size.width / 2f,
                        y = anchor.y - layout.size.height / 2f,
                    ),
                    alpha = fade,
                )
            }

            val up = Offset(0f, -1f)
            drawLine(
                color = Color.Black.copy(alpha = 0.4f),
                start = centre + up * (radius + 5.dp.toPx()),
                end = centre + up * (radius - 18.dp.toPx()),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Marigold,
                start = centre + up * (radius + 5.dp.toPx()),
                end = centre + up * (radius - 18.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // The live ratio, over the pointer. Without the chips this is the only place
        // the traveller can read what the zoom is actually doing.
        Text(
            text = capabilities.zoomRatio.formatZoom(),
            style = StampType.ordinal,
            color = InkBrown,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .clip(Pill)
                .background(Marigold)
                .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
        )
    }
}

/** How long a tap on a tick takes to travel there. */
private const val ZOOM_GLIDE_MILLIS = 260

private val DIAL_WIDTH = 164.dp

/**
 * Only as tall as the arc actually reaches.
 *
 * The canvas below the ticks is empty — the circle carries on downwards off the
 * screen — so every dp of it was pure gap between the dial and the frame's edge.
 */
private val DIAL_HEIGHT = 68.dp

/**
 * The dial's geometry: a circle far below the screen, of which only the top arc shows.
 *
 * A wide radius is what makes it read as a dial rather than a bent slider — the ticks
 * stay nearly upright, and the curve is felt more than seen.
 */
private const val DIAL_RADIUS_FACTOR = 1.35f

/**
 * Radians the whole zoom range spans, and how much of it is on screen at once.
 *
 * Measured on the device rather than guessed. The narrower dial turns the same
 * angle over a third of the pixels, so the sweep widens to keep the feel: about
 * half the range crosses the face in one drag, which still puts two labelled stops
 * in view — the thing that tells the traveller where to go next now that the chips
 * are gone.
 */
private const val DIAL_SWEEP_RADIANS = 1.4f
private const val DIAL_VISIBLE_RADIANS = 0.35f
private const val DIAL_VISIBLE_DEGREES = 20f

/** How close to the pointer a tick label has to be before the readout speaks for it. */
private const val DIAL_LABEL_GAP_RADIANS = 0.1f
private const val DIAL_NOTCHES = 40

/** Leaves room above the arc for the live-ratio readout that sits over the pointer. */
private val DIAL_ARC_TOP = 32.dp
