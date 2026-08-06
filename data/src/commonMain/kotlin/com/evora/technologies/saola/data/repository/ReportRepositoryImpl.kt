package com.evora.technologies.saola.data.repository

import com.evora.technologies.saola.data.local.db.dao.ReportDao
import com.evora.technologies.saola.data.local.db.entity.DiscoveryReportEntity
import com.evora.technologies.saola.data.mapper.toDomain
import com.evora.technologies.saola.domain.model.DiscoveryReport
import com.evora.technologies.saola.domain.model.ReportReason
import com.evora.technologies.saola.domain.repository.ReportRepository
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The objections the traveller has filed, one row per discovery.
 *
 * The simplest repository in the module, and the only interesting thing about it is what it
 * does *not* own: no files, no network, no cascade to clean up after. A report is four short
 * columns, and the discovery's own foreign key takes the row away when the record goes.
 */
internal class ReportRepositoryImpl(
    private val reportDao: ReportDao,
    private val ioDispatcher: CoroutineDispatcher,
) : ReportRepository {

    override fun observeReport(discoveryId: String): Flow<DiscoveryReport?> =
        reportDao.observeByDiscovery(discoveryId)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)
            // The result page draws its whole footer from this. A Flow that threw would take
            // the collector with it and leave the page permanently without one, so a report
            // that cannot be read is reported as "not reported yet" — which is wrong in the
            // one direction the traveller can correct, by filing it again.
            .fallbackOnFailure(null, what = "observe the report for $discoveryId")

    override suspend fun submit(
        discoveryId: String,
        reason: ReportReason,
        note: String,
    ): AppResult<DiscoveryReport> = withContext(ioDispatcher) {
        // Stamped once and both written and returned, rather than read back after the write.
        // The caller shares this exact value, and a second `Clock.System.now()` for the copy
        // that leaves the device would put a timestamp in the mail that matches nothing on
        // the phone it came from.
        val now = Clock.System.now()
        runCatchingStorage(what = "file the report for $discoveryId") {
            reportDao.upsert(
                DiscoveryReportEntity(
                    discoveryId = discoveryId,
                    reason = reason.name,
                    note = note,
                    createdAt = now.toEpochMilliseconds(),
                ),
            )
            DiscoveryReport(
                discoveryId = discoveryId,
                reason = reason,
                note = note,
                // Through the epoch and back, so the returned value is what the next read of
                // the row will produce rather than a hair more precise than it.
                createdAt = Instant.fromEpochMilliseconds(now.toEpochMilliseconds()),
            )
        }
    }
}
