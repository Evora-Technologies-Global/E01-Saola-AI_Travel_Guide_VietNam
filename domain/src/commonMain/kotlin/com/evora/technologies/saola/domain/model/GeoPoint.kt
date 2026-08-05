package com.evora.technologies.saola.domain.model

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** A plain WGS84 coordinate pair, kept free of any Android Location type. */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    /**
     * Great-circle distance to [other] in metres.
     *
     * The haversine formula on a sphere, not an ellipsoid: over the few kilometres this
     * is ever asked about, the difference from a proper geodesic is well under a metre,
     * and the screens that read it round to the nearest hundred anyway.
     *
     * Written here rather than taken from a platform API because both platforms have
     * one and they are different ones — `Location.distanceBetween` on Android,
     * `CLLocation.distanceFromLocation` on iOS — and a distance that disagreed between
     * the two would sort the same list of places into two different orders.
     */
    fun distanceMetersTo(other: GeoPoint): Double {
        val lat1 = latitude.toRadians()
        val lat2 = other.latitude.toRadians()
        val halfDeltaLat = (other.latitude - latitude).toRadians() / 2
        val halfDeltaLon = (other.longitude - longitude).toRadians() / 2

        val h = sin(halfDeltaLat) * sin(halfDeltaLat) +
            cos(lat1) * cos(lat2) * sin(halfDeltaLon) * sin(halfDeltaLon)
        // asin(sqrt(h)) rather than atan2: `h` can drift a hair above 1 for antipodal
        // points through floating-point error, and asin of anything above 1 is NaN.
        return 2 * EARTH_RADIUS_METERS * asin(min(1.0, sqrt(h)))
    }

    private fun Double.toRadians(): Double = this * PI / 180.0

    companion object {
        /** Hoàn Kiếm Lake — the fallback anchor when no fix is available. */
        val HANOI_CENTER = GeoPoint(latitude = 21.0287, longitude = 105.8524)

        /** Mean Earth radius, the value the haversine formula is normally quoted with. */
        private const val EARTH_RADIUS_METERS = 6_371_008.8
    }
}
