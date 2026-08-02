package com.duylt.trave.vietlensai.domain.model

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

    @Test
    fun `language opposite is what a menu should be translated into`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.VIETNAMESE.opposite)
        assertEquals(AppLanguage.VIETNAMESE, AppLanguage.ENGLISH.opposite)
    }
}
