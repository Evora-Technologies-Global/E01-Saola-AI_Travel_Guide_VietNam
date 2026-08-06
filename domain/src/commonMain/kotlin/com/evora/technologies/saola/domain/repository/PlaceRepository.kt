package com.evora.technologies.saola.domain.repository

import com.evora.technologies.saola.domain.model.AppLanguage
import com.evora.technologies.saola.domain.model.GeoPoint
import com.evora.technologies.saola.domain.model.NearbyPlace
import com.evora.technologies.saola.domain.model.PlaceDetails
import com.evora.technologies.saola.domain.util.AppResult

/**
 * What the open map of the world knows about the ground the traveller is standing on.
 *
 * Read-only and not observable, unlike every other repository here. There is no Room
 * table behind it and no Flow: this is the one part of the app whose whole value is
 * that it is *current*, and a place list restored from disk on a cold launch would be
 * about wherever the phone was the last time it was open. Freshness is handled inside
 * the implementation with a short-lived cache instead, so moving down the street does
 * not cost another search but crossing town does.
 *
 * No API key appears anywhere in this contract, and that is the point of the sources
 * behind it: OpenStreetMap, Wikipedia and Wikimedia Commons are all callable without
 * one, so the feature cannot be switched off by a billing account.
 */
interface PlaceRepository {

    /**
     * The most worthwhile places inside [radiusMeters] of [center], best first.
     *
     * Ordering is the repository's own — see [NearbyPlace.prominence] — because none of
     * the sources ranks anything. OpenStreetMap has no notion of importance and
     * Wikipedia's geosearch is ordered purely by distance.
     *
     * @param forceRefresh skips the cache, for pull-to-refresh.
     */
    suspend fun nearbyPlaces(
        center: GeoPoint,
        radiusMeters: Int,
        language: AppLanguage,
        forceRefresh: Boolean = false,
    ): AppResult<List<NearbyPlace>>

    /** The article and the photographs for one place, fetched when its sheet opens. */
    suspend fun placeDetails(place: NearbyPlace, language: AppLanguage): AppResult<PlaceDetails>
}
