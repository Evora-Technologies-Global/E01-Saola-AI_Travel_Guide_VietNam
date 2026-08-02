package com.duylt.trave.vietlensai.domain.usecase

import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.model.Province
import com.duylt.trave.vietlensai.domain.model.TravelPassport
import com.duylt.trave.vietlensai.domain.repository.ProvinceRepository
import kotlinx.coroutines.flow.Flow

class ObserveTravelPassportUseCase(
    private val repository: ProvinceRepository,
) {
    operator fun invoke(): Flow<TravelPassport> = repository.observePassport()
}

class ResolveProvinceUseCase(
    private val repository: ProvinceRepository,
) {
    suspend operator fun invoke(location: GeoPoint): Province? = repository.provinceAt(location)
}

/**
 * Stamps discoveries that predate the passport.
 *
 * Run once when the map opens rather than at startup: it is only the map that cares,
 * and a cold start should not pay for a table scan the traveller may never look at.
 */
class BackfillProvincesUseCase(
    private val repository: ProvinceRepository,
) {
    suspend operator fun invoke(): Int = repository.backfillProvinces()
}
