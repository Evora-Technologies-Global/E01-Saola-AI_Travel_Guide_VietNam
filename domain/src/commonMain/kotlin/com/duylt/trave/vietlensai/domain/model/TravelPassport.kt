package com.duylt.trave.vietlensai.domain.model

import kotlin.time.Instant

/**
 * One province on the map, and what the traveller has collected there.
 *
 * An unvisited province is still a stamp — the map draws all 34 whether they have
 * been earned or not, and the empty ones are the whole point of a passport.
 */
data class PassportStamp(
    val province: Province,
    val discoveryCount: Int,
    /** Photo shown inside the province outline: the favourite if there is one, else the newest. */
    val coverImagePath: String?,
    val firstVisitAt: Instant?,
    val lastVisitAt: Instant?,
) {
    val isUnlocked: Boolean get() = discoveryCount > 0
}

/**
 * The travel map: every province, with the ones visited filled in.
 *
 * Always holds all 34 stamps in a stable order so the renderer can build its paths
 * once and never reorder them between emissions.
 */
data class TravelPassport(
    val stamps: List<PassportStamp>,
) {
    val unlockedCount: Int get() = stamps.count { it.isUnlocked }

    val totalCount: Int get() = stamps.size

    val discoveryCount: Int get() = stamps.sumOf { it.discoveryCount }

    /** 0f..1f, for the progress bar. */
    val progress: Float
        get() = if (stamps.isEmpty()) 0f else unlockedCount.toFloat() / stamps.size

    val mostRecent: PassportStamp?
        get() = stamps.filter { it.isUnlocked }
            .maxByOrNull { it.lastVisitAt ?: Instant.DISTANT_PAST }

    companion object {
        val EMPTY = TravelPassport(emptyList())
    }
}
