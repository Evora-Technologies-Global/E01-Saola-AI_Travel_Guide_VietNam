package com.evora.technologies.saola.domain.repository

import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.domain.model.GeoPoint
import com.evora.technologies.saola.domain.model.LensMode
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Recognition plus the local history of everything recognised.
 *
 * Room is the single source of truth: [recognize] persists before returning, so
 * every observer downstream sees the new discovery through the same Flow that
 * serves the journal — there is no second path for "fresh" data.
 */
interface DiscoveryRepository {

    /**
     * Sends the captured photo to Gemini and stores whatever comes back.
     *
     * @param imagePath absolute path of a JPEG already written to app storage.
     * @param location best-known position, folded into the prompt to disambiguate
     *   lookalike temples; null when location is off or unavailable.
     */
    suspend fun recognize(
        imagePath: String,
        mode: LensMode,
        location: GeoPoint?,
    ): AppResult<Discovery>

    fun observeDiscoveries(): Flow<List<Discovery>>

    fun observeFavorites(): Flow<List<Discovery>>

    /**
     * Everything captured inside one province, newest first.
     *
     * The passport's province sheet is the only caller: a stamp knows how many
     * discoveries stand behind it, but not which ones, and a count nobody can open is
     * where that screen used to end.
     */
    fun observeByProvince(provinceId: String): Flow<List<Discovery>>

    fun observeDiscovery(id: String): Flow<Discovery?>

    suspend fun getDiscovery(id: String): Discovery?

    suspend fun toggleFavorite(id: String): AppResult<Unit>

    suspend fun delete(id: String): AppResult<Unit>

    suspend fun deleteAll(): AppResult<Unit>
}
