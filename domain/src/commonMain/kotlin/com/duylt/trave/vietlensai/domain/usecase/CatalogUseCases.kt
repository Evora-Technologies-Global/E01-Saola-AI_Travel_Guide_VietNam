package com.duylt.trave.vietlensai.domain.usecase

import com.duylt.trave.vietlensai.domain.model.CultureCollection
import com.duylt.trave.vietlensai.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow

class ObserveCollectionUseCase(
    private val repository: CatalogRepository,
) {
    operator fun invoke(): Flow<CultureCollection> = repository.observeCollection()
}
