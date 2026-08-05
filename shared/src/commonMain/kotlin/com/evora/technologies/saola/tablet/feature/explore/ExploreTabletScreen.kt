package com.evora.technologies.saola.tablet.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.component.AppSnackbarHost
import com.evora.technologies.saola.core.designsystem.component.BackChip
import com.evora.technologies.saola.core.designsystem.component.OverlayHeader
import com.evora.technologies.saola.core.designsystem.component.OverlayHeaderStyle
import com.evora.technologies.saola.core.designsystem.theme.PageSpacing
import com.evora.technologies.saola.core.designsystem.theme.PaneWidth
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.screenInsetsPadding
import com.evora.technologies.saola.core.util.toUserMessage
import com.evora.technologies.saola.feature.explore.ExploreHost
import com.evora.technologies.saola.feature.explore.ExploreIntent
import com.evora.technologies.saola.feature.explore.ExploreState
import com.evora.technologies.saola.feature.explore.ExploreViewModel
import com.evora.technologies.saola.feature.explore.PlaceMap
import com.evora.technologies.saola.feature.explore.component.MapFailureCover
import com.evora.technologies.saola.feature.explore.component.MapLoadingCover
import com.evora.technologies.saola.feature.explore.component.MapNotice
import com.evora.technologies.saola.feature.explore.component.MapPermissionCover
import com.evora.technologies.saola.feature.explore.component.PlaceColumn
import com.evora.technologies.saola.feature.explore.component.PlaceDetailBody
import com.evora.technologies.saola.feature.explore.component.RecenterButton
import com.evora.technologies.saola.feature.explore.component.RefreshButton
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.explore_empty_body
import com.evora.technologies.saola.resources.explore_empty_title
import com.evora.technologies.saola.resources.explore_nearby_count
import com.evora.technologies.saola.resources.explore_search_failed_title
import com.evora.technologies.saola.resources.explore_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The map takes the whole window; the heading, the controls and the results float on it.
 *
 * **A `Box`, not a `Row`, and that is the decision this screen turns on.** The wireframe does
 * not draw Explore, so the arrangement was given directly: the *"Around you"* card with its
 * two controls stacked below it in the top-left corner of the screen, the results in a column
 * down the right-hand side — both described by where they sit *on the screen*, which is what a
 * layer floating over a full-bleed map means and not what a pane means. The difference is not
 * cosmetic: in two panes the map would be re-measured every time the results column appeared
 * or went away, and a map re-measured is a map whose camera has been pushed sideways under the
 * traveller's finger. Full-bleed, it never moves.
 *
 * The phone puts the same two controls in the header's `trailing` slot because that is the
 * only spare width a phone has. Here they come out of the header and stack under the card,
 * which is the arrangement changing and not the components: [RecenterButton] and
 * [RefreshButton] are the same two the phone draws.
 *
 * **The open place replaces the list rather than sliding over the map.** On a phone the map is
 * the screen, so reading about a place has to cover it; here the column is already on screen
 * and a sheet would cover a map the column is not covering. Selecting still moves the camera
 * to the place, and the marker does not end up behind the column: on a 1194 dp window the rail
 * takes 104 and the column and its margin 424, so the centre of the map the camera flies to
 * sits some 120 dp clear of the column's leading edge.
 */
@Composable
fun ExploreTabletRoute(
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = koinViewModel(),
) {
    ExploreHost(viewModel) { state, isDarkTheme, isPermissionBlocked, snackbarHostState, onIntent,
        onRequestPermission, onOpenArticle ->
        ExploreTabletScreen(
            state = state,
            isDarkTheme = isDarkTheme,
            isPermissionBlocked = isPermissionBlocked,
            snackbarHostState = snackbarHostState,
            onIntent = onIntent,
            onRequestPermission = onRequestPermission,
            onOpenArticle = onOpenArticle,
            modifier = modifier,
        )
    }
}

@Composable
private fun ExploreTabletScreen(
    state: ExploreState,
    isDarkTheme: Boolean,
    isPermissionBlocked: Boolean,
    snackbarHostState: SnackbarHostState,
    onIntent: (ExploreIntent) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The phone's sheet closes on back; here the same gesture puts the results back in the
    // column, and only while a place is open. Left on, it would take the back gesture off the
    // shell and make Explore a tab with no way out of it — the mistake `JournalTabletScreen`
    // documents, and the reason this is an explicit handler rather than a nested `NavHost`.
    BackHandler(enabled = state.selectedPlaceId != null) {
        onIntent(ExploreIntent.DismissSelection)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Composed the moment the permission is answered — not when the first fix lands — and
        // left composed through every state after it. One of the four warm-up pieces the map
        // is measured on; `LLM.md` §12 holds all four, and this is the one that buys the
        // engine its head start behind the covers below.
        if (state.hasLocationPermission) {
            MapWithPlaces(
                state = state,
                isDarkTheme = isDarkTheme,
                onIntent = onIntent,
                onOpenArticle = onOpenArticle,
            )
        }

        // Over the map rather than instead of it, and opaque — see `mapCover`. Ordered by
        // what the traveller can do about it: a missing permission is one tap away, so it
        // comes first and gets the button.
        when {
            !state.hasLocationPermission -> MapPermissionCover(
                isBlocked = isPermissionBlocked,
                onRequestPermission = onRequestPermission,
            )

            !state.hasMap && state.isLoading -> MapLoadingCover()

            !state.hasMap -> MapFailureCover(
                message = state.error?.toUserMessage(),
                onRetry = { onIntent(ExploreIntent.Refresh(force = true)) },
            )
        }

        // `listBottom`, not `PageSpacing.snackbarLift`: that one is the height of a
        // navigation bar plus a gutter, and this window puts its navigation on a rail.
        AppSnackbarHost(
            snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = PageSpacing.listBottom),
        )
    }
}

@Composable
private fun MapWithPlaces(
    state: ExploreState,
    isDarkTheme: Boolean,
    onIntent: (ExploreIntent) -> Unit,
    onOpenArticle: (String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Every argument here is narrower than `state`, deliberately: `PlaceMap` is the most
        // expensive composable in the app to re-run, and it must not re-run because an article
        // arrived in the column beside it. With the intent dispatcher held steady by
        // `ExploreHost`, the two lambdas below are memoised and this whole call skips on every
        // emission that did not move a marker.
        PlaceMap(
            places = state.places,
            selectedPlaceId = state.selectedPlaceId,
            userLocation = state.center,
            camera = state.camera,
            isDarkTheme = isDarkTheme,
            onPlaceSelected = { id -> onIntent(ExploreIntent.SelectPlace(id)) },
            onCameraApplied = { onIntent(ExploreIntent.CameraApplied) },
            modifier = Modifier.fillMaxSize(),
        )

        // The inset is taken once, here, and `OverlayHeader`'s own call to it resolves to
        // nothing because `windowInsetsPadding` consumes what it applies for everything below.
        // Taken on the buttons instead they would be inset a second time and stand a notch's
        // depth below the card they belong to.
        Column(
            modifier = Modifier.align(Alignment.TopStart).screenInsetsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OverlayHeader(
                title = stringResource(Res.string.explore_title),
                subtitle = stringResource(Res.string.explore_nearby_count, state.places.size),
                busy = state.isRefreshing || state.isLoading,
                // Not a scrim: a map is pale and already full of its own labels, and a black
                // gradient laid over it reads as a bruise.
                style = OverlayHeaderStyle.Card,
                // No `trailing`. The two controls are the column below rather than the end of
                // this row — which is also why the card is left to size itself to its own
                // text: unconstrained it holds "%1$d places within 5 km" on one line in all
                // eight languages, where any width measured against one of them would wrap in
                // the longest.
            )

            Column(
                // The gutter, matching the header's own horizontal padding, so the buttons
                // line up with the left edge of the card above them.
                modifier = Modifier.padding(start = ScreenGutter),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                RecenterButton(onIntent)
                RefreshButton(onIntent)
            }
        }

        // All three sit *over* the map rather than instead of it. The traveller is still
        // somewhere, and being shown where they are while being told the search failed is more
        // honest — and more useful — than replacing the whole screen. The two notices take the
        // middle of the map rather than the column, because with nothing found there is no
        // column: a card floated in an empty 392 dp panel would be a panel apologising for
        // itself.
        when {
            state.error != null && state.places.isEmpty() -> MapNotice(
                title = stringResource(Res.string.explore_search_failed_title),
                body = state.error.toUserMessage(),
                onRetry = { onIntent(ExploreIntent.Refresh(force = true)) },
                modifier = Modifier.align(Alignment.Center),
            )

            state.isEmpty -> MapNotice(
                title = stringResource(Res.string.explore_empty_title),
                body = stringResource(Res.string.explore_empty_body),
                onRetry = null,
                modifier = Modifier.align(Alignment.Center),
            )

            // Only once there is something in it, and that condition is this arrangement's
            // own. The phone's equivalent arm draws an empty `LazyRow` while the first search
            // is in flight, which costs nothing because an empty row has no height and no
            // colour; a column is a `Surface`, so the same arm left a cream panel a third of
            // the window wide standing empty for the length of an Overpass round trip. Seen on
            // a 1664 × 768 dp window, 04.08.2026. The header's own spinner is what says the
            // search is running.
            state.places.isNotEmpty() -> ResultsColumn(
                state = state,
                onIntent = onIntent,
                onOpenArticle = onOpenArticle,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

/**
 * The ranking, and the place the traveller opened out of it, in the same column.
 *
 * [PaneWidth.journalList] rather than a sixth width of its own: this is the same role the
 * journal's day column plays — a list of things standing beside a large surface — and one
 * token used in two places is still one token.
 *
 * **It swallows drag gestures, and it has to.** A scrolling surface laid over a map is the
 * second thing on this screen to need the guard `Modifier.mapCover()` carries for the first;
 * `LLM.md` §12 states the rule and this is the other half of it. Without the empty
 * `pointerInput`, a drag that the list does not consume — a horizontal one, or a vertical one
 * once the list is at its end — reaches the map underneath and pans it, so scrolling the
 * results would drag the traveller's own surroundings out from under them.
 */
@Composable
private fun ResultsColumn(
    state: ExploreState,
    onIntent: (ExploreIntent) -> Unit,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.selectedPlace

    Surface(
        modifier = modifier
            .screenInsetsPadding()
            .padding(ScreenGutter)
            .width(PaneWidth.journalList)
            .fillMaxHeight()
            .pointerInput(Unit) {},
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        if (selected == null) {
            PlaceColumn(
                places = state.places,
                selectedPlaceId = state.selectedPlaceId,
                onSelect = { id -> onIntent(ExploreIntent.SelectPlace(id)) },
            )
            return@Surface
        }

        Column {
            // `BackChip`, the app's back affordance on a page in scheme colours — not
            // `OverlayIconButton`, which is black glass for a button over a photograph and
            // would read as a hole punched in this card.
            BackChip(
                onClick = { onIntent(ExploreIntent.DismissSelection) },
                modifier = Modifier.padding(start = ScreenGutter, top = PageSpacing.headerTop),
            )
            PlaceDetailBody(
                place = selected,
                details = state.details,
                isLoadingDetails = state.isLoadingDetails,
                onStartNavigation = { onIntent(ExploreIntent.StartNavigation(selected.id)) },
                onOpenArticle = onOpenArticle,
                modifier = Modifier.weight(1f).padding(top = Spacing.md),
            )
        }
    }
}
