package com.evora.technologies.saola.feature.explore.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.component.AppAsyncImage
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.domain.model.NearbyPlace
import com.evora.technologies.saola.feature.explore.asDistanceLabel
import com.evora.technologies.saola.feature.explore.categoryIcon
import com.evora.technologies.saola.feature.explore.compact
import com.evora.technologies.saola.feature.explore.markerColor

/**
 * One place, as it appears in whichever list the window has room for.
 *
 * The phone lays these along the foot of the map in a scrolling row 240 dp wide; a large
 * window stacks them full-width down a column beside it. Same thumbnail, same title, same
 * distance and readership, same raised-when-selected treatment — only the [modifier] the
 * caller sizes it with differs, which is exactly the line the project's constraint draws
 * between a component and an arrangement.
 *
 * @param modifier carries the width. Sized by the container rather than here: a card that
 *   fixed its own width could not be the column, and a column of cards with 240 dp of
 *   nothing to their right is what copying it into the tablet branch would have produced.
 */
@Composable
internal fun PlaceCard(
    place: NearbyPlace,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Remembered on the category rather than resolved on every recomposition. There is
    // one of these per card and the list recomposes whenever the selection moves along
    // it; the `when` is cheap, but the point of a card that can skip is that it does.
    val accent = remember(place.category) { markerColor(place.category) }
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        // Raised rather than outlined when selected: the card sits on a map whose colour
        // under it is unknown, and a border competes with whatever is behind it.
        tonalElevation = if (isSelected) 6.dp else 1.dp,
        shadowElevation = if (isSelected) 8.dp else 2.dp,
    ) {
        Row(modifier = Modifier.padding(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            PlaceThumbnail(place = place, accent = accent, modifier = Modifier.size(THUMBNAIL_SIDE))
            Spacer(Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = place.distanceMeters.asDistanceLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                    )
                    // Readership stands where a star rating would on a Google-backed
                    // card. It is shown only when there is one — an absent article is
                    // silence, not a score of zero, and a card that said "0" about a
                    // temple would be worse than saying nothing.
                    place.monthlyReaders?.takeIf { it > 0 }?.let { readers ->
                        Spacer(Modifier.width(Spacing.sm))
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(READERS_ICON_SIDE),
                        )
                        Spacer(Modifier.width(Spacing.xxs))
                        Text(
                            text = readers.compact(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A place's photograph, or its category drawn as one when there is none.
 *
 * Most places have no photograph — see [categoryIcon] — so this is the common case
 * rather than the fallback, and it has to look deliberate.
 */
@Composable
private fun PlaceThumbnail(place: NearbyPlace, accent: Color, modifier: Modifier = Modifier) {
    if (place.photoUrl != null) {
        AppAsyncImage(
            model = place.photoUrl,
            contentDescription = null,
            modifier = modifier,
            shape = MaterialTheme.shapes.small,
        )
        return
    }
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(accent.copy(alpha = CATEGORY_TILE_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = categoryIcon(place.category),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(CATEGORY_GLYPH_SIDE),
        )
    }
}

/** Sizes, not gaps — so none of these belongs on the `Spacing` scale. */
private val THUMBNAIL_SIDE = 64.dp
private val READERS_ICON_SIDE = 13.dp
private val CATEGORY_GLYPH_SIDE = 26.dp

/** Enough tint to read as a filled tile, little enough to keep the glyph legible. */
private const val CATEGORY_TILE_ALPHA = 0.16f
