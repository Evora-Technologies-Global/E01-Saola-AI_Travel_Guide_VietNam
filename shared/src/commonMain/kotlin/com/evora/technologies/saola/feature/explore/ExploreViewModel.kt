package com.evora.technologies.saola.feature.explore

import androidx.lifecycle.viewModelScope
import com.evora.technologies.saola.core.mvi.MviViewModel
import com.evora.technologies.saola.core.util.log
import com.evora.technologies.saola.domain.model.GeoPoint
import com.evora.technologies.saola.domain.model.NearbyPlace
import com.evora.technologies.saola.domain.usecase.GetCurrentLocationUseCase
import com.evora.technologies.saola.domain.usecase.LoadNearbyPlacesUseCase
import com.evora.technologies.saola.domain.usecase.LoadPlaceDetailsUseCase
import com.evora.technologies.saola.domain.usecase.ObserveSettingsUseCase
import com.evora.technologies.saola.domain.util.AppResult
import com.evora.technologies.saola.domain.util.directionsUrl
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * The Explore tab: where the traveller is, and what is worth walking to from there.
 *
 * This is the screen the README had been holding the tab open for — the one that says
 * where to go *next* rather than giving a third account of where someone has already
 * been. It is also the answer to why the old AI recommendations were removed: every
 * place, position and description here came out of OpenStreetMap or Wikipedia, so
 * nothing on it is invented and a marker is where the place actually is.
 *
 * The first load is not started in `init`. It cannot be: a search needs a location fix,
 * a fix needs a permission, and the permission is the screen's to ask for — so the
 * screen reports the answer with [ExploreIntent.PermissionResolved] and the search
 * begins from there.
 */
class ExploreViewModel(
    private val getCurrentLocation: GetCurrentLocationUseCase,
    private val loadNearbyPlaces: LoadNearbyPlacesUseCase,
    private val loadPlaceDetails: LoadPlaceDetailsUseCase,
    observeSettings: ObserveSettingsUseCase,
) : MviViewModel<ExploreState, ExploreIntent, ExploreEffect>(ExploreState()) {

    /**
     * The in-flight details request, so opening a second sheet cancels the first.
     *
     * Without this, tapping quickly along a row of markers leaves several requests
     * racing and the slowest one wins — the traveller ends up reading about a place
     * they have already moved on from.
     */
    private var detailsJob: Job? = null

    /** Monotonic, so re-focusing a place the traveller has panned away from still moves. */
    private var cameraRequests = 0L

    init {
        observeSettings()
            .onEach { settings ->
                val languageChanged = settings.language != currentState.language
                setState { copy(language = settings.language) }
                // Places come back localised, so a language switch in Settings has to
                // re-search rather than merely re-render: the article summaries and the
                // readership figures are both per-wiki.
                if (languageChanged && currentState.places.isNotEmpty()) {
                    search(force = true)
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: ExploreIntent) {
        when (intent) {
            is ExploreIntent.PermissionResolved -> onPermissionResolved(intent.isGranted)
            is ExploreIntent.Refresh -> search(force = intent.force)
            is ExploreIntent.SelectPlace -> selectPlace(intent.placeId)
            ExploreIntent.DismissSelection -> dismissSelection()
            ExploreIntent.RecenterOnUser -> recenterOnUser()
            ExploreIntent.CameraApplied -> setState { copy(camera = null) }
            is ExploreIntent.StartNavigation -> startNavigation(intent.placeId)
        }
    }

    private fun onPermissionResolved(isGranted: Boolean) {
        val wasGranted = currentState.hasLocationPermission
        setState { copy(hasLocationPermission = isGranted) }

        // Losing the permission has to take the sheet with it. Otherwise the screen
        // falls back to the "grant location" state with a modal sheet still sitting on
        // top of it, describing a place the map behind it is no longer showing.
        if (!isGranted) {
            dismissSelection()
            return
        }
        // Only on the edge into "granted", and only while there is nothing on the map:
        // this arrives again every time the screen resumes, and re-searching on each
        // return from a sheet would spend a request to redraw what is already there.
        if (!wasGranted || currentState.places.isEmpty()) search(force = false)
    }

    private fun search(force: Boolean) {
        if (!currentState.hasLocationPermission) return
        if (currentState.isLoading || currentState.isRefreshing) return

        // Keyed on whether there is a map yet, not on whether there are places. A retry
        // from the "nothing found here" card has an empty place list but a perfectly
        // good map, and reporting that as a first load would blank the map out — worse,
        // the whole-screen spinner is only rendered when there is no map, so the retry
        // would have run with no visible sign that anything was happening at all.
        val isFirstLoad = currentState.center == null
        setState { copy(isLoading = isFirstLoad, isRefreshing = !isFirstLoad, error = null) }

        // Both spinners come down as well as the error being written. An unwrapped throw
        // skips every `AppResult` arm below, so this is the only thing that lowers them —
        // and because the guard above refuses to start a search while either is up, leaving
        // one raised does not merely spin, it retires refresh and retry for the life of the
        // process. Same rule as `LensViewModel`'s analysis job and the chat's `isSending`.
        launchSafely(
            onError = { setState { copy(isLoading = false, isRefreshing = false, error = it) } },
        ) {
            // The fix first, and published on its own. The map can be drawn from this
            // alone, so a search failure a moment later costs the traveller their
            // markers rather than the whole screen.
            val center = when (val fix = getCurrentLocation()) {
                is AppResult.Failure -> {
                    log.w { "No location fix for Explore: ${fix.error}" }
                    setState { copy(isLoading = false, isRefreshing = false, error = fix.error) }
                    return@launchSafely
                }
                is AppResult.Success -> fix.data
            }

            setState {
                copy(
                    center = center,
                    // Frame the map on the fix the search is centred on. Only on a first
                    // load: a refresh should leave the traveller looking at whatever part
                    // of the map they had dragged into view.
                    camera = if (isFirstLoad) nextCamera(center, MapZoom.NEIGHBOURHOOD) else camera,
                )
            }

            when (
                val result = loadNearbyPlaces(
                    center = center,
                    language = currentState.language,
                    forceRefresh = force,
                )
            ) {
                is AppResult.Success -> onPlacesLoaded(result.data)

                is AppResult.Failure -> {
                    log.w { "Could not load nearby places: ${result.error}" }
                    setState {
                        // The places are deliberately left as they are rather than
                        // cleared: a refresh that fails should leave the last good set of
                        // markers on the map with the failure reported over them, not
                        // strip the screen back to an empty map.
                        copy(isLoading = false, isRefreshing = false, error = result.error)
                    }
                    // Over a map that already has markers the failure has nowhere to be
                    // drawn, so it is spoken instead. With nothing on the map the screen
                    // renders it as a card and this would be the same news twice.
                    if (currentState.places.isNotEmpty()) {
                        sendEffect(ExploreEffect.ShowError(result.error))
                    }
                }
            }
        }
    }

    /**
     * Publishes a new set of places, reconciling the open sheet against it.
     *
     * The reconciliation is the important half. A refresh replaces the list wholesale,
     * and if the open place is not in the new one the sheet leaves composition without
     * ever calling `onDismissRequest` — so `selectedPlaceId` would be left pointing at
     * nothing, with every marker stuck dimmed behind a sheet that is no longer there
     * and no way for the traveller to clear it. Worse, a later refresh that brought the
     * place back would reopen the sheet by itself.
     */
    private fun onPlacesLoaded(places: List<NearbyPlace>) {
        val stillShown = currentState.selectedPlaceId?.let { id -> places.any { it.id == id } } == true
        if (!stillShown && currentState.selectedPlaceId != null) detailsJob?.cancel()

        setState {
            copy(
                isLoading = false,
                isRefreshing = false,
                places = places,
                error = null,
                selectedPlaceId = selectedPlaceId.takeIf { stillShown },
                details = details.takeIf { stillShown },
                isLoadingDetails = isLoadingDetails && stillShown,
            )
        }
    }

    private fun dismissSelection() {
        detailsJob?.cancel()
        setState { copy(selectedPlaceId = null, details = null, isLoadingDetails = false) }
    }

    private fun selectPlace(placeId: String) {
        // Re-selecting what is already open does nothing. On iOS `selectAnnotation` makes
        // MapKit re-enter its own delegate, which reports the selection back as if the
        // traveller had tapped it — without this guard, opening one sheet cancels and
        // restarts its own details request.
        if (placeId == currentState.selectedPlaceId) return
        val place = currentState.places.firstOrNull { it.id == placeId } ?: return

        detailsJob?.cancel()
        setState {
            copy(
                selectedPlaceId = placeId,
                // Cleared rather than kept: leaving the previous place's article on
                // screen under a new place's name is worse than a moment of nothing.
                details = null,
                isLoadingDetails = true,
                camera = nextCamera(place.location, MapZoom.PLACE),
            )
        }

        // Guarded on the id for the same reason the two arms below are: a throw belongs to
        // the request that raised this flag, and by the time it lands the traveller may have
        // opened another place whose own spinner must be left alone.
        detailsJob = launchSafely(
            onError = {
                setState {
                    if (selectedPlaceId == placeId) copy(isLoadingDetails = false, error = it)
                    else copy(error = it)
                }
            },
        ) {
            when (val result = loadPlaceDetails(place, currentState.language)) {
                is AppResult.Success -> setState {
                    // Guarded: a slow response for a place the traveller has since
                    // closed must not reopen it or overwrite the one now showing.
                    if (selectedPlaceId == placeId) {
                        copy(details = result.data, isLoadingDetails = false)
                    } else {
                        this
                    }
                }

                is AppResult.Failure -> {
                    // Deliberately not surfaced. The sheet already has the name, the
                    // category, the distance and whatever OSM knew; failing to add an
                    // article to that is not worth an error over the top of it.
                    log.w { "Could not load details for $placeId: ${result.error}" }
                    setState {
                        if (selectedPlaceId == placeId) copy(isLoadingDetails = false) else this
                    }
                }
            }
        }
    }

    /**
     * Puts the traveller back in the middle of their own map.
     *
     * Takes a fresh fix rather than reusing the search's centre. Those are the same
     * point only for as long as nobody moves: the centre is where the search was run,
     * and someone who has walked half a kilometre since then would be flown *away* from
     * the blue dot by a control whose entire promise is to fly them to it.
     */
    private fun recenterOnUser() {
        launchSafely(onError = { setState { copy(error = it) } }) {
            val target = when (val fix = getCurrentLocation()) {
                is AppResult.Success -> fix.data
                // The last known centre is a better answer than refusing to move.
                is AppResult.Failure -> currentState.center ?: return@launchSafely
            }
            setState { copy(center = target, camera = nextCamera(target, MapZoom.NEIGHBOURHOOD)) }
        }
    }

    private fun startNavigation(placeId: String) {
        val place = currentState.places.firstOrNull { it.id == placeId } ?: return
        sendEffect(ExploreEffect.OpenUrl(directionsUrl(place)))
    }

    private fun nextCamera(target: GeoPoint, zoom: MapZoom) =
        MapCamera(target = target, zoom = zoom, requestId = ++cameraRequests)
}
