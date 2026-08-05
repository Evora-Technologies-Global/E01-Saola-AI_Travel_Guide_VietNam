package com.evora.technologies.saola.domain.model

/**
 * What kind of thing was recognised.
 *
 * [wireName] is the exact token the Gemini response schema is constrained to, so
 * parsing stays a lookup rather than fuzzy string matching.
 */
enum class DiscoveryCategory(val wireName: String) {
    LANDMARK("LANDMARK"),
    FOOD("FOOD"),
    ARTIFACT("ARTIFACT"),
    ARCHITECTURE("ARCHITECTURE"),
    NATURE("NATURE"),
    CULTURE("CULTURE"),
    OTHER("OTHER"),
    ;

    companion object {
        fun fromWire(value: String?): DiscoveryCategory =
            entries.firstOrNull { it.wireName.equals(value?.trim(), ignoreCase = true) } ?: OTHER
    }
}

/**
 * What the traveller asked the camera to do.
 *
 * [AUTO] lets Gemini decide between a landmark and a plate of food, which is the
 * mode most people leave it on; the explicit modes bias the prompt when the shot
 * is ambiguous (a decorated dish inside a famous temple, say).
 */
enum class LensMode(val wireName: String) {
    AUTO("AUTO"),
    LANDMARK("LANDMARK"),
    FOOD("FOOD"),
    ARTIFACT("ARTIFACT"),
    TRANSLATE("TRANSLATE"),
}
