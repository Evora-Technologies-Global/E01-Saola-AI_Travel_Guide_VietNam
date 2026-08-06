package com.evora.technologies.saola.data.platform

import com.evora.technologies.saola.domain.model.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The fallback direction, which is the whole of this rule and easy to get backwards.
 *
 * The app ships eight string tables and Compose Resources sends every locale it does not
 * have to `values/` — the English one. So a phone in German or Portuguese draws an English
 * interface, and narration has to follow it there. The preference this replaced defaulted
 * to Vietnamese instead, which is why a traveller could end up reading English labels
 * while the guide talked to them in Vietnamese.
 */
class DeviceLanguageTest {

    @Test
    fun vietnameseTagsResolveToVietnamese() {
        listOf("vi", "vi-VN", "VI", "vi_VN").forEach {
            assertEquals(AppLanguage.VIETNAMESE, languageForTag(it), "tag '$it'")
        }
    }

    @Test
    fun everyShippedLanguageResolvesToItself() {
        val tags = mapOf(
            "en-GB" to AppLanguage.ENGLISH,
            "vi-VN" to AppLanguage.VIETNAMESE,
            "ja" to AppLanguage.JAPANESE,
            "ko-KR" to AppLanguage.KOREAN,
            // Script and region subtags are dropped, so both Chinese scripts land on the
            // one table the app ships. Simplified is what it is written in.
            "zh-Hans-CN" to AppLanguage.CHINESE,
            "zh-Hant" to AppLanguage.CHINESE,
            "fr-FR" to AppLanguage.FRENCH,
            "es-419" to AppLanguage.SPANISH,
            "th-TH" to AppLanguage.THAI,
        )
        tags.forEach { (tag, expected) ->
            assertEquals(expected, languageForTag(tag), "tag '$tag'")
        }
    }

    @Test
    fun aLanguageTheAppDoesNotShipResolvesToEnglish() {
        // German, Portuguese, Indonesian, Hindi — all real phones a traveller arrives
        // with, none of them a string table this app carries, so all of them read the
        // English one and must be narrated to in English.
        listOf("de", "pt-BR", "id", "hi-IN", "ru").forEach {
            assertEquals(AppLanguage.ENGLISH, languageForTag(it), "tag '$it'")
        }
    }

    @Test
    fun anAbsentTagIsEnglish() {
        assertEquals(AppLanguage.ENGLISH, languageForTag(null))
        assertEquals(AppLanguage.ENGLISH, languageForTag(""))
    }

    /**
     * A three-letter ISO 639-2 code is *not* accepted, and that is the deliberate choice.
     *
     * Matching by prefix would take "vie" — but it would also take "kok" (Konkani) as
     * Korean and "the" as Thai, quietly narrating to someone in a language they never
     * asked for. Both platforms hand over two-letter codes here: Android's
     * `Locale.getDefault().language` is ISO 639-1, and iOS answers with the name of a
     * localization the bundle actually contains, which is a `values-xx` directory.
     */
    @Test
    fun aThreeLetterCodeIsNotMistakenForATwoLetterOne() {
        assertEquals(AppLanguage.ENGLISH, languageForTag("vie"))
        assertEquals(AppLanguage.ENGLISH, languageForTag("kok"))
    }

    /** The real platform call, whatever this machine is set to, must answer something. */
    @Test
    fun theDeviceAnswersOneOfTheTwo() {
        val resolved = deviceLanguage()
        assertEquals(true, resolved in AppLanguage.entries)
    }
}
