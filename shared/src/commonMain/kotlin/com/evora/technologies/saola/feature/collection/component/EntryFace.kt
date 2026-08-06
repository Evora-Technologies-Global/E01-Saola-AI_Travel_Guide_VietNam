package com.evora.technologies.saola.feature.collection.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.component.AppAsyncImage
import com.evora.technologies.saola.core.designsystem.theme.Pill
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.util.accentColor
import com.evora.technologies.saola.domain.model.CollectionEntry
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.collection_collected_badge
import com.evora.technologies.saola.resources.collection_locked_hint
import com.evora.technologies.saola.resources.collection_open_discovery
import org.jetbrains.compose.resources.stringResource

/**
 * One thing to find, and the two faces it has.
 *
 * Collected and uncollected are deliberately the same silhouette — same square, same corners —
 * so a page of them reads as one set with parts of it filled in, rather than as two different
 * lists interleaved. What changes is only what is inside: a photograph, or the hatching that
 * stands for one not taken yet. That is why all three composables share this file: they are one
 * decision seen twice, and a change to the corner that reached only one of them would split the
 * board in half.
 *
 * Sized entirely by [modifier], which is what lets the same face be a square on the board and a
 * thumbnail beside a sentence in the guide. It carries no size of its own on purpose: a default
 * here would be a third opinion about how big the thing is, and the two callers already have
 * one each.
 *
 * @param isClickable false when the caller has made something larger clickable and this sits
 *   inside it. The guide row does exactly that — a 64 dp square inside a full-width row is a
 *   smaller target than the row it sits in, and two nested click regions would give one row two
 *   answers and a screen reader two nodes for one entry.
 */
@Composable
internal fun EntryFace(
    entry: CollectionEntry,
    onOpenDiscovery: (String) -> Unit,
    onShowHint: (String) -> Unit,
    modifier: Modifier = Modifier,
    isClickable: Boolean = true,
) {
    val name = entry.item.name
    val openLabel = stringResource(Res.string.collection_open_discovery)
    val hintLabel = stringResource(Res.string.collection_locked_hint)

    Box(modifier = modifier) {
        val discovery = entry.discovery
        if (discovery != null) {
            AppAsyncImage(
                model = discovery.imagePath,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.medium,
                contentScale = ContentScale.Crop,
                onClick = { onOpenDiscovery(discovery.id) }.takeIf { isClickable },
                onClickLabel = openLabel,
            )
            CollectedBadge(
                accent = entry.item.category.accentColor,
                modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.xs),
            )
        } else {
            LockedFace(
                onClick = { onShowHint(entry.item.id) }.takeIf { isClickable },
                onClickLabel = hintLabel,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * The hatched square that stands in for a photograph not taken yet.
 *
 * Hatching rather than a flat grey block, and no padlock: a locked tile would say the app is
 * withholding something, when in fact nothing is locked at all — the traveller simply has not
 * been there. The magnifier reads as "go and look", which is the actual instruction.
 */
@Composable
private fun LockedFace(
    onClick: (() -> Unit)?,
    onClickLabel: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val ground = MaterialTheme.colorScheme.surfaceContainerHigh
    val stripe = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = STRIPE_ALPHA)

    val hatching = Modifier
        .fillMaxSize()
        .drawBehind {
            drawRect(ground)
            // 45° hatching, started a full height to the left so the first stripe still
            // crosses the top-left corner.
            val step = STRIPE_STEP_DP.dp.toPx()
            val width = STRIPE_WIDTH_DP.dp.toPx()
            var x = -size.height
            while (x < size.width) {
                drawLine(
                    color = stripe,
                    start = Offset(x, size.height),
                    end = Offset(x + size.height, 0f),
                    strokeWidth = width,
                )
                x += step
            }
        }

    // The two `Surface` overloads rather than the clickable one with `enabled = false`: a
    // disabled node is announced as disabled, and a face inside a clickable row is not
    // disabled — it is simply not the thing that was made clickable.
    if (onClick == null) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            color = Color.Transparent,
        ) {
            Box(modifier = hatching, contentAlignment = Alignment.Center) {
                HintGlyph(contentDescription = contentDescription)
            }
        }
    } else {
        Surface(
            onClick = onClick,
            // `Surface` has no click-label parameter, so the label is put on the node
            // directly — without it a screen reader announces the name and leaves what
            // the tap does to guesswork.
            modifier = modifier.semantics { onClick(label = onClickLabel, action = null) },
            shape = MaterialTheme.shapes.medium,
            color = Color.Transparent,
        ) {
            Box(modifier = hatching, contentAlignment = Alignment.Center) {
                HintGlyph(contentDescription = contentDescription)
            }
        }
    }
}

/** The magnifier at the centre of an uncollected face, and the name a screen reader reads. */
@Composable
private fun HintGlyph(contentDescription: String) {
    Icon(
        imageVector = Icons.Outlined.Search,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ICON_ALPHA),
        modifier = Modifier.size(HINT_ICON),
    )
}

/** A tick in the category's colour, so a filled face reads as collected at a glance. */
@Composable
private fun CollectedBadge(accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(BADGE)
            .clip(Pill)
            .background(accent),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = stringResource(Res.string.collection_collected_badge),
            tint = Color.White,
            modifier = Modifier.size(BADGE_TICK),
        )
    }
}

/** The face's own marks — a badge and the magnifier, sized against a square, not a scale. */
private val BADGE = 20.dp
private val BADGE_TICK = 13.dp
private val HINT_ICON = 22.dp

private const val STRIPE_ALPHA = 0.11f
private const val STRIPE_STEP_DP = 11f
private const val STRIPE_WIDTH_DP = 4f
private const val ICON_ALPHA = 0.45f
