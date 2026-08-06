package com.evora.technologies.saola.data.repository

import com.evora.technologies.saola.data.catalog.buildCollection
import com.evora.technologies.saola.data.local.asset.CatalogAssetSource
import com.evora.technologies.saola.domain.model.CultureCollection
import com.evora.technologies.saola.domain.repository.CatalogRepository
import com.evora.technologies.saola.domain.repository.DiscoveryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * The collection board, assembled on every read.
 *
 * Takes [DiscoveryRepository] rather than the DAO because what it needs is exactly
 * what that interface already promises — domain `Discovery` objects, newest first —
 * and reaching for the DAO would mean duplicating the entity mapping to get there.
 *
 * There is no cache and no stored progress. The catalogue is held in memory by the
 * asset source and the discoveries arrive from Room, so a rebuild is a few thousand
 * string comparisons against data that is already in hand; storing the result would
 * buy nothing and introduce a second copy of the truth that could fall behind a
 * deleted photograph.
 */
internal class CatalogRepositoryImpl(
    private val assetSource: CatalogAssetSource,
    private val discoveryRepository: DiscoveryRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : CatalogRepository {

    /**
     * The guard is here rather than only upstream because the board is rebuilt on every
     * emission and this is the one place that failure could come from.
     *
     * [DiscoveryRepository.observeDiscoveries] already degrades to an empty list, and
     * [CatalogAssetSource] already returns one for a catalogue it cannot parse — so what
     * this catches is the matching in between, which runs a few thousand string
     * comparisons over data that arrives from the wire. A board that gave up would be
     * blank until the app was killed and relaunched, because there is no refresh here to
     * try again with: the collection has no network call to retry and no button to press.
     */
    override fun observeCollection(): Flow<CultureCollection> =
        discoveryRepository.observeDiscoveries()
            .map { discoveries -> buildCollection(assetSource.items(), discoveries) }
            .flowOn(ioDispatcher)
            .fallbackOnFailure(CultureCollection.EMPTY, what = "build the collection board")
}
