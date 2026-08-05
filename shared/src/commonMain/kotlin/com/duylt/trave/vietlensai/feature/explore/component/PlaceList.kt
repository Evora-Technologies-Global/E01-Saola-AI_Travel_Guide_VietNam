package com.duylt.trave.vietlensai.feature.explore.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.domain.model.NearbyPlace

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
 */
@Composable
internal fun PlaceStrip(
    places: List<NearbyPlace>,
    selectedPlaceId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
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
