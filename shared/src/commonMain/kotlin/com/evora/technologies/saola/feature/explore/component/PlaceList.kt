package com.evora.technologies.saola.feature.explore.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.component.MapSourceNote
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.domain.model.NearbyPlace
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.map_source_osm
import org.jetbrains.compose.resources.stringResource

/**
 * The same list as the markers, in the same order, in whichever direction there is room for.
 *
 * A map alone answers "what is near me" only for whatever the traveller happens to notice;
 * the list makes the ranking legible — the best place is the first card, whether or not it is
 * the nearest pin. Tapping a card selects the marker, so the two are one control with two
 * ways in.
 *
 * **Two orientations in one file, which is one decision seen twice.** A phone has a strip of
 * width and no height to spare, so the list runs along the foot of the map; a large window has
 * a column beside it and no reason to cover the picture. What must not differ is the part
 * below — the selection sync — and holding both here is what stops one of them from being
 * fixed alone. The cards themselves are [PlaceCard], shared, sized by whichever of these two
 * is drawing.
 *
 * **Both carry the OpenStreetMap credit, and they carry it because they are the results.**
 * Every card below came out of an Overpass query, so ODbL's notice belongs with them rather
 * than pinned to a corner of the map: the tiles under it are Google's on Android and Apple's
 * on iOS, and each of those already draws its own. It is also guarded on the list having
 * something in it, which answers the two cases where there is nothing to credit: a search that
 * came back empty draws neither list at all, and a search still in flight draws the strip with
 * no cards in it.
 */
@Composable
internal fun PlaceStrip(
    places: List<NearbyPlace>,
    selectedPlaceId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Above the row rather than after the last card: a trailing item on a strip that
        // scrolls sideways is a credit the traveller reaches only by swiping to the end of
        // forty places, which is not a notice.
        //
        // Guarded on there being something to credit. The strip is drawn while the first
        // search is still in flight — `ExploreState.isEmpty` is false until it settles — and
        // in that window the only thing on screen is Google's or Apple's own cartography,
        // which carries its own attribution and none of OpenStreetMap's.
        if (places.isNotEmpty()) {
            MapSourceNote(
                text = stringResource(Res.string.map_source_osm),
                modifier = Modifier.padding(start = ScreenGutter),
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            state = rememberSelectionFollowingState(places, selectedPlaceId),
            contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            items(items = places, key = { it.id }) { place ->
                PlaceCard(
                    place = place,
                    isSelected = place.id == selectedPlaceId,
                    onClick = { onSelect(place.id) },
                    modifier = Modifier.width(CARD_WIDTH),
                )
            }
        }
    }
}

/**
 * The results as a column, for a window with a side to spare.
 *
 * This is what replaces the phone's `ModalBottomSheet` on a large window: the traveller reads
 * the ranking and the open place in the same column, so choosing one never covers the map that
 * put it there.
 */
@Composable
internal fun PlaceColumn(
    places: List<NearbyPlace>,
    selectedPlaceId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = rememberSelectionFollowingState(places, selectedPlaceId),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(items = places, key = { it.id }) { place ->
            PlaceCard(
                place = place,
                isSelected = place.id == selectedPlaceId,
                onClick = { onSelect(place.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // After the last card here, not before the first as on the strip: a column is read
        // top to bottom and the credit is a footnote to the list, not a heading over it. It
        // is reachable either way, which is the whole difference from a sideways scroll.
        // Guarded for the reason the strip's is.
        if (places.isNotEmpty()) {
            item(key = "source") {
                MapSourceNote(text = stringResource(Res.string.map_source_osm))
            }
        }
    }
}

/**
 * Keeps the list in step with the map: choosing a pin scrolls its card into view.
 *
 * Without it the selection is represented in one place and not the other — a marker drawn
 * large and in front while the card describing it sits four screens along the list.
 */
@Composable
private fun rememberSelectionFollowingState(
    places: List<NearbyPlace>,
    selectedPlaceId: String?,
): LazyListState {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedPlaceId) {
        val index = places.indexOfFirst { it.id == selectedPlaceId }
        if (index >= 0) listState.animateScrollToItem(index)
    }
    return listState
}

/**
 * A size, not a gap.
 *
 * Measured against the strip: two cards and the edge of a third have to be visible at once on
 * a 360 dp display, which is what tells the traveller the row scrolls. The column has no such
 * problem — it takes the width of the pane it is in — which is why this is the row's alone.
 */
private val CARD_WIDTH = 240.dp
