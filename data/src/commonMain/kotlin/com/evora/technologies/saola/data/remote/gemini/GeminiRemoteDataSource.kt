package com.evora.technologies.saola.data.remote.gemini

import com.evora.technologies.saola.data.remote.gemini.dto.Content
import com.evora.technologies.saola.data.remote.gemini.dto.DiscoveryPayload
import com.evora.technologies.saola.data.remote.gemini.dto.InlineData
import com.evora.technologies.saola.data.remote.gemini.dto.Part
import com.evora.technologies.saola.data.remote.gemini.dto.LineTranslationPayload
import com.evora.technologies.saola.data.remote.gemini.dto.TripSummaryPayload
import com.evora.technologies.saola.domain.model.AppLanguage
import com.evora.technologies.saola.domain.model.ChatMessage
import com.evora.technologies.saola.domain.model.ChatRole
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.domain.model.GeminiModel
import com.evora.technologies.saola.domain.model.GeoPoint
import com.evora.technologies.saola.domain.model.LensMode
import com.evora.technologies.saola.domain.model.TranslateLanguage
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.serialization.serializer
import kotlin.io.encoding.Base64

/**
 * Feature-shaped wrapper over [GeminiClient].
 *
 * Repositories ask for "recognise this photo" rather than assembling parts,
 * schemas and prompts themselves, which keeps prompt engineering in one file and
 * out of the persistence code.
 */
internal class GeminiRemoteDataSource(
    private val client: GeminiClient,
) {

    suspend fun recognize(
        imageBytes: ByteArray,
        mimeType: String,
        mode: LensMode,
        location: GeoPoint?,
        language: AppLanguage,
        model: GeminiModel,
    ): AppResult<StructuredResponse<DiscoveryPayload>> = client.generateStructured(
        modelChain = model.fallbackChain,
        systemInstruction = GeminiPrompts.recognitionSystemInstruction(language),
        parts = listOf(
            Part(text = GeminiPrompts.recognitionPrompt(mode, location, language)),
            Part(inlineData = InlineData(mimeType = mimeType, data = imageBytes.toBase64())),
        ),
        schema = GeminiSchemas.discovery,
        deserializer = serializer(),
    )

    /**
     * Translates lines the device already read off the photo.
     *
     * Text-only on purpose: the picture has been through a recogniser that knows
     * exactly where every line is, so sending it again would spend an image's worth
     * of tokens inviting the model to disagree about what the lines even were.
     */
    suspend fun translateLines(
        lines: List<String>,
        source: TranslateLanguage?,
        target: TranslateLanguage,
        model: GeminiModel,
    ): AppResult<StructuredResponse<LineTranslationPayload>> = client.generateStructured(
        modelChain = model.fallbackChain,
        systemInstruction = GeminiPrompts.lineTranslationSystemInstruction(source, target),
        parts = listOf(Part(text = GeminiPrompts.lineTranslationPrompt(lines))),
        schema = GeminiSchemas.lineTranslation,
        deserializer = serializer(),
        // Transcription is not a creative task; keep it as literal as possible.
        temperature = 0.1f,
    )

    /**
     * Continues a conversation about a discovery.
     *
     * Prior turns are replayed as real multi-turn [Content] rather than flattened
     * into the prompt, so the model sees the exchange the way it was written.
     */
    suspend fun chat(
        discovery: Discovery,
        history: List<ChatMessage>,
        question: String,
        language: AppLanguage,
        model: GeminiModel,
    ): AppResult<StructuredResponse<String>> {
        val turns = history.takeLast(MAX_HISTORY_TURNS).map { message ->
            Content(
                parts = listOf(Part(text = message.content)),
                role = message.role.wireName,
            )
        }
        return client.generateText(
            modelChain = model.fallbackChain,
            systemInstruction = GeminiPrompts.chatSystemInstruction(discovery, language),
            history = turns + Content(
                parts = listOf(Part(text = question)),
                role = ChatRole.USER.wireName,
            ),
        )
    }

    /** @param notes what the traveller wrote themselves that day; empty when they wrote nothing. */
    suspend fun daySummary(
        dateLabel: String,
        entries: List<String>,
        notes: List<String>,
        language: AppLanguage,
        model: GeminiModel,
    ): AppResult<StructuredResponse<TripSummaryPayload>> = client.generateStructured(
        modelChain = model.fallbackChain,
        systemInstruction = GeminiPrompts.summarySystemInstruction(language),
        parts = listOf(Part(text = GeminiPrompts.summaryPrompt(dateLabel, entries, notes))),
        schema = GeminiSchemas.tripSummary,
        deserializer = serializer(),
        temperature = 0.8f,
    )

    /**
     * `android.util.Base64` with NO_WRAP, expressed in the common stdlib.
     *
     * The default `Base64.Default` encoder emits no line breaks, which is what NO_WRAP
     * asked for — the Gemini REST API rejects a wrapped payload.
     */
    private fun ByteArray.toBase64(): String = Base64.Default.encode(this)

    private companion object {
        /** Enough for a real conversation without paying to resend the whole trip. */
        const val MAX_HISTORY_TURNS = 20
    }
}
