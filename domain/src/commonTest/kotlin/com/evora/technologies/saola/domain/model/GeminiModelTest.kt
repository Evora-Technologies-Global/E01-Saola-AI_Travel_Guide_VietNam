package com.evora.technologies.saola.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class GeminiModelTest {

    @Test
    fun `fallback chain starts with the chosen model and covers the rest`() {
        val chain = GeminiModel.FLASH_LITE_3_1.fallbackChain

        assertEquals(GeminiModel.FLASH_LITE_3_1.id, chain.first())
        assertEquals(GeminiModel.entries.size, chain.size)
        assertEquals(chain.distinct(), chain)
        assertTrue(chain.containsAll(GeminiModel.entries.map { it.id }))
    }

    @Test
    fun `an unknown stored model id falls back to the default`() {
        assertEquals(GeminiModel.DEFAULT, GeminiModel.fromId("gemini-2.5-flash"))
        assertEquals(GeminiModel.DEFAULT, GeminiModel.fromId(null))
    }

    @Test
    fun `every id round-trips`() {
        GeminiModel.entries.forEach { model ->
            assertEquals(model, GeminiModel.fromId(model.id))
        }
    }

    /**
     * Google retires older Gemini generations for keys created after a cutoff; a
     * 2.x id reaching the wire would 404 for every new user of this app.
     */
    @Test
    fun `no retired model generation is offered`() {
        GeminiModel.entries.forEach { model ->
            assertTrue(
                model.id.startsWith("gemini-3"),
                "Model ${model.id} is from a retired generation",
            )
        }
    }

    /**
     * The two language enums have to stay in step.
     *
     * They are separate types on purpose — one is the language the app speaks, the other
     * a target the traveller picks per photo — but the same eight languages, and the
     * same BCP-47 tag for each. A tag that drifted apart would give one sentence two
     * different voices depending on which screen read it aloud, since `TextToSpeech`
     * takes the tag from whichever enum the caller happened to hold.
     */
    @Test
    fun `AppLanguage and TranslateLanguage agree on every language`() {
        assertEquals(
            TranslateLanguage.entries.map { it.code },
            AppLanguage.entries.map { it.code },
        )
        AppLanguage.entries.forEach { app ->
            val translate = TranslateLanguage.entries.first { it.code == app.code }
            assertEquals(translate.bcp47, app.bcp47, "bcp47 differs for ${app.code}")
            assertEquals(translate.displayName, app.displayName, "name differs for ${app.code}")
        }
    }

    /**
     * Every language names a month table of its own, checked through the assembled date
     * rather than the month name alone.
     *
     * Japanese and Chinese genuinely share "1月", so the names are not all distinct — but
     * the *dates* are, because Korean spaces its parts and the others do not, and because
     * only Vietnamese puts a comma before the year. A language whose table went missing
     * would fall back to English and collide with English here.
     */
    @Test
    fun `every language writes its own dates`() {
        val date = LocalDate(2026, 3, 12)

        // Japanese and Chinese write this date identically, and that is correct rather
        // than a collision to fix — so Chinese is excluded and then pinned to Japanese.
        // Everything else must be distinct: a language whose table went missing would
        // fall back to English and show up here as a duplicate.
        val written = (AppLanguage.entries - AppLanguage.CHINESE).map { date.longLabel(it) }
        assertEquals(AppLanguage.entries.size - 1, written.distinct().size, "$written")
        assertEquals("2026年3月12日", date.longLabel(AppLanguage.JAPANESE))
        assertEquals(date.longLabel(AppLanguage.JAPANESE), date.longLabel(AppLanguage.CHINESE))

        AppLanguage.entries.forEach { language ->
            (1..12).forEach { month ->
                assertTrue(language.monthLong(month).isNotBlank(), "$language month $month")
                assertTrue(language.monthShort(month).isNotBlank(), "$language short $month")
            }
        }
    }
}
