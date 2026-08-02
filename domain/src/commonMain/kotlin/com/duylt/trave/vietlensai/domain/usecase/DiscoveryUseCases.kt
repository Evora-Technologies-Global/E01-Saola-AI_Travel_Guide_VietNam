package com.duylt.trave.vietlensai.domain.usecase

import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.model.LensMode
import com.duylt.trave.vietlensai.domain.repository.DiscoveryRepository
import com.duylt.trave.vietlensai.domain.repository.LocationRepository
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Turns a captured photo into a stored [Discovery].
 *
 * Owns the decision of whether to attach a location: the repository should not
 * have to know how the permission was obtained, and the ViewModel should not have
 * to remember to ask. A failed location fix is deliberately non-fatal —
 * recognising Hạ Long Bay without coordinates is still worth doing.
 *
 * The granted permission is the whole test. There used to be an app-level
 * "use my location" preference in front of it, which turned out to be the reason
 * the passport never filled in: it defaulted to on while the permission had never
 * been requested at all, so every capture was written with null coordinates and
 * nothing downstream could tell that anything was missing.
 */
class RecognizeImageUseCase(
    private val discoveryRepository: DiscoveryRepository,
    private val locationRepository: LocationRepository,
) {
    suspend operator fun invoke(imagePath: String, mode: LensMode): AppResult<Discovery> {
        val location = if (locationRepository.hasLocationPermission()) {
            locationRepository.currentLocation().getOrNull()
        } else {
            null
        }
        return discoveryRepository.recognize(imagePath = imagePath, mode = mode, location = location)
    }
}

class ObserveDiscoveriesUseCase(
    private val repository: DiscoveryRepository,
) {
    operator fun invoke(): Flow<List<Discovery>> = repository.observeDiscoveries()
}

class ObserveFavoritesUseCase(
    private val repository: DiscoveryRepository,
) {
    operator fun invoke(): Flow<List<Discovery>> = repository.observeFavorites()
}

class ObserveDiscoveryUseCase(
    private val repository: DiscoveryRepository,
) {
    operator fun invoke(id: String): Flow<Discovery?> = repository.observeDiscovery(id)
}

/** What the passport's province sheet lists under a stamp. Newest first. */
class ObserveProvinceDiscoveriesUseCase(
    private val repository: DiscoveryRepository,
) {
    operator fun invoke(provinceId: String): Flow<List<Discovery>> =
        repository.observeByProvince(provinceId)
}

class ToggleFavoriteUseCase(
    private val repository: DiscoveryRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.toggleFavorite(id)
}

class DeleteDiscoveryUseCase(
    private val repository: DiscoveryRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.delete(id)
}
