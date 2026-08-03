package com.duylt.trave.vietlensai.data.repository

import com.duylt.trave.vietlensai.data.geo.locateProvince
import com.duylt.trave.vietlensai.data.local.asset.ProvinceAssetSource
import com.duylt.trave.vietlensai.data.local.db.dao.DiscoveryDao
import com.duylt.trave.vietlensai.data.util.log
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.model.PassportStamp
import com.duylt.trave.vietlensai.domain.model.Province
import com.duylt.trave.vietlensai.domain.model.TravelPassport
import com.duylt.trave.vietlensai.domain.repository.CaptureStore
import com.duylt.trave.vietlensai.domain.repository.ProvinceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Instant

/**
 * The passport: 34 fixed outlines joined to whatever Room knows about each one.
 *
 * The stamp roll-up is computed by SQLite in one grouped query rather than by
 * loading every discovery and counting in Kotlin — the map only ever needs a count,
 * two timestamps and a cover path per province, and a traveller with a few hundred
 * captures should not deserialise all of them to draw 34 shapes.
 */
internal class ProvinceRepositoryImpl(
    private val assetSource: ProvinceAssetSource,
    private val discoveryDao: DiscoveryDao,
    private val captureStore: CaptureStore,
    private val ioDispatcher: CoroutineDispatcher,
) : ProvinceRepository {

    override suspend fun provinces(): List<Province> = assetSource.provinces()

    override suspend fun provinceAt(location: GeoPoint): Province? = withContext(ioDispatcher) {
        locateProvince(assetSource.provinces(), location)
    }

    override fun observePassport(): Flow<TravelPassport> =
        discoveryDao.observeProvinceStamps()
            // Caught here rather than at the end of the chain, and that placement is the
            // whole point: the 34 outlines come from the shipped asset, not from Room, so
            // an unreadable roll-up costs the counts and still leaves every province on
            // screen waiting to be stamped. Catching after the map would have emitted an
            // empty passport instead — a blank map, which is exactly what the comment
            // below says must never happen.
            .fallbackOnFailure(emptyList(), what = "read the passport stamps")
            .map { rows ->
                val byProvince = rows.associateBy { it.provinceId }
                // Every province gets a stamp, visited or not: an empty outline is
                // an invitation, and the map would be meaningless without them.
                TravelPassport(
                    stamps = assetSource.provinces().map { province ->
                        val row = byProvince[province.id]
                        PassportStamp(
                            province = province,
                            discoveryCount = row?.discoveryCount ?: 0,
                            coverImagePath = row?.coverImageName?.let(captureStore::resolve),
                            firstVisitAt = row?.firstVisitAt?.let(Instant::fromEpochMilliseconds),
                            lastVisitAt = row?.lastVisitAt?.let(Instant::fromEpochMilliseconds),
                        )
                    },
                )
            }
            .flowOn(ioDispatcher)

    /**
     * Stamps discoveries recorded before the passport existed.
     *
     * Cheap and idempotent: the query only returns rows that have coordinates and no
     * province yet, so once everything is stamped this walks an empty result set.
     */
    override suspend fun backfillProvinces(): Int = withContext(ioDispatcher) {
        // Zero on a failed read, which the caller already handles: this runs unprompted
        // when the passport opens, and nobody is waiting on it. Reporting "stamped
        // nothing" is the truth in both cases.
        val unstamped = runCatchingStorageOr(
            what = "read the unstamped discoveries",
            fallback = emptyList(),
        ) {
            discoveryDao.getUnstamped()
        }
        if (unstamped.isEmpty()) return@withContext 0

        val provinces = assetSource.provinces()
        if (provinces.isEmpty()) return@withContext 0

        var stamped = 0
        unstamped.forEach { row ->
            val province = locateProvince(
                provinces,
                GeoPoint(latitude = row.latitude, longitude = row.longitude),
            )
            if (province != null) {
                // Per row rather than around the loop: one row that will not take a
                // province must not cost the other three hundred their stamps. Written
                // out rather than as `runCatching`, which would also absorb the
                // cancellation raised when the traveller leaves the passport mid-backfill.
                try {
                    discoveryDao.setProvinceId(row.id, province.id)
                    stamped++
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.e(e) { "Could not stamp discovery ${row.id}" }
                }
            }
        }
        if (stamped > 0) log.i { "Backfilled province for $stamped discoveries" }
        stamped
    }
}
