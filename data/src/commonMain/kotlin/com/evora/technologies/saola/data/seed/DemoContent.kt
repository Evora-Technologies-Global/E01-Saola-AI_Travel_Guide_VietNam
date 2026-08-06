package com.evora.technologies.saola.data.seed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The on-disk shape of `tools/seed/demo-content.json`.
 *
 * Separate types rather than reusing the entities, for the same reason
 * [com.evora.technologies.saola.data.local.db.entity.SectionJson] is separate from the domain
 * models: this file is edited by hand and read by `tools/seed_demo.py` as well as by the app,
 * so its shape must not move every time a column is renamed. Every field carries a default so
 * that an older copy of the file still parses — a demo that loses one field should lose that
 * field, not the whole trip.
 */
@Serializable
internal data class DemoContent(
    val discoveries: List<DemoDiscovery> = emptyList(),
    val chat: DemoChat? = null,
    val tripSummaries: List<DemoTripSummary> = emptyList(),
)

@Serializable
internal data class DemoDiscovery(
    /** Also the photograph's asset name — `seed/<id>.jpg`. */
    val id: String = "",
    /** How many days back from the seeding date this was "taken"; 0 is today. */
    val day: Int = 0,
    /** Hour of that day, so a day's finds are ordered the way a real one would be. */
    val hour: Int = 12,
    val title: String = "",
    val localName: String? = null,
    val category: String = "OTHER",
    val lat: Double? = null,
    val lon: Double? = null,
    val placeHint: String? = null,
    val confidence: Float = 0.9f,
    val favorite: Boolean = false,
    val summary: String = "",
    val sections: List<DemoSection> = emptyList(),
    val funFacts: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val nearby: List<DemoNearby> = emptyList(),
    val suggestedQuestions: List<String> = emptyList(),
)

@Serializable
internal data class DemoSection(
    val title: String = "",
    val body: String = "",
)

@Serializable
internal data class DemoNearby(
    val name: String = "",
    val reason: String = "",
    val category: String = "OTHER",
    val walkingMinutes: Int? = null,
)

@Serializable
internal data class DemoChat(
    val discoveryId: String = "",
    val messages: List<DemoMessage> = emptyList(),
)

@Serializable
internal data class DemoMessage(
    /** `user` or `model`, matching `ChatRole.wireName`. */
    val role: String = "model",
    @SerialName("text") val content: String = "",
)

@Serializable
internal data class DemoTripSummary(
    val day: Int = 0,
    val headline: String = "",
    val narrative: String = "",
    val highlights: List<String> = emptyList(),
    val tomorrowIdeas: List<String> = emptyList(),
)
