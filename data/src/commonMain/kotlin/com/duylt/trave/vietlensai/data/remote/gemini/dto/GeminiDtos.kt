package com.duylt.trave.vietlensai.data.remote.gemini.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Wire format for `generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`.
 *
 * Hand-rolled rather than taken from an SDK: the Google AI client libraries move
 * quickly and lag new model families, while the REST contract has stayed stable —
 * and owning the DTOs is what lets [GenerationConfig.responseSchema] carry an
 * arbitrary JSON Schema instead of whatever an SDK builder happens to support.
 */
@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    @SerialName("systemInstruction") val systemInstruction: Content? = null,
    @SerialName("generationConfig") val generationConfig: GenerationConfig? = null,
    @SerialName("safetySettings") val safetySettings: List<SafetySetting>? = null,
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null,
)

/**
 * A single piece of a turn.
 *
 * Gemini 3 attaches a [thoughtSignature] to parts and may emit reasoning parts
 * flagged with [thought]; both have to be tolerated on the way in and ignored
 * when reading the answer out.
 */
@Serializable
data class Part(
    val text: String? = null,
    @SerialName("inlineData") val inlineData: InlineData? = null,
    val thought: Boolean? = null,
    @SerialName("thoughtSignature") val thoughtSignature: String? = null,
)

@Serializable
data class InlineData(
    @SerialName("mimeType") val mimeType: String,
    /** Base64, no data-URI prefix. */
    val data: String,
)

@Serializable
data class GenerationConfig(
    @SerialName("responseMimeType") val responseMimeType: String? = null,
    @SerialName("responseSchema") val responseSchema: JsonObject? = null,
    val temperature: Float? = null,
    @SerialName("maxOutputTokens") val maxOutputTokens: Int? = null,
    @SerialName("thinkingConfig") val thinkingConfig: ThinkingConfig? = null,
)

/**
 * Gemini 3 thinks before answering. Recognition is a perception task where extra
 * deliberation mostly costs latency, so the app dials this down per request.
 */
@Serializable
data class ThinkingConfig(
    @SerialName("thinkingLevel") val thinkingLevel: String? = null,
)

@Serializable
data class SafetySetting(
    val category: String,
    val threshold: String,
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null,
    @SerialName("usageMetadata") val usageMetadata: UsageMetadata? = null,
    @SerialName("modelVersion") val modelVersion: String? = null,
    @SerialName("promptFeedback") val promptFeedback: PromptFeedback? = null,
) {
    /**
     * The answer text, with reasoning parts filtered out.
     *
     * Thought parts carry no user-facing content but *do* carry text, so
     * concatenating everything blindly corrupts the JSON payload downstream.
     */
    val text: String?
        get() = candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.filter { it.thought != true }
            ?.mapNotNull { it.text }
            ?.joinToString(separator = "")
            ?.takeIf { it.isNotBlank() }
}

@Serializable
data class Candidate(
    val content: Content? = null,
    @SerialName("finishReason") val finishReason: String? = null,
    val index: Int? = null,
)

@Serializable
data class UsageMetadata(
    @SerialName("promptTokenCount") val promptTokenCount: Int? = null,
    @SerialName("candidatesTokenCount") val candidatesTokenCount: Int? = null,
    @SerialName("totalTokenCount") val totalTokenCount: Int? = null,
)

@Serializable
data class PromptFeedback(
    @SerialName("blockReason") val blockReason: String? = null,
)

@Serializable
data class GeminiErrorEnvelope(
    val error: GeminiError? = null,
)

@Serializable
data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null,
)
