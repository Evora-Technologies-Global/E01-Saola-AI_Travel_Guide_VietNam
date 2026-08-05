package com.evora.technologies.saola.domain.model

import kotlin.time.Instant

/**
 * One thing the traveller pointed the camera at, plus everything Gemini said about it.
 *
 * A [Discovery] is the app's central record: it is what the result screen renders,
 * what the chat is grounded on, and what the journal groups by day.
 */
data class Discovery(
    val id: String,
    val title: String,
    val localName: String?,
    val category: DiscoveryCategory,
    val imagePath: String?,
    val summary: String,
    val sections: List<DiscoverySection>,
    val funFacts: List<String>,
    val tags: List<String>,
    val nearbySuggestions: List<NearbySuggestion>,
    val suggestedQuestions: List<String>,
    val confidence: Float,
    val location: GeoPoint?,
    val placeHint: String?,
    val isFavorite: Boolean,
    val modelUsed: String?,
    val createdAt: Instant,
) {
    /** Below this, the UI warns the traveller that the match is a guess. */
    val isConfident: Boolean get() = confidence >= LOW_CONFIDENCE_THRESHOLD

    companion object {
        const val LOW_CONFIDENCE_THRESHOLD = 0.55f
    }
}

/**
 * A titled block of narrative — "History", "How to eat it", "Architecture".
 *
 * Sections are model-authored rather than a fixed set of fields so a temple and a
 * bowl of phở can both be described well without one schema pretending to fit both.
 */
data class DiscoverySection(
    val title: String,
    val body: String,
)

/** A place Gemini thinks pairs well with what was just recognised. */
data class NearbySuggestion(
    val name: String,
    val reason: String,
    val category: DiscoveryCategory,
    val walkingMinutes: Int?,
)
