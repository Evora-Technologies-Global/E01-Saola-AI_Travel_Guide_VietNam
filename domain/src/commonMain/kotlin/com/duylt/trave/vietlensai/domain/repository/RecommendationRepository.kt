package com.duylt.trave.vietlensai.domain.repository

import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.model.Recommendation
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * "Where should I go next?", answered from the trip so far.
 *
 * Results are cached because generating them costs a full model call and the
 * answer barely changes while the traveller is standing in the same street.
 */
interface RecommendationRepository {

    fun observeRecommendations(): Flow<List<Recommendation>>

    /** @param forceRefresh bypasses the cache when the traveller pulls to refresh. */
    suspend fun refresh(location: GeoPoint?, forceRefresh: Boolean): AppResult<List<Recommendation>>
}
