package com.evora.technologies.saola.domain.repository

import com.evora.technologies.saola.domain.model.CultureCollection
import kotlinx.coroutines.flow.Flow

/**
 * The culture collection: what there is to find, crossed with what has been found.
 *
 * One method, and no refresh: the catalogue ships with the app and the discoveries
 * come from Room, so the board is always current and never needs fetching. Nothing
 * here touches the network.
 */
interface CatalogRepository {

    /** Re-emits whenever a new photograph is recognised. */
    fun observeCollection(): Flow<CultureCollection>
}
