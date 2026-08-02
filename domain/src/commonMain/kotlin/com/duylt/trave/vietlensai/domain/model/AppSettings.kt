package com.duylt.trave.vietlensai.domain.model

/**
 * Everything the traveller can tune, persisted in DataStore.
 *
 * [apiKey] is nullable rather than blank-by-default so "never configured" is
 * distinguishable from "deliberately cleared".
 */
data class AppSettings(
    val apiKey: String?,
    val language: AppLanguage,
    val preferredModel: GeminiModel,
    val speakAnswers: Boolean,
    /**
     * Whether the app has already put the location request in front of this
     * traveller.
     *
     * There is no in-app switch for location, deliberately. A preference beside the
     * OS permission gives two off switches for one thing, and the app-level one can
     * only ever be wrong: it read "on" while the permission had never been asked
     * for, so every capture was silently saved without coordinates and the passport
     * stayed empty. The system permission is the only switch now, and this flag just
     * stops the app from asking for it twice.
     */
    val hasAskedLocation: Boolean,
    val darkTheme: ThemePreference,
) {
    val hasApiKey: Boolean get() = !apiKey.isNullOrBlank()

    companion object {
        val DEFAULT = AppSettings(
            apiKey = null,
            language = AppLanguage.VIETNAMESE,
            preferredModel = GeminiModel.DEFAULT,
            speakAnswers = true,
            hasAskedLocation = false,
            darkTheme = ThemePreference.SYSTEM,
        )
    }
}

/**
 * The narration language. Also drives the TTS voice and the "translate into"
 * target, so a single switch changes the whole experience.
 */
enum class AppLanguage(
    val code: String,
    val displayName: String,
    val bcp47: String,
) {
    VIETNAMESE(code = "vi", displayName = "Tiếng Việt", bcp47 = "vi-VN"),
    ENGLISH(code = "en", displayName = "English", bcp47 = "en-US"),
    ;

    /** What a menu should be translated *into* is the other language, not this one. */
    val opposite: AppLanguage get() = if (this == VIETNAMESE) ENGLISH else VIETNAMESE

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: VIETNAMESE
    }
}

enum class ThemePreference {
    SYSTEM, LIGHT, DARK,
}

/**
 * The Gemini models this app is allowed to call.
 *
 * Google retires older Gemini generations for keys created after a cutoff, so the
 * list is deliberately restricted to the 3.x family. Each entry knows its own
 * fallback chain: when the preferred model answers 503 (which Flash regularly does
 * under load) the data layer walks down [fallbackChain] instead of failing the shot.
 */
enum class GeminiModel(
    val id: String,
    val displayName: String,
    val description: String,
) {
    FLASH_3_5(
        id = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash",
        description = "Best balance of speed and depth. Recommended.",
    ),
    FLASH_LITE_3_1(
        id = "gemini-3.1-flash-lite",
        displayName = "Gemini 3.1 Flash Lite",
        description = "Fastest and cheapest. Great for menus and signs.",
    ),
    PRO_3(
        id = "gemini-3-pro-preview",
        displayName = "Gemini 3 Pro",
        description = "Deepest cultural and historical detail. Slower.",
    ),
    ;

    /** Preferred model first, then progressively cheaper/less loaded alternatives. */
    val fallbackChain: List<String>
        get() = buildList {
            add(id)
            entries.forEach { if (it.id != id) add(it.id) }
        }

    companion object {
        val DEFAULT = FLASH_3_5

        fun fromId(id: String?): GeminiModel =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}
