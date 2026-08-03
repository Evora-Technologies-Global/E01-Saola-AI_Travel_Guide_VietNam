package com.duylt.trave.vietlensai.feature.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duylt.trave.vietlensai.core.designsystem.component.AppSnackbarHost
import com.duylt.trave.vietlensai.core.designsystem.component.EmptyState
import com.duylt.trave.vietlensai.core.designsystem.component.OverlayHeader
import com.duylt.trave.vietlensai.core.designsystem.component.OverlayHeaderStyle
import com.duylt.trave.vietlensai.core.designsystem.component.showError
import com.duylt.trave.vietlensai.core.designsystem.theme.PageSpacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.screenInsetsPadding
import com.duylt.trave.vietlensai.core.mvi.CollectEffects
import com.duylt.trave.vietlensai.core.util.rememberLocationPermissionState
import com.duylt.trave.vietlensai.core.util.toUserMessage
import com.duylt.trave.vietlensai.core.util.userMessage
import com.duylt.trave.vietlensai.platform.rememberUrlOpener
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.action_retry
import com.duylt.trave.vietlensai.resources.explore_empty_body
import com.duylt.trave.vietlensai.resources.explore_empty_title
import com.duylt.trave.vietlensai.resources.explore_loading
import com.duylt.trave.vietlensai.resources.explore_nearby_count
import com.duylt.trave.vietlensai.resources.explore_permission_blocked_body
import com.duylt.trave.vietlensai.resources.explore_permission_body
import com.duylt.trave.vietlensai.resources.explore_permission_title
import com.duylt.trave.vietlensai.resources.explore_recenter
import com.duylt.trave.vietlensai.resources.explore_refresh
import com.duylt.trave.vietlensai.resources.explore_search_failed_title
import com.duylt.trave.vietlensai.resources.explore_title
import com.duylt.trave.vietlensai.resources.location_permission_grant
import com.duylt.trave.vietlensai.resources.nav_explore
import com.duylt.trave.vietlensai.resources.permission_open_settings
import kotlinx.coroutines.launch
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
 */
@Composable
fun ExploreRoute(
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permission = rememberLocationPermissionState()
    val openUrl = rememberUrlOpener()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Remembered, where the other nine routes write `viewModel::onIntent` at the call
    // site. A bound callable reference captures its receiver, and the compose report
    // marks every ViewModel in this project `unstable` — so the compiler cannot memoise
    // it and the expression is a **new object on every recomposition**. Every child
    // taking it, and every lambda closing over it, is then unequal to its predecessor
    // and is denied the skip its `skippable` mark promises.
    //
    // On the other nine screens that costs a few wasted layout passes. Here the subtree
    // is a map: re-running `PlaceMap` re-enters the Maps SDK once per marker on Android
    // and re-runs the whole `UIKitView` update — the annotation diff, the selection
    // sweep — on iOS, and it did that on every emission, including the ones that only
    // moved a spinner.
    val onIntent = remember(viewModel) { viewModel::onIntent }

    // The permission state re-reads itself on resume, so this also covers the traveller
    // granting it out in system settings and coming back — the search starts by itself
    // rather than waiting for them to work out that they have to pull to refresh.
    LaunchedEffect(permission.isGranted) {
        onIntent(ExploreIntent.PermissionResolved(permission.isGranted))
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is ExploreEffect.OpenUrl -> openUrl(effect.url)
            // From the effect's own payload. This effect is only raised when the map already
            // has markers, so there is no card on screen to read the failure off — which
            // makes it the one path where losing the message loses it entirely, and reading
            // `state.error` here lost it on every first failure. See `AppError.userMessage`.
            is ExploreEffect.ShowError -> scope.launch {
                snackbarHostState.showError(effect.error.userMessage())
            }
        }
    }

    ExploreScreen(
        state = state,
        isDarkTheme = MaterialTheme.colorScheme.surface.isDark(),
        isPermissionBlocked = permission.isDeniedForGood,
        onIntent = onIntent,
        onRequestPermission = permission::requestOrOpenSettings,
        onOpenArticle = openUrl,
        modifier = modifier,
    )

    // Over the map, above the bottom bar. The host is placed last so it draws on top of
    // the map and the strip alike.
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AppSnackbarHost(
            snackbarHostState,
            modifier = Modifier.padding(bottom = PageSpacing.snackbarLift),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreScreen(
    state: ExploreState,
    isDarkTheme: Boolean,
    isPermissionBlocked: Boolean,
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

        // Over the map rather than instead of it, and opaque — see [mapCover]. Ordered by
        // what the traveller can do about it: a missing permission is one tap away, so it
        // comes first and gets the button.
        when {
            !state.hasLocationPermission -> PermissionState(
                isBlocked = isPermissionBlocked,
                onRequestPermission = onRequestPermission,
            )

            !state.hasMap && state.isLoading -> LoadingMap()

            !state.hasMap -> FailedState(
                message = state.error?.toUserMessage(),
                onRetry = { onIntent(ExploreIntent.Refresh(force = true)) },
            )
        }
    }

    val selected = state.selectedPlace
    if (selected != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { onIntent(ExploreIntent.DismissSelection) },
            sheetState = sheetState,
        ) {
            PlaceDetailSheetContent(
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
        // steady by the route, the two lambdas below are memoised and this whole call
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
        // one of them moved.
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
                    MapControl(
                        icon = Icons.Filled.MyLocation,
                        contentDescription = stringResource(Res.string.explore_recenter),
                        onClick = { onIntent(ExploreIntent.RecenterOnUser) },
                    )
                    MapControl(
                        icon = Icons.Filled.Refresh,
                        contentDescription = stringResource(Res.string.explore_refresh),
                        onClick = { onIntent(ExploreIntent.Refresh(force = true)) },
                    )
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

/**
 * The ground a whole-screen state stands on, over a map that is already running.
 *
 * Two jobs, and the screen is wrong without either. **Opaque**, because the map is now
 * composed *underneath* these rather than instead of them, and a transparent state would
 * show a map of Hanoi warming up behind the sentence explaining that we do not know where
 * the traveller is. And **it swallows touches**: an empty `pointerInput` is hit-tested
 * like any other pointer node, so the drag that would otherwise reach straight through to
 * the map — panning a map nobody can see, under a spinner — stops here instead.
 */
@Composable
private fun Modifier.mapCover(): Modifier = this
    .fillMaxSize()
    .background(MaterialTheme.colorScheme.background)
    .pointerInput(Unit) {}
    .screenInsetsPadding()

@Composable
private fun LoadingMap() {
    Column(
        modifier = Modifier.mapCover(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(SPINNER_SIDE), strokeWidth = 3.dp)
        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = stringResource(Res.string.explore_loading),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/**
 * No location yet, and the two quite different reasons for that.
 *
 * Past a permanent denial the OS will not show its dialog again, so both the
 * explanation and the button have to change: a button still labelled "Allow" would do
 * nothing visible when tapped. The same rule `PermissionSheet` follows, applied to a
 * whole-screen state — this screen has nothing at all to show without a fix, so a sheet
 * over an empty map would be a prompt floating on nothing.
 */
@Composable
private fun PermissionState(isBlocked: Boolean, onRequestPermission: () -> Unit) {
    Column(modifier = Modifier.mapCover()) {
        EmptyState(
            icon = Icons.Outlined.LocationOff,
            title = stringResource(Res.string.explore_permission_title),
            description = stringResource(
                if (isBlocked) {
                    Res.string.explore_permission_blocked_body
                } else {
                    Res.string.explore_permission_body
                },
            ),
            action = {
                FilledTonalButton(onClick = onRequestPermission) {
                    Text(
                        stringResource(
                            if (isBlocked) {
                                Res.string.permission_open_settings
                            } else {
                                Res.string.location_permission_grant
                            },
                        ),
                    )
                }
            },
        )
    }
}

@Composable
private fun FailedState(message: String?, onRetry: () -> Unit) {
    Column(modifier = Modifier.mapCover()) {
        EmptyState(
            icon = Icons.Outlined.Explore,
            title = stringResource(Res.string.nav_explore),
            description = message ?: stringResource(Res.string.explore_empty_body),
            action = {
                FilledTonalButton(onClick = onRetry) {
                    Text(stringResource(Res.string.action_retry))
                }
            },
        )
    }
}

/**
 * Whether the app is currently in its dark scheme.
 *
 * Read off the surface colour rather than from `isSystemInDarkTheme()`, because the
 * traveller can force light or dark in Settings regardless of the system — and the map
 * has to match the app around it, not the OS.
 */
private fun Color.isDark(): Boolean =
    (LUMINANCE_RED * red + LUMINANCE_GREEN * green + LUMINANCE_BLUE * blue) < 0.5f

private const val LUMINANCE_RED = 0.299f
private const val LUMINANCE_GREEN = 0.587f
private const val LUMINANCE_BLUE = 0.114f

/** A size, not a gap — the spinner is the one thing on an otherwise empty screen. */
private val SPINNER_SIDE = 40.dp
