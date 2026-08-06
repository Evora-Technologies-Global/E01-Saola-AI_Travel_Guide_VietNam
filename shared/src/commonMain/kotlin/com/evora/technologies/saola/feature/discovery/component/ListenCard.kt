package com.evora.technologies.saola.feature.discovery.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.InkBrown
import com.evora.technologies.saola.core.designsystem.theme.Marigold
import com.evora.technologies.saola.core.designsystem.theme.PaperCream
import com.evora.technologies.saola.core.designsystem.theme.Pill
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.domain.model.AppLanguage
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.discovery_listen
import com.evora.technologies.saola.resources.discovery_listen_meta
import com.evora.technologies.saola.resources.discovery_listen_title
import com.evora.technologies.saola.resources.discovery_stop
import org.jetbrains.compose.resources.stringResource

/**
 * The narration, offered as a thing to press rather than an icon to find.
 *
 * Fixed ink-and-marigold rather than scheme colours: this is the one block on a pale page that
 * has to read as a different object — a player, not a paragraph. It is also the reason the
 * page can be put in a pocket, so it sits high on both arrangements.
 */
@Composable
internal fun ListenCard(
    discovery: Discovery,
    isSpeaking: Boolean,
    language: AppLanguage,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = InkBrown,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(PLAY_SIZE).clip(CircleShape).background(Marigold),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (isSpeaking) Res.string.discovery_stop else Res.string.discovery_listen,
                    ),
                    tint = InkBrown,
                    modifier = Modifier.size(PLAY_ICON_SIZE),
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.discovery_listen_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = PaperCream,
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = stringResource(
                        Res.string.discovery_listen_meta,
                        discovery.narrationMinutes(),
                        language.displayName,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = PaperCream.copy(alpha = META_ALPHA),
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Waveform(isPlaying = isSpeaking)
        }
    }
}

/** Four bars that only move while the voice is actually speaking. */
@Composable
private fun Waveform(isPlaying: Boolean) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val heights = WAVEFORM_BARS.mapIndexed { index, resting ->
        val animated by transition.animateFloat(
            initialValue = WAVE_FLOOR,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = WAVE_BASE_MILLIS + index * WAVE_STEP_MILLIS),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bar$index",
        )
        if (isPlaying) animated else resting
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
        modifier = Modifier.height(WAVE_HEIGHT),
    ) {
        heights.forEachIndexed { index, fraction ->
            Box(
                modifier = Modifier
                    .width(BAR_WIDTH)
                    .height(WAVE_HEIGHT * fraction)
                    .clip(Pill)
                    .background(
                        if (index % 2 == 0) Marigold else PaperCream.copy(alpha = BAR_ALPHA),
                    ),
            )
        }
    }
}

/**
 * How long the voice guide will talk, rounded to whole minutes.
 *
 * Estimated from the text rather than measured, because the engine only knows once it has
 * finished speaking — and by then the number is no longer a decision aid.
 */
private fun Discovery.narrationMinutes(): Int {
    val characters = summary.length +
        sections.sumOf { it.title.length + it.body.length } +
        funFacts.sumOf { it.length }
    return (characters / CHARACTERS_PER_MINUTE).coerceAtLeast(1)
}

/** Roughly 150 words a minute at an average five characters plus a space. */
private const val CHARACTERS_PER_MINUTE = 800

private val PLAY_SIZE = 48.dp
private val PLAY_ICON_SIZE = 26.dp
private const val META_ALPHA = 0.7f

/** Resting heights of the waveform bars, as a fraction of the row. */
private val WAVEFORM_BARS = listOf(0.45f, 0.75f, 0.55f, 0.9f)
private val WAVE_HEIGHT = 28.dp
private val BAR_WIDTH = 3.dp
private const val BAR_ALPHA = 0.55f

/** Never fully collapsed: a bar at zero reads as a dead channel rather than a quiet one. */
private const val WAVE_FLOOR = 0.35f

/** Each bar a little slower than the last, so the four never beat in unison. */
private const val WAVE_BASE_MILLIS = 520
private const val WAVE_STEP_MILLIS = 130
