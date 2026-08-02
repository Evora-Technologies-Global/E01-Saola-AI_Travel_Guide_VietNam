package com.duylt.trave.vietlensai.domain.repository

import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.model.Province
import com.duylt.trave.vietlensai.domain.model.TravelPassport
import kotlinx.coroutines.flow.Flow

/**
 * The 34 provinces, and which of them the traveller has stamped.
 *
 * Everything here is offline: the boundaries ship as an asset and the stamps come
 * from Room, so the map draws in an airplane-mode cave with no network at all.
 */
interface ProvinceRepository {

    /** All 34, in a stable order. Parsed once and cached for the process lifetime. */
    suspend fun provinces(): List<Province>

    /**
     * Which province a coordinate belongs to, or null if it is not in Vietnam.
     *
     * A point just offshore still resolves: boundaries are clipped to the coastline,
     * so a photo taken on a Hạ Long Bay boat or a Mỹ Khê beach would otherwise fall
     * in the sea and stamp nothing.
     */
    suspend fun provinceAt(location: GeoPoint): Province?

    fun observePassport(): Flow<TravelPassport>

    /**
     * Fills in the province of discoveries recorded before this feature existed.
     *
     * @return how many rows were stamped.
     */
    suspend fun backfillProvinces(): Int
}
