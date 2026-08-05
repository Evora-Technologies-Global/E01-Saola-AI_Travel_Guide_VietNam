package com.duylt.trave.vietlensai.feature.collection.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.component.AppAsyncImage
import com.duylt.trave.vietlensai.core.designsystem.theme.Pill
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.util.accentColor
import com.duylt.trave.vietlensai.domain.model.CollectionEntry
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.collection_collected_badge
import com.duylt.trave.vietlensai.resources.collection_locked_hint
import com.duylt.trave.vietlensai.resources.collection_open_discovery
import org.jetbrains.compose.resources.stringResource

/**
 * One thing to find, and the two faces it has.
 *
 * Collected and uncollected are deliberately the same silhouette — same square, same corners,
 * same caption underneath — so the board reads as one grid with parts of it filled in, rather
 * than as two different lists interleaved. What changes is only what is inside: a photograph,
 * or the hatching that stands for one not taken yet. That is why all three composables share
 * this file: they are one decision seen twice, and a change to the corner or the caption that
 * reached only one of them would split the board in half.
 *
 * Tapping does different things on purpose. A collected tile goes straight to the discovery,
 * because the traveller already knows what it is and wants their own page back. An uncollected
 * one opens the hint, because the only useful thing the app can offer is help finding it.
 */
@Composable
internal fun CollectionTile(
    entry: CollectionEntry,
    onOpenDiscovery: (String) -> Unit,
    onShowHint: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = entry.item.name
    val openLabel = stringResource(Res.string.collection_open_discovery)
    val hintLabel = stringResource(Res.string.collection_locked_hint)

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            val discovery = entry.discovery
            if (discovery != null) {
                AppAsyncImage(
                    model = discovery.imagePath,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.medium,
                    contentScale = ContentScale.Crop,
                    onClick = { onOpenDiscovery(discovery.id) },
                    onClickLabel = openLabel,
                )
                CollectedBadge(
                    accent = entry.item.category.accentColor,
                    modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.xs),
                )
            } else {
                LockedTile(
                    onClick = { onShowHint(entry.item.id) },
                    onClickLabel = hintLabel,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            // Uncollected names are dimmed rather than hidden. Hiding them would make
            // the board a wall of question marks and take away the only thing that
            // tells a traveller what they are supposed to be looking for.
            color = if (entry.isCollected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
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
private fun LockedTile(
    onClick: () -> Unit,
    onClickLabel: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val ground = MaterialTheme.colorScheme.surfaceContainerHigh
    val stripe = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = STRIPE_ALPHA)

    Surface(
        onClick = onClick,
        // `Surface` has no click-label parameter, so the label is put on the node
        // directly — without it a screen reader announces the name and leaves what
        // the tap does to guesswork.
        modifier = modifier.semantics { onClick(label = onClickLabel, action = null) },
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(ground)
                    // 45° hatching, started a full height to the left so the first
                    // stripe still crosses the top-left corner.
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
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ICON_ALPHA),
                modifier = Modifier.size(HINT_ICON),
            )
        }
    }
}

/** A tick in the category's colour, so a filled tile reads as collected at a glance. */
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

/** The tile's own marks — a badge and the magnifier, sized against a square, not a scale. */
private val BADGE = 20.dp
private val BADGE_TICK = 13.dp
private val HINT_ICON = 22.dp

private const val STRIPE_ALPHA = 0.11f
private const val STRIPE_STEP_DP = 11f
private const val STRIPE_WIDTH_DP = 4f
private const val ICON_ALPHA = 0.45f
