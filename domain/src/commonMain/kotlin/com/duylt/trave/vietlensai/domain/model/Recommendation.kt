package com.duylt.trave.vietlensai.domain.model

import kotlin.time.Instant

/**
 * A "where to next" suggestion, generated from the traveller's location and the
 * places they have already discovered.
 *
 * Deliberately not tied to a Places API result: Gemini reasons over the trip so
 * far, so the value is in [reason] — why *this* traveller should go there next.
 */
data class Recommendation(
    val id: String,
    val name: String,
    val category: DiscoveryCategory,
    val reason: String,
    val addressHint: String?,
    val distanceHint: String?,
    val bestTime: String?,
    val estimatedCost: String?,
    val location: GeoPoint?,
    val generatedAt: Instant,
)
