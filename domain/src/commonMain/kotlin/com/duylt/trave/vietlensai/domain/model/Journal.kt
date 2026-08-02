package com.duylt.trave.vietlensai.domain.model

import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/** One day of travel: everything discovered between midnight and midnight, local time. */
data class JournalDay(
    val date: LocalDate,
    val discoveries: List<Discovery>,
    val summary: TripSummary?,
) {
    val categories: Set<DiscoveryCategory> get() = discoveries.mapTo(mutableSetOf()) { it.category }
}

/**
 * Gemini's write-up of a day, generated on demand from that day's discoveries.
 *
 * Stored rather than recomputed so the journal still reads back offline, and so a
 * day's narrative does not silently change every time it is opened.
 */
data class TripSummary(
    val date: LocalDate,
    val headline: String,
    val narrative: String,
    val highlights: List<String>,
    val tomorrowIdeas: List<String>,
    val generatedAt: Instant,
)

/** Aggregate counters shown at the top of the journal. */
data class JournalStats(
    val totalDiscoveries: Int,
    val totalDays: Int,
    val favoriteCount: Int,
    val categoryBreakdown: Map<DiscoveryCategory, Int>,
)
