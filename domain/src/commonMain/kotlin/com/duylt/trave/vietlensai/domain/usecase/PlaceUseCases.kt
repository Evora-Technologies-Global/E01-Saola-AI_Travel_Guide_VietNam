package com.duylt.trave.vietlensai.domain.usecase

import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.model.NearbyPlace
import com.duylt.trave.vietlensai.domain.model.PlaceDetails
import com.duylt.trave.vietlensai.domain.repository.LocationRepository
import com.duylt.trave.vietlensai.domain.repository.PlaceRepository
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult

/**
 * Where the traveller is standing.
 *
 * Deliberately separate from [LoadNearbyPlacesUseCase], which it used to be folded into.
 * Combining them meant one `AppResult` for two quite different questions, and a failure
 * to answer the second threw away the answer to the first: a Places outage — or a key
 * with the wrong API enabled — left the screen with a perfectly good fix in hand and no
 * map to put it on, because the only way to report the failure was to discard everything.
 *
 * Split, the screen can draw the map the moment the fix lands and let the markers arrive
 * afterwards, which is also the better order to show them in.
 */
class GetCurrentLocationUseCase(
    private val locationRepository: LocationRepository,
) {
    suspend operator fun invoke(): AppResult<GeoPoint> {
        // Checked here rather than left to the platform: without it, both implementations
        // report a missing permission as "no fix available", which sends the traveller
        // looking for open sky instead of the toggle they actually need.
        if (!locationRepository.hasLocationPermission()) {
            return AppResult.Failure(AppError.LocationUnavailable)
        }
        return locationRepository.currentLocation()
    }
}

/**
 * The places worth walking to from [center], best first.
 *
 * Takes the centre rather than finding it, so that a caller holding a fix can search
 * again — a retry after a failed search must not spend another GPS wait.
 */
class LoadNearbyPlacesUseCase(
    private val repository: PlaceRepository,
) {
    suspend operator fun invoke(
        center: GeoPoint,
        language: AppLanguage,
        radiusMeters: Int = DEFAULT_RADIUS_METERS,
        forceRefresh: Boolean = false,
    ): AppResult<List<NearbyPlace>> = repository.nearbyPlaces(
        center = center,
        radiusMeters = radiusMeters,
        language = language,
        forceRefresh = forceRefresh,
    )

    companion object {
        /**
         * Five kilometres, as asked for.
         *
         * Wide enough that a traveller in a quiet district still gets a screenful, and
         * narrow enough that everything on it is a short ride rather than a day trip.
         */
        const val DEFAULT_RADIUS_METERS = 5_000
    }
}

/**
 * The article and photographs for one place, fetched when its sheet opens.
 *
 * Takes the whole [NearbyPlace] rather than an id: the details lookup is driven by the
 * Wikipedia title and coordinates the search already resolved, and passing an id alone
 * would mean the repository holding a second index just to find them again.
 */
class LoadPlaceDetailsUseCase(
    private val repository: PlaceRepository,
) {
    suspend operator fun invoke(
        place: NearbyPlace,
        language: AppLanguage,
    ): AppResult<PlaceDetails> = repository.placeDetails(place, language)
}
