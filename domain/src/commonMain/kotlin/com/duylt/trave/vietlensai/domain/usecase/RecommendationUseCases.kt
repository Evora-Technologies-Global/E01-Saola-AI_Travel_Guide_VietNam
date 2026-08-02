package com.duylt.trave.vietlensai.domain.usecase

import com.duylt.trave.vietlensai.domain.model.Recommendation
import com.duylt.trave.vietlensai.domain.repository.LocationRepository
import com.duylt.trave.vietlensai.domain.repository.RecommendationRepository
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.flow.Flow

class ObserveRecommendationsUseCase(
    private val repository: RecommendationRepository,
) {
    operator fun invoke(): Flow<List<Recommendation>> = repository.observeRecommendations()
}

/**
 * Refreshes "where to next" from the current position plus the trip so far.
 *
 * Without a location fix Gemini still has the visited places to reason from, so
 * this degrades to "more like the things you have been enjoying" instead of failing.
 */
class RefreshRecommendationsUseCase(
    private val recommendationRepository: RecommendationRepository,
    private val locationRepository: LocationRepository,
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): AppResult<List<Recommendation>> {
        val location = if (locationRepository.hasLocationPermission()) {
            locationRepository.currentLocation().getOrNull()
        } else {
            null
        }
        return recommendationRepository.refresh(location = location, forceRefresh = forceRefresh)
    }
}
