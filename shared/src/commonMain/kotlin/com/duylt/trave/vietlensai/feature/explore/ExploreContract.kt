package com.duylt.trave.vietlensai.feature.explore

import com.duylt.trave.vietlensai.core.mvi.UiEffect
import com.duylt.trave.vietlensai.core.mvi.UiIntent
import com.duylt.trave.vietlensai.core.mvi.UiState
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.model.NearbyPlace
import com.duylt.trave.vietlensai.domain.model.PlaceDetails
import com.duylt.trave.vietlensai.domain.util.AppError

/**
 * @param places every marker on the map, best first — the same order the list under the
 *   map is in, so "the top one" means one thing on this screen.
 * @param selectedPlaceId which sheet is open, held as an id rather than as the place so
 *   a refresh that arrives while the sheet is up re-renders it with the new rating
 *   instead of leaving a stale copy on screen. The same reasoning as the culture board's.
 * @param details the extra half of the open place, loaded on demand. Null while it is
 *   still arriving, which is not the same as a place having no reviews.
 * @param error the last failure, kept in state rather than fired as an effect because
 *   this screen has to be able to show it as a whole-screen state and not only as a
 *   snackbar — a map with no places on it and no explanation is just a broken map.
 */
data class ExploreState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val center: GeoPoint? = null,
    val places: List<NearbyPlace> = emptyList(),
    val selectedPlaceId: String? = null,
    val details: PlaceDetails? = null,
    val isLoadingDetails: Boolean = false,
    val camera: MapCamera? = null,
    val error: AppError? = null,
    val language: AppLanguage = AppLanguage.VIETNAMESE,
) : UiState {

    val selectedPlace: NearbyPlace?
        get() = selectedPlaceId?.let { id -> places.firstOrNull { it.id == id } }

    /**
     * Whether the map itself is worth drawing yet.
     *
     * A map centred on nothing is worse than an empty state: it opens on the middle of
     * the ocean at zoom zero, which reads as a bug rather than as "we do not know where
     * you are yet".
     */
    val hasMap: Boolean get() = center != null

    /** Nothing came back, and not because anything failed. */
    val isEmpty: Boolean
        get() = !isLoading && error == null && hasMap && places.isEmpty()
}

sealed interface ExploreIntent : UiIntent {
    /**
     * Sent once the screen knows whether it may read the location.
     *
     * Permission state lives in the composable — it needs an Activity result launcher on
     * Android and a `CLLocationManager` on iOS, neither of which belongs in a view model —
     * so the screen tells the view model rather than the other way round.
     */
    data class PermissionResolved(val isGranted: Boolean) : ExploreIntent

    data class Refresh(val force: Boolean = false) : ExploreIntent
    data class SelectPlace(val placeId: String) : ExploreIntent
    data object DismissSelection : ExploreIntent

    /** The recentre control: put the traveller back in the middle of their own map. */
    data object RecenterOnUser : ExploreIntent

    /**
     * The map has finished moving, so the request may be retired.
     *
     * Without this a camera move is a value that sits in state for ever rather than an
     * instruction that gets carried out. The view model outlives the composable — the
     * navigation entry is retained across tab switches — so the map would re-apply the
     * last move every time it was recomposed, snapping the traveller back to wherever
     * they were an hour ago each time they returned to the tab.
     */
    data object CameraApplied : ExploreIntent

    /** "Bắt đầu" — hand this place to Google Maps with the destination filled in. */
    data class StartNavigation(val placeId: String) : ExploreIntent
}

sealed interface ExploreEffect : UiEffect {
    /** Leaves the app for Google Maps. The screen owns the opener; the view model does not. */
    data class OpenUrl(val url: String) : ExploreEffect

    /**
     * A failure that has nowhere on the screen to be drawn.
     *
     * Only for a refresh that fails over a map already carrying markers: replacing those
     * with an error card would throw away good results to report a bad request. With an
     * empty map the screen renders [ExploreState.error] as a card instead, and this
     * would be the same news told twice.
     */
    data class ShowError(val error: AppError) : ExploreEffect
}
