package com.duylt.trave.vietlensai.domain.model

/**
 * The languages the photo translator works between.
 *
 * Separate from [AppLanguage], which is the language the *app* speaks: a traveller
 * whose app is in English still photographs a Japanese temple sign, and picking
 * one has never meant picking the other.
 *
 * Each is named in its own script rather than in the reader's language, so the
 * picker reads identically whichever locale the phone is in.
 */
enum class TranslateLanguage(
    val code: String,
    val displayName: String,
    val flag: String,
    /** Which voice reads a translation aloud; the app's own language cannot say. */
    val bcp47: String,
) {
    VIETNAMESE(code = "vi", displayName = "Tiếng Việt", flag = "🇻🇳", bcp47 = "vi-VN"),
    ENGLISH(code = "en", displayName = "English", flag = "🇬🇧", bcp47 = "en-US"),
    JAPANESE(code = "ja", displayName = "日本語", flag = "🇯🇵", bcp47 = "ja-JP"),
    KOREAN(code = "ko", displayName = "한국어", flag = "🇰🇷", bcp47 = "ko-KR"),
    CHINESE(code = "zh", displayName = "中文", flag = "🇨🇳", bcp47 = "zh-CN"),
    FRENCH(code = "fr", displayName = "Français", flag = "🇫🇷", bcp47 = "fr-FR"),
    SPANISH(code = "es", displayName = "Español", flag = "🇪🇸", bcp47 = "es-ES"),
    THAI(code = "th", displayName = "ไทย", flag = "🇹🇭", bcp47 = "th-TH"),
    ;

    companion object {
        /** Null for a blank or unknown code — the "detect it for me" case. */
        fun fromCode(code: String?): TranslateLanguage? =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
