package com.duylt.trave.vietlensai.feature.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.component.AppAsyncImage
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.domain.model.NearbyPlace
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.action_retry
import org.jetbrains.compose.resources.stringResource

/**
 * The furniture that floats on the Explore map: the strip, its cards, and the two controls.
 *
 * Split out of `ExploreScreen.kt` rather than kept at the bottom of it — the screen file
 * had grown past 550 lines, and the checklist in `docs/android-mvi-best-practices.md` §9
 * asks for under 200. What stayed behind is the route, the screen, the map layer and the
 * three whole-screen states; what moved here is everything drawn *on top of* the map,
 * which is the natural seam.
 */

/** A card floated over the map when there is something to say but the map still stands. */
@Composable
internal fun MapNotice(
    title: String,
    body: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(horizontal = Spacing.xxl),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onRetry != null) {
                Spacer(Modifier.height(Spacing.md))
                FilledTonalButton(onClick = onRetry) {
                    Text(stringResource(Res.string.action_retry))
                }
            }
        }
    }
}

/**
 * The same list as the markers, in the same order, along the bottom.
 *
 * A map alone answers "what is near me" only for whatever the traveller happens to
 * notice; the strip makes the ranking legible — the best place is the first card,
 * whether or not it is the nearest pin. Tapping a card selects the marker, so the two
 * are one control with two ways in.
 */
@Composable
internal fun PlaceStrip(
    places: List<NearbyPlace>,
    selectedPlaceId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Keeps the strip in step with the map: choosing a pin scrolls its card into view,
    // so the selection is never represented in one place and not the other.
    LaunchedEffect(selectedPlaceId) {
        val index = places.indexOfFirst { it.id == selectedPlaceId }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        items(items = places, key = { it.id }) { place ->
            PlaceCard(
                place = place,
                isSelected = place.id == selectedPlaceId,
                onClick = { onSelect(place.id) },
            )
        }
    }
}

@Composable
private fun PlaceCard(
    place: NearbyPlace,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    // Remembered on the category rather than resolved on every recomposition. There is
    // one of these per card and the strip recomposes whenever the selection moves along
    // it; the `when` is cheap, but the point of a card that can skip is that it does.
    val accent = remember(place.category) { markerColor(place.category) }
    Surface(
        modifier = Modifier.width(CARD_WIDTH).clickable(onClick = onClick),
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

@Composable
internal fun MapControl(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(CONTROL_SIDE).clip(CircleShape).clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(CONTROL_GLYPH_SIDE),
            )
        }
    }
}

/**
 * Sizes, not gaps — so none of these belongs on the `Spacing` scale.
 *
 * [CARD_WIDTH] is measured against the strip: two cards and the edge of a third have to
 * be visible at once on a 360 dp display, which is what tells the traveller the row
 * scrolls. [CONTROL_SIDE] is a hit target and is the 44 dp minimum, not a decision.
 */
private val CARD_WIDTH = 240.dp
private val THUMBNAIL_SIDE = 64.dp
private val READERS_ICON_SIDE = 13.dp
private val CATEGORY_GLYPH_SIDE = 26.dp
private val CONTROL_SIDE = 44.dp
private val CONTROL_GLYPH_SIDE = 20.dp

/** Enough tint to read as a filled tile, little enough to keep the glyph legible. */
private const val CATEGORY_TILE_ALPHA = 0.16f
