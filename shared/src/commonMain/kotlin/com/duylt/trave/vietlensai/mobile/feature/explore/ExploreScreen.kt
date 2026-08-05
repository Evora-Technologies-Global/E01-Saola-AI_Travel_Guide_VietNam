package com.duylt.trave.vietlensai.mobile.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.duylt.trave.vietlensai.core.designsystem.component.AppSnackbarHost
import com.duylt.trave.vietlensai.core.designsystem.component.OverlayHeader
import com.duylt.trave.vietlensai.core.designsystem.component.OverlayHeaderStyle
import com.duylt.trave.vietlensai.core.designsystem.theme.PageSpacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.util.toUserMessage
import com.duylt.trave.vietlensai.feature.explore.ExploreHost
import com.duylt.trave.vietlensai.feature.explore.ExploreIntent
import com.duylt.trave.vietlensai.feature.explore.ExploreState
import com.duylt.trave.vietlensai.feature.explore.ExploreViewModel
import com.duylt.trave.vietlensai.feature.explore.PlaceMap
import com.duylt.trave.vietlensai.feature.explore.component.MapFailureCover
import com.duylt.trave.vietlensai.feature.explore.component.MapLoadingCover
import com.duylt.trave.vietlensai.feature.explore.component.MapNotice
import com.duylt.trave.vietlensai.feature.explore.component.MapPermissionCover
import com.duylt.trave.vietlensai.feature.explore.component.PlaceDetailBody
import com.duylt.trave.vietlensai.feature.explore.component.PlaceStrip
import com.duylt.trave.vietlensai.feature.explore.component.RecenterButton
import com.duylt.trave.vietlensai.feature.explore.component.RefreshButton
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.explore_empty_body
import com.duylt.trave.vietlensai.resources.explore_empty_title
import com.duylt.trave.vietlensai.resources.explore_nearby_count
import com.duylt.trave.vietlensai.resources.explore_search_failed_title
import com.duylt.trave.vietlensai.resources.explore_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Where to go next: a map of the traveller's own surroundings, with the places worth
 * the walk already on it.
 *
 * This is what the Explore tab was being kept empty for. The passport and the culture
 * board are both accounts of where someone has already been; this is the only screen in
 * the app that points forwards, and the reason it can exist now is that it is not
 * guessing — every marker, rating and address on it came back from the Places API, so
 * tapping one and walking there gets the traveller to a real door.
 *
 * The map is full-bleed under the status bar rather than inset like the rest of the app.
 * A map is a window, and a window with a margin around it is a picture of a window; the
 * controls and the strip that float on top carry the insets instead.
 *
 * Everything this screen *does* — the permission bridge, the effects, the memoised intent
 * dispatcher the map needs to skip — is [ExploreHost], shared with the large-window
 * arrangement. What this file owns is the phone's answer to where the pieces go: the strip
 * along the foot, the controls in the header, and the open place in a sheet over the lot,
 * because on a phone the map *is* the screen and reading about a place has to cover it.
 */
@Composable
fun ExploreRoute(
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = koinViewModel(),
) {
    ExploreHost(viewModel) { state, isDarkTheme, isPermissionBlocked, snackbarHostState, onIntent,
        onRequestPermission, onOpenArticle ->
        ExploreScreen(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreScreen(
    state: ExploreState,
    isDarkTheme: Boolean,
    isPermissionBlocked: Boolean,
    snackbarHostState: SnackbarHostState,
    onIntent: (ExploreIntent) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Composed the moment the permission is answered — not when the first fix lands —
        // and left composed through every state after it.
        //
        // Both map engines cost a few hundred milliseconds the first time a process asks
        // for one: the Maps SDK fetches and links its renderer out of Play services,
        // MapKit builds its tile pipeline. Neither offers to do that anywhere but the
        // thread that asked, which inside a composable is the main one. Composed at the
        // old point — the `else` arm of the `when` below — the bill fell due on the
        // single frame that swapped the spinner for the map, which is the one frame the
        // traveller is watching and the one visit that pays for it. That is the stutter.
        //
        // Started here it runs while the fix and the search are still in flight, behind
        // the covers below, and by the time they lift the engine is warm. It also stops
        // the map being torn down and rebuilt every time the screen crosses between
        // states — a search that failed and was retried used to pay twice more.
        if (state.hasLocationPermission) {
            MapWithPlaces(state = state, isDarkTheme = isDarkTheme, onIntent = onIntent)
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

        // Over the map, above the bottom bar, and last in the box so it draws on top of
        // the map, the strip and the covers alike.
        AppSnackbarHost(
            snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = PageSpacing.snackbarLift),
        )
    }

    val selected = state.selectedPlace
    if (selected != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { onIntent(ExploreIntent.DismissSelection) },
            sheetState = sheetState,
        ) {
            PlaceDetailBody(
                place = selected,
                details = state.details,
                isLoadingDetails = state.isLoadingDetails,
                onStartNavigation = { onIntent(ExploreIntent.StartNavigation(selected.id)) },
                onOpenArticle = onOpenArticle,
            )
        }
    }
}

@Composable
private fun MapWithPlaces(
    state: ExploreState,
    isDarkTheme: Boolean,
    onIntent: (ExploreIntent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Every argument here is narrower than `state`, deliberately: `PlaceMap` is the
        // most expensive composable in the app to re-run, and it must not re-run because
        // an article arrived in a sheet drawn over it. With the intent dispatcher held
        // steady by the host, the two lambdas below are memoised and this whole call
        // skips on every emission that did not move a marker.
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

        // Title and controls in one header, so the notch inset is applied once. They used
        // to be two siblings that each called `screenInsetsPadding()` — which worked only
        // because both happened to want the same edge, and would have drifted the moment
        // one of them moved. The large window takes the controls back out of the header
        // and stacks them under the card, which is arrangement: the same two buttons, in
        // the room a phone does not have.
        OverlayHeader(
            title = stringResource(Res.string.explore_title),
            subtitle = stringResource(Res.string.explore_nearby_count, state.places.size),
            busy = state.isRefreshing || state.isLoading,
            // Not a scrim: a map is pale and already full of its own labels, and a black
            // gradient laid over it reads as a bruise.
            style = OverlayHeaderStyle.Card,
            modifier = Modifier.align(Alignment.TopCenter),
            trailing = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    RecenterButton(onIntent)
                    RefreshButton(onIntent)
                }
            },
        )

        // All three of these sit *over* the map rather than instead of it. The traveller
        // is still somewhere, and being shown where they are while being told the search
        // failed is more honest — and more useful — than replacing the whole screen.
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

            else -> PlaceStrip(
                places = state.places,
                selectedPlaceId = state.selectedPlaceId,
                onSelect = { id -> onIntent(ExploreIntent.SelectPlace(id)) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
