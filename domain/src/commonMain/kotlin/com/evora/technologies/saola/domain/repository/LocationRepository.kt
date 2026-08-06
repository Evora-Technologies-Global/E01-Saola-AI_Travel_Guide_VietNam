package com.evora.technologies.saola.domain.repository

import com.evora.technologies.saola.domain.model.GeoPoint
import com.evora.technologies.saola.domain.util.AppResult

/** Wraps the platform's fused location provider behind a domain-friendly type. */
interface LocationRepository {

    /** Returns null-safe failure rather than throwing when permission is missing. */
    suspend fun currentLocation(): AppResult<GeoPoint>

    fun hasLocationPermission(): Boolean
}
