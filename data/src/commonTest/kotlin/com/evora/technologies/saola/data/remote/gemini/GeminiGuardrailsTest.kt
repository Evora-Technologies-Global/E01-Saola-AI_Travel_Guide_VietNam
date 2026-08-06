package com.evora.technologies.saola.data.remote.gemini

import com.evora.technologies.saola.domain.model.AppLanguage
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.domain.model.DiscoveryCategory
import com.evora.technologies.saola.domain.model.LensMode
import com.evora.technologies.saola.domain.model.TranslateLanguage
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Guards the decision about *which* prompts carry the guardrails, because that decision
 * is invisible at the call site.
 *
 * A new prompt is written by copying an existing one, and the copy that gets picked is
 * whichever is nearest — line translation as often as recognition. Nothing in
 * `GeminiRemoteDataSource` would look wrong if a system instruction quietly went out
 * without the sovereignty block, and no test of a response could notice: the model would
 * simply answer a little differently, once, to one traveller.
 */
class GeminiGuardrailsTest {

    private val discovery = Discovery(
        id = "d1",
        title = "One Pillar Pagoda",
        localName = "Chùa Một Cột",
        category = DiscoveryCategory.LANDMARK,
        imagePath = null,
        summary = "A small pagoda raised on a single stone pillar.",
        sections = emptyList(),
        funFacts = emptyList(),
        tags = emptyList(),
        nearbySuggestions = emptyList(),
        suggestedQuestions = emptyList(),
        confidence = 0.9f,
        location = null,
        placeHint = "Ba Đình, Hà Nội",
        isFavorite = false,
        modelUsed = null,
        createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000),
    )

    /** One phrase per guardrail, each chosen to appear nowhere else in the prompts. */
    private val markers = listOf(
        "religious advocacy",      // scope
        "never triumphal",         // tone
        "revisionist",             // historical accuracy
        "Hoàng Sa",                // sovereignty
        "nine-dash line",          // sovereignty
    )

    private fun assertGuarded(instruction: String) {
        markers.forEach { marker ->
            assertTrue(marker in instruction, "System instruction is missing the rule about '$marker'")
        }
    }

    @Test
    fun recognitionCarriesEveryGuardrail() {
        AppLanguage.entries.forEach { assertGuarded(GeminiPrompts.recognitionSystemInstruction(it)) }
    }

    @Test
    fun chatCarriesEveryGuardrail() {
        AppLanguage.entries.forEach { assertGuarded(GeminiPrompts.chatSystemInstruction(discovery, it)) }
    }

    @Test
    fun theDaySummaryCarriesEveryGuardrail() {
        AppLanguage.entries.forEach { assertGuarded(GeminiPrompts.summarySystemInstruction(it)) }
    }

    /**
     * The exception, asserted rather than assumed.
     *
     * Translation renders text the traveller has pointed a camera at. Guarding it would
     * refuse a war-museum caption or a headstone — the moment they most need the words.
     */
    @Test
    fun lineTranslationIsDeliberatelyUnguarded() {
        val instruction = GeminiPrompts.lineTranslationSystemInstruction(
            source = TranslateLanguage.VIETNAMESE,
            target = TranslateLanguage.ENGLISH,
        )
        markers.forEach { marker ->
            assertFalse(marker in instruction, "Translation picked up the guardrail about '$marker'")
        }
    }

    /**
     * Out-of-scope photos have to come back through `recognized = false`, which
     * `DiscoveryRepositoryImpl` turns into `AppError.NotRecognized`. A refusal written
     * into `summary` instead would be saved to the passport as a discovery.
     */
    @Test
    fun recognitionRoutesRefusalsThroughTheSchema() {
        LensMode.entries.forEach { mode ->
            val prompt = GeminiPrompts.recognitionPrompt(mode, null, AppLanguage.ENGLISH)
            assertTrue(
                "set recognized to false" in prompt,
                "$mode does not tell the model how to refuse",
            )
            assertTrue("notRecognizedHint" in prompt, "$mode does not name the hint field")
        }
    }
}
