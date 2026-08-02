package com.duylt.trave.vietlensai.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.action_back
import com.duylt.trave.vietlensai.resources.action_retry
import com.duylt.trave.vietlensai.core.designsystem.theme.Marigold
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion

/**
 * Full-screen loading state with a rotating hint.
 *
 * A recognition round-trip is 3-8 seconds — long enough that a bare spinner reads
 * as "stuck". Telling the traveller what the AI is doing turns dead time into
 * something that feels deliberate.
 */
@Composable
fun LoadingState(
    message: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(ScreenGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (detail != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(ScreenGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(24.dp))
            FilledTonalButton(onClick = onRetry) {
                Text(stringResource(Res.string.action_retry))
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(ScreenGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

/** A pill that carries a category's own colour, used across cards and detail headers. */
@Composable
fun AccentChip(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = CHIP_BACKGROUND_ALPHA))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
        )
    }
}

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
    shape: Shape = RoundedCornerShape(12.dp),
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

/** How long the band takes to cross the frame once. */
private const val SHIMMER_SWEEP_MILLIS = 1400

/** Half the width of the band, as a fraction of the frame. */
private const val SHIMMER_BAND = 0.45f

/**
 * Whether something drawn on [color] should be treated as sitting on a light surface.
 *
 * Alpha counts as well as luminance: a nearly transparent white laid over the black of
 * the photo viewer is a dark surface in every way that matters to the eye, and reading
 * only its luminance would call it light and put black ink on black.
 */
internal fun isLightSurface(color: Color): Boolean =
    color.luminance() > 0.5f && color.alpha > 0.5f

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

/**
 * Small caps with wide tracking — the label that names a block without shouting.
 *
 * Monospaced on purpose: the kickers read as stamped-on captions beside the
 * proportional body type, the way a museum label sits beside its object.
 */
@Composable
fun Kicker(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * The back affordance on pushed screens: a round chip rather than a bare arrow.
 *
 * Screens like the passport put their title in the page itself instead of in a
 * `TopAppBar`, and an unbounded arrow floating beside a headline reads as part of
 * the heading. The chip gives it its own footprint and a 40 dp target.
 */
@Composable
fun BackChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    IconChip(
        onClick = onClick,
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = stringResource(Res.string.action_back),
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
    )
}

/**
 * The same chip with a cross in it, for a screen that is dismissed rather than
 * navigated away from.
 *
 * The sovereignty statement is the one screen that opens by itself, on first
 * launch. A back arrow there would point at nothing the traveller has seen.
 */
@Composable
fun CloseChip(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    IconChip(
        onClick = onClick,
        icon = Icons.Filled.Close,
        contentDescription = contentDescription,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
    )
}

@Composable
private fun IconChip(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * How much of a collection is filled in, drawn as a gauge rather than as progress.
 *
 * Two boxes rather than `LinearProgressIndicator`, which since M3 1.3 puts a gap and a
 * stop dot at the leading edge. That is correct for a determinate task — something is
 * running and will finish — and wrong here: nothing is in flight, the bar is a fill
 * level, and the stop dot reads as a target the traveller is falling short of.
 *
 * Shared by the passport and the culture collection, which are the same idea counted
 * along two different axes. They are meant to look like siblings, and the surest way
 * to keep them that way is for there to be one bar rather than two that agree today.
 */
@Composable
fun FillGauge(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = Marigold.copy(alpha = GAUGE_TRACK_ALPHA),
    fillColor: Color = Vermilion,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(CircleShape)
                .background(fillColor),
        )
    }
}

private const val GAUGE_TRACK_ALPHA = 0.32f

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(contentPadding),
    )
}

private const val CHIP_BACKGROUND_ALPHA = 0.14f
