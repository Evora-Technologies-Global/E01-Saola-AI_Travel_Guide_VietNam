package com.duylt.trave.vietlensai.domain.usecase

import com.duylt.trave.vietlensai.domain.repository.CaptureMaintenance

/**
 * Clears out capture files nothing points at.
 *
 * Run once at launch rather than on a timer: launch is the one moment nothing is in
 * flight, and an orphan costs a few megabytes of storage — not something worth waking
 * the app up for.
 */
class SweepOrphanCapturesUseCase(
    private val maintenance: CaptureMaintenance,
) {
    suspend operator fun invoke(): Int = maintenance.sweepOrphans()
}
