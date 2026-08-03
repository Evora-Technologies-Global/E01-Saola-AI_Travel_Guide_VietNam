package com.duylt.trave.vietlensai.data.remote.gemini.dto

import kotlinx.serialization.Serializable

/**
 * The shapes Gemini is *constrained* to return, one per feature.
 *
 * Each of these has a matching JSON Schema in
 * [com.duylt.trave.vietlensai.data.remote.gemini.GeminiSchemas]. Because the model
 * is bound to that schema server-side, parsing is a plain deserialisation instead
 * of the usual "find the JSON inside the prose" clean-up.
 */
@Serializable
data class DiscoveryPayload(
    val recognized: Boolean = false,
    val title: String = "",
    val localName: String? = null,
    val category: String = "OTHER",
    val confidence: Float = 0f,
    val summary: String = "",
    val sections: List<SectionPayload> = emptyList(),
    val funFacts: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val nearbySuggestions: List<NearbySuggestionPayload> = emptyList(),
    val suggestedQuestions: List<String> = emptyList(),
    val placeHint: String? = null,
    /** Populated when [recognized] is false, to tell the traveller what to try instead. */
    val notRecognizedHint: String? = null,
)

@Serializable
data class SectionPayload(
    val title: String = "",
    val body: String = "",
)

@Serializable
data class NearbySuggestionPayload(
    val name: String = "",
    val reason: String = "",
    val category: String = "OTHER",
    val walkingMinutes: Int? = null,
)

@Serializable
data class LineTranslationPayload(
    val detectedLanguage: String = "",
    val contextNote: String? = null,
    val lines: List<TranslatedLinePayload> = emptyList(),
)

@Serializable
data class TranslatedLinePayload(
    /** Which recognised line this belongs to; the tie back to its place in the photo. */
    val index: Int = -1,
    val original: String = "",
    val translated: String = "",
    val note: String? = null,
    val price: String? = null,
)

@Serializable
data class TripSummaryPayload(
    val headline: String = "",
    val narrative: String = "",
    val highlights: List<String> = emptyList(),
    val tomorrowIdeas: List<String> = emptyList(),
)
