package com.duylt.trave.vietlensai.feature.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duylt.trave.vietlensai.core.designsystem.component.AppAsyncImage
import com.duylt.trave.vietlensai.core.designsystem.component.AppSnackbarHost
import com.duylt.trave.vietlensai.core.designsystem.component.OverlayHeader
import com.duylt.trave.vietlensai.core.designsystem.component.OverlayHeaderStyle
import com.duylt.trave.vietlensai.core.designsystem.component.EmptyState
import com.duylt.trave.vietlensai.core.designsystem.component.showError
import com.duylt.trave.vietlensai.core.designsystem.theme.PageSpacing
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.screenInsetsPadding
import com.duylt.trave.vietlensai.core.mvi.CollectEffects
import com.duylt.trave.vietlensai.core.util.rememberLocationPermissionState
import com.duylt.trave.vietlensai.core.util.toUserMessage
import com.duylt.trave.vietlensai.core.util.userMessage
import com.duylt.trave.vietlensai.domain.model.NearbyPlace
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

    // The permission state re-reads itself on resume, so this also covers the traveller
    // granting it out in system settings and coming back — the search starts by itself
    // rather than waiting for them to work out that they have to pull to refresh.
    LaunchedEffect(permission.isGranted) {
        viewModel.onIntent(ExploreIntent.PermissionResolved(permission.isGranted))
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
        onIntent = viewModel::onIntent,
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
        when {
            // Ordered by what the traveller can do about it: a missing permission is one
            // tap away, so it comes first and gets the button.
            !state.hasLocationPermission -> PermissionState(
                isBlocked = isPermissionBlocked,
                onRequestPermission = onRequestPermission,
            )

            !state.hasMap && state.isLoading -> LoadingMap()

            !state.hasMap -> FailedState(
                message = state.error?.toUserMessage(),
                onRetry = { onIntent(ExploreIntent.Refresh(force = true)) },
            )

            else -> MapWithPlaces(state = state, isDarkTheme = isDarkTheme, onIntent = onIntent)
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

/** A card floated over the map when there is something to say but the map still stands. */
@Composable
private fun MapNotice(
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
private fun PlaceStrip(
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
    val accent = markerColor(place.category)
    Surface(
        modifier = Modifier.width(240.dp).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        // Raised rather than outlined when selected: the card sits on a map whose colour
        // under it is unknown, and a border competes with whatever is behind it.
        tonalElevation = if (isSelected) 6.dp else 1.dp,
        shadowElevation = if (isSelected) 8.dp else 2.dp,
    ) {
        Row(modifier = Modifier.padding(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            PlaceThumbnail(place = place, accent = accent, modifier = Modifier.size(64.dp))
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
                            modifier = Modifier.size(13.dp),
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
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun MapControl(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
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
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun LoadingMap() {
    Column(
        modifier = Modifier.fillMaxSize().screenInsetsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(40.dp), strokeWidth = 3.dp)
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
    Column(modifier = Modifier.fillMaxSize().screenInsetsPadding()) {
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
    Column(modifier = Modifier.fillMaxSize().screenInsetsPadding()) {
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

/** Enough to read the title over a map, little enough to keep the map legible behind it. */

/** Enough tint to read as a filled tile, little enough to keep the glyph legible. */
private const val CATEGORY_TILE_ALPHA = 0.16f
