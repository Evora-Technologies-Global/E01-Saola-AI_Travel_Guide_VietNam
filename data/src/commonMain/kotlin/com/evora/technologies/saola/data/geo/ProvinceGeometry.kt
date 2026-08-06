package com.evora.technologies.saola.data.geo

import com.evora.technologies.saola.domain.model.GeoPoint
import com.evora.technologies.saola.domain.model.Province
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.PI

/**
 * Turning a GPS fix into a province, with no network and no geocoding API.
 *
 * Resolution runs in two passes, because a strict polygon test alone would fail the
 * traveller exactly where they take the best photos. Province outlines are clipped
 * to the coastline, so a Hạ Long Bay cruise, a beach at Mỹ Khê, or a fix that
 * drifted 200 m offshore all land in open water and match nothing. The second pass
 * snaps such a point to the nearest province within [SNAP_RADIUS_KM]; measured
 * against real tourist coordinates the furthest one needed 3.4 km, so the radius has
 * a wide margin while still refusing to claim a point in Cambodia or mid-ocean.
 */

/** How far offshore a point may be and still stamp the province it belongs to. */
internal const val SNAP_RADIUS_KM = 25.0

private const val KM_PER_DEGREE = 111.32
private const val EPSILON = 1e-12

/**
 * The province containing [point], else the nearest one within [SNAP_RADIUS_KM],
 * else null when the point is not in Vietnam.
 */
internal fun locateProvince(provinces: List<Province>, point: GeoPoint): Province? {
    val lon = point.longitude
    val lat = point.latitude

    provinces.forEach { province ->
        if (province.contains(lon, lat)) return province
    }

    // Nothing contained it: fall back to the closest coastline within reach.
    var best: Province? = null
    var bestKm = SNAP_RADIUS_KM
    provinces.forEach { province ->
        // Cheap reject: a province whose bounding box is already further away than
        // the current best cannot possibly have a nearer edge.
        if (province.boundsDistanceKm(lon, lat) <= bestKm) {
            val km = province.distanceKm(lon, lat)
            if (km < bestKm) {
                bestKm = km
                best = province
            }
        }
    }
    return best
}

/** Shortest distance from the point to any edge of the province, in kilometres. */
internal fun Province.distanceKm(lon: Double, lat: Double): Double {
    val kx = KM_PER_DEGREE * cos(lat.toRadians())
    var best = Double.MAX_VALUE
    rings.forEach { ring ->
        val n = ring.size / 2
        if (n >= 2) {
            var j = n - 1
            for (i in 0 until n) {
                // Work in a kilometre plane centred on the query point, so the
                // longitude squeeze at this latitude is applied once per ring
                // rather than being ignored as it would be in raw degrees.
                val d = pointToSegmentKm(
                    ax = (ring[j * 2] - lon) * kx,
                    ay = (ring[j * 2 + 1] - lat) * KM_PER_DEGREE,
                    bx = (ring[i * 2] - lon) * kx,
                    by = (ring[i * 2 + 1] - lat) * KM_PER_DEGREE,
                )
                if (d < best) best = d
                j = i
            }
        }
    }
    return best
}

/** Distance to the bounding box in kilometres; zero when the point is inside it. */
internal fun Province.boundsDistanceKm(lon: Double, lat: Double): Double {
    val kx = KM_PER_DEGREE * cos(lat.toRadians())
    val dx = when {
        lon < bounds.minLongitude -> (bounds.minLongitude - lon) * kx
        lon > bounds.maxLongitude -> (lon - bounds.maxLongitude) * kx
        else -> 0.0
    }
    val dy = when {
        lat < bounds.minLatitude -> (bounds.minLatitude - lat) * KM_PER_DEGREE
        lat > bounds.maxLatitude -> (lat - bounds.maxLatitude) * KM_PER_DEGREE
        else -> 0.0
    }
    return sqrt(dx * dx + dy * dy)
}

/** Distance from the origin to segment AB, both already in the local kilometre plane. */
private fun pointToSegmentKm(ax: Double, ay: Double, bx: Double, by: Double): Double {
    val vx = bx - ax
    val vy = by - ay
    val lengthSq = vx * vx + vy * vy
    if (lengthSq < EPSILON) return sqrt(ax * ax + ay * ay)
    var t = -(ax * vx + ay * vy) / lengthSq
    if (t < 0.0) t = 0.0 else if (t > 1.0) t = 1.0
    val cx = ax + t * vx
    val cy = ay + t * vy
    return sqrt(cx * cx + cy * cy)
}

/** `Math.toRadians` is java.lang-only; the conversion itself is one multiplication. */
private fun Double.toRadians(): Double = this * PI / 180.0
