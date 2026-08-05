package com.evora.technologies.saola.feature.journal.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.Pill
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing

/**
 * A way of looking at the whole trip, as one row: a mark, a bar, and the count.
 *
 * These sit above the days because they are the only things on this screen that show the trip
 * as a whole — everything below them is one day at a time. There are two, the passport and the
 * collection, and they share a shape deliberately: they answer the same question along
 * different axes, where the traveller has been and what they have found, and drawing them as
 * siblings is what keeps them from reading as rivals.
 *
 * @param selected whether the row is the view currently on screen. The phone never passes it:
 *   there, a tap pushes a screen and the row it came from is gone, so there is nothing for a
 *   selected state to describe. On a large window the two rows are the switch for the pane
 *   beside them and both stay visible, so without this the traveller has no way to tell which
 *   of the two they are looking at. Not a mode bolted on for the tablet — it is the same
 *   question the rail answers about its four places, asked about these two.
 */
@Composable
internal fun ProgressRow(
    icon: ImageVector,
    title: String,
    progress: Float,
    progressLabel: String,
    openLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val animated by animateFloatAsState(targetValue = progress, label = "rowProgress")
    val outline = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary.copy(alpha = UNSELECTED_BORDER_ALPHA)
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(width = 1.dp, color = outline),
    ) {
        Row(
            modifier = Modifier.padding(
                start = Spacing.md,
                end = Spacing.sm,
                top = Spacing.md,
                bottom = Spacing.md,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(ICON_TILE)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(ICON_SIZE),
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { animated },
                        modifier = Modifier
                            .weight(1f)
                            .height(BAR_HEIGHT)
                            .clip(Pill),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = progressLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = openLabel,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(CHEVRON_SIZE),
            )
        }
    }
}

/** The square the mark sits in, and the mark inside it — a component's own geometry. */
private val ICON_TILE = 44.dp
private val ICON_SIZE = 22.dp
private val CHEVRON_SIZE = 24.dp

/** The bar is drawn at `Pill`, so its height is the whole of its shape. */
private val BAR_HEIGHT = 6.dp

private const val UNSELECTED_BORDER_ALPHA = 0.45f
