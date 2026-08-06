package com.evora.technologies.saola.domain.model

/**
 * Everything the traveller can tune, persisted in DataStore.
 *
 * Neither half of "intelligence" is here any more. The Gemini key is the one baked in at
 * build time — see `DataBuildConfig.GEMINI_API_KEY` — and the model is [GeminiModel.CONFIGURED],
 * a constant in this file. Both used to be stored preferences with a settings card each; they
 * are build decisions now, so a value nothing can write is not carried around as if it could be.
 */
data class AppSettings(
    val language: AppLanguage,
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
    companion object {
        val DEFAULT = AppSettings(
            language = AppLanguage.VIETNAMESE,
            speakAnswers = true,
            hasAskedLocation = false,
            darkTheme = ThemePreference.SYSTEM,
        )
    }
}

/**
 * The narration language. Also drives the TTS voice and the "translate into"
 * target, so one value decides the whole experience.
 *
 * Not chosen in the app: it is read from the phone, so the answers are in the language
 * the interface is already drawn in. `deviceLanguage()` in :data is where that happens,
 * and it is why [AppSettings.language] has no setter on `SettingsRepository`.
 */
enum class AppLanguage(
    val code: String,
    val displayName: String,
    val bcp47: String,
    /** How this language orders a written date; see `DateNames.kt`. */
    val dateStyle: DateStyle,
    /** English is the only one of the eight that reads a clock in halves. */
    val uses12HourClock: Boolean = false,
) {
    VIETNAMESE("vi", "Tiếng Việt", "vi-VN", DateStyle.DAY_FIRST_COMMA),
    ENGLISH("en", "English", "en-US", DateStyle.DAY_FIRST, uses12HourClock = true),
    JAPANESE("ja", "日本語", "ja-JP", DateStyle.YEAR_FIRST_CJK),
    KOREAN("ko", "한국어", "ko-KR", DateStyle.YEAR_FIRST_CJK),
    CHINESE("zh", "中文", "zh-CN", DateStyle.YEAR_FIRST_CJK),
    FRENCH("fr", "Français", "fr-FR", DateStyle.DAY_FIRST),
    SPANISH("es", "Español", "es-ES", DateStyle.DAY_DE_MONTH),
    THAI("th", "ไทย", "th-TH", DateStyle.DAY_FIRST),
    ;

    // Deliberately the same eight, in the same order, with the same codes and BCP-47 tags
    // as `TranslateLanguage`. They stay two enums because they answer different questions
    // — one is the language the app speaks, the other a target the traveller picks per
    // photo — but a tag that disagreed between them would give the same sentence two
    // different voices depending on which screen read it aloud.

    // No `fromCode`. It existed to read the stored preference, and it defaulted to
    // Vietnamese for anything it did not recognise — which is the wrong answer for a
    // phone in a language the app does not ship, whose interface falls back to the
    // English string table. `languageForTag` in :data replaces it and defaults that way.
}

/**
 * The three date orders the app's eight languages need.
 *
 * Not a format string: `DateTimeFormatter` is JVM-only and `NSDateFormatter` Apple-only,
 * so the ordering is applied by hand in `DateNames.kt`.
 */
enum class DateStyle {
    /** 12 March 2026 · 12 mars 2026 · 12 มีนาคม 2026 */
    DAY_FIRST,

    /** 12 tháng 3, 2026 — Vietnamese puts a comma before the year. */
    DAY_FIRST_COMMA,

    /** 2026年3月12日 · 2026년 3월 12일 — year, month, day, each with its own marker. */
    YEAR_FIRST_CJK,

    /** 12 de marzo de 2026 — Spanish joins the parts with a preposition. */
    DAY_DE_MONTH,
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
 *
 * **Which one the app calls is [CONFIGURED], and that is a build decision, not a
 * preference.** It was a picker on the settings page and a key in DataStore until
 * 06.08.2026; three names for what is one sentence of technical trivia, and the only
 * traveller who could answer it correctly was the one who already knew the catalogue.
 * Change the model by editing that one line and rebuilding.
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
        /**
         * The model this build calls. **The one line to edit to change it.**
         *
         * Every request in the app starts here: recognition, chat, translation and the day
         * summary all read this constant rather than a stored setting, so the four of them
         * cannot disagree and there is no state to migrate when it changes. The other entries
         * are still reachable — [fallbackChain] walks them when this one is overloaded.
         */
        val CONFIGURED = FLASH_3_5
    }
}
