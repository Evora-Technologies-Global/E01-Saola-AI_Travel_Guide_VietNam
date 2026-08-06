package com.evora.technologies.saola.domain.usecase

import com.evora.technologies.saola.domain.model.CultureCollection
import com.evora.technologies.saola.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow

class ObserveCollectionUseCase(
    private val repository: CatalogRepository,
) {
    operator fun invoke(): Flow<CultureCollection> = repository.observeCollection()
}
