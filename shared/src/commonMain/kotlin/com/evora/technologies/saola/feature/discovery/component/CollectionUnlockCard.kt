package com.evora.technologies.saola.feature.discovery.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.component.Kicker
import com.evora.technologies.saola.core.designsystem.theme.Motion
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.util.accentColor
import com.evora.technologies.saola.core.util.label
import com.evora.technologies.saola.domain.model.CatalogItem
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.collection_section_progress
import com.evora.technologies.saola.resources.discovery_collected_kicker
import com.evora.technologies.saola.resources.discovery_collected_open
import org.jetbrains.compose.resources.stringResource

/**
 * The moment a square on the board fills, said out loud on the page where it happened.
 *
 * **The collection used to unlock in complete silence.** A recognised photograph that matches
 * one of the sixty-one entries fills its tile the instant it is saved — the whole feature — and
 * nothing anywhere told the traveller. They found out by opening the board some time later and
 * noticing a square had changed, or they never found out at all, which for someone who takes
 * four photographs and puts the phone away is the same as the feature not existing.
 *
 * **A card in state rather than a snackbar, and that is the design rather than the cheap
 * option.** A snackbar is the obvious reading of "moment", and it needs a fact this app has no
 * honest way to hold: whether *this* photograph was the first to match, which a board derived
 * at read time cannot answer, and whether the notice has already been shown, which nothing
 * persists. What the page can say truthfully on every visit is *this photograph is your phở* —
 * and saying it as a card means it survives a rotation, is still there when they come back, and
 * doubles as the way to the board. The moment is carried by the entrance instead: it expands
 * and fades in under `Motion.enter`, so on the frame after recognition it visibly arrives.
 *
 * @param item null when this photograph is not the collection's picture of anything, which is
 *   most of them. The card is animated in and out on that rather than composed conditionally,
 *   so a second photograph of the same thing taken elsewhere removes it as deliberately as the
 *   first one put it there.
 */
@Composable
internal fun CollectionUnlockCard(
    item: CatalogItem?,
    collected: Int,
    total: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = item != null,
        enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
        exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit()),
        modifier = modifier,
    ) {
        // Held through the exit: `item` is null by the time the shrink runs, and reading it
        // straight would blank the card's own text on the way out.
        val shown = rememberLastItem(item)
        if (shown != null) {
            UnlockCard(
                item = shown,
                collected = collected,
                total = total,
                onOpen = onOpen,
            )
        }
    }
}

@Composable
private fun UnlockCard(
    item: CatalogItem,
    collected: Int,
    total: Int,
    onOpen: () -> Unit,
) {
    val accent = item.category.accentColor

    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        // Outlined in the category's own colour rather than filled with it, for the reason
        // `SovereigntyCard` gives: a filled panel in an accent reads as an alert, and this is
        // good news about a photograph the traveller just took.
        border = BorderStroke(HAIRLINE, accent),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Verified,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(SEAL),
            )
            Spacer(Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Kicker(
                    text = stringResource(Res.string.discovery_collected_kicker),
                    color = accent,
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = item.category.label(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.width(Spacing.md))

            Column(horizontalAlignment = Alignment.End) {
                // The count is the point of the card as much as the name: one square filled
                // means nothing on its own, and "12 / 61" is what says there are forty-nine
                // more to go and this is a thing that can be finished.
                Text(
                    text = stringResource(Res.string.collection_section_progress, collected, total),
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                )
                Spacer(Modifier.height(Spacing.xxs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.discovery_collected_open),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(CHEVRON),
                    )
                }
            }
        }
    }
}

/** The last non-null value, so a card can finish leaving with its own words still on it. */
@Composable
private fun rememberLastItem(item: CatalogItem?): CatalogItem? {
    var last by remember { mutableStateOf(item) }
    if (item != null) last = item
    return last
}

/** The card's own marks, sized against the row they sit in rather than on the spacing scale. */
private val SEAL = 28.dp
private val CHEVRON = 16.dp
private val HAIRLINE = 1.dp
