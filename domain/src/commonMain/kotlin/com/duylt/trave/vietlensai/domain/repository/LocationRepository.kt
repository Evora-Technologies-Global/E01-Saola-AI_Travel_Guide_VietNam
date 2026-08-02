package com.duylt.trave.vietlensai.domain.repository

import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.util.AppResult

/** Wraps the platform's fused location provider behind a domain-friendly type. */
interface LocationRepository {

    /** Returns null-safe failure rather than throwing when permission is missing. */
    suspend fun currentLocation(): AppResult<GeoPoint>

    fun hasLocationPermission(): Boolean
}
