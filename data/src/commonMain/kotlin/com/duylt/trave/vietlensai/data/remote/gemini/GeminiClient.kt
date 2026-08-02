package com.duylt.trave.vietlensai.data.remote.gemini

import com.duylt.trave.vietlensai.data.remote.gemini.dto.Content
import com.duylt.trave.vietlensai.data.remote.gemini.dto.GeminiErrorEnvelope
import com.duylt.trave.vietlensai.data.remote.gemini.dto.GenerateContentRequest
import com.duylt.trave.vietlensai.data.remote.gemini.dto.GenerateContentResponse
import com.duylt.trave.vietlensai.data.remote.gemini.dto.GenerationConfig
import com.duylt.trave.vietlensai.data.remote.gemini.dto.Part
import com.duylt.trave.vietlensai.data.util.log
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * The one place the app talks to Google AI Studio.
 *
 * Beyond wrapping the REST call it does two things that matter in the field:
 *
 * 1. **Model fallback.** Gemini Flash routinely answers 503 "experiencing high
 *    demand" during peak hours. Failing a capture because of that would be awful
 *    when someone is standing in front of a temple, so a request walks down a
 *    chain of models and only gives up when every one of them is busy.
 * 2. **Typed failures.** Everything leaves as [AppResult] with a domain [AppError],
 *    so no Ktor or JSON exception ever escapes the data layer.
 */
internal class GeminiClient(
    private val httpClient: HttpClient,
    private val apiKeyProvider: ApiKeyProvider,
    private val json: Json,
) {

    /**
     * Runs a schema-constrained generation, returning the parsed payload.
     *
     * @param modelChain preferred model first; later entries are tried only when an
     *   earlier one is unavailable, never when it simply gave a bad answer.
     */
    suspend fun <T : Any> generateStructured(
        modelChain: List<String>,
        systemInstruction: String,
        parts: List<Part>,
        schema: JsonObject,
        deserializer: DeserializationStrategy<T>,
        temperature: Float = DEFAULT_TEMPERATURE,
        thinkingLevel: String? = THINKING_LOW,
    ): AppResult<StructuredResponse<T>> {
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = parts, role = ROLE_USER)),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig(
                responseMimeType = MIME_JSON,
                responseSchema = schema,
                temperature = temperature,
                thinkingConfig = thinkingLevel?.let { com.duylt.trave.vietlensai.data.remote.gemini.dto.ThinkingConfig(it) },
            ),
        )

        return when (val raw = execute(modelChain, request)) {
            is AppResult.Failure -> raw
            is AppResult.Success -> {
                val text = raw.data.response.text
                    ?: return AppResult.Failure(AppError.Malformed("Empty response body"))
                try {
                    AppResult.Success(
                        StructuredResponse(
                            value = json.decodeFromString(deserializer, text),
                            modelUsed = raw.data.modelUsed,
                        ),
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.e(e) { "Gemini returned JSON that did not match the schema" }
                    AppResult.Failure(AppError.Malformed(e.message))
                }
            }
        }
    }

    /** Free-form generation for the chat, where the reply is prose rather than data. */
    suspend fun generateText(
        modelChain: List<String>,
        systemInstruction: String,
        history: List<Content>,
        temperature: Float = CHAT_TEMPERATURE,
    ): AppResult<StructuredResponse<String>> {
        val request = GenerateContentRequest(
            contents = history,
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig(
                temperature = temperature,
                thinkingConfig = com.duylt.trave.vietlensai.data.remote.gemini.dto.ThinkingConfig(THINKING_LOW),
            ),
        )

        return when (val raw = execute(modelChain, request)) {
            is AppResult.Failure -> raw
            is AppResult.Success -> raw.data.response.text
                ?.let { AppResult.Success(StructuredResponse(it.trim(), raw.data.modelUsed)) }
                ?: AppResult.Failure(AppError.Malformed("Empty response body"))
        }
    }

    /**
     * Tries each model in turn, stopping at the first definitive outcome.
     *
     * "Definitive" is the important word: a 400 or a bad key means the request is
     * wrong and retrying a different model would just burn quota, so only capacity
     * and transport failures advance the chain.
     */
    private suspend fun execute(
        modelChain: List<String>,
        request: GenerateContentRequest,
    ): AppResult<RawResponse> {
        val apiKey = apiKeyProvider.apiKey()
        if (apiKey.isNullOrBlank()) return AppResult.Failure(AppError.MissingApiKey)

        val models = modelChain.distinct().ifEmpty { return AppResult.Failure(AppError.Unexpected("No model configured")) }
        var lastRetryableError: AppError = AppError.AllModelsBusy(models)

        for (model in models) {
            when (val attempt = callModel(model, apiKey, request)) {
                is Attempt.Success -> {
                    if (attempt.response.candidates.isNullOrEmpty()) {
                        val blocked = attempt.response.promptFeedback?.blockReason
                        return AppResult.Failure(
                            AppError.NotRecognized(blocked?.let { "Blocked: $it" }),
                        )
                    }
                    return AppResult.Success(RawResponse(attempt.response, model))
                }
                is Attempt.Fatal -> return AppResult.Failure(attempt.error)
                is Attempt.Retryable -> {
                    log.w { "Gemini model $model unavailable (${attempt.error}), trying next" }
                    lastRetryableError = attempt.error
                }
            }
        }
        return AppResult.Failure(lastRetryableError)
    }

    private suspend fun callModel(
        model: String,
        apiKey: String,
        request: GenerateContentRequest,
    ): Attempt = try {
        val response: HttpResponse = httpClient.post("$BASE_URL/models/$model:generateContent") {
            header(HEADER_API_KEY, apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) {
            Attempt.Success(response.body())
        } else {
            classifyHttpError(response)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: UnresolvedAddressException) {
        Attempt.Fatal(AppError.NoConnection)
    } catch (e: HttpRequestTimeoutException) {
        // Ahead of IOException, which it extends. Behind that catch it was reported
        // as NoConnection — telling a traveller with four bars of signal that they
        // are offline, and sending them to fix a network that was never the problem.
        // Retryable rather than fatal: the next model in the chain is usually a
        // smaller one, and a smaller one is exactly what beats the clock.
        log.w(e) { "Request to $model timed out" }
        Attempt.Retryable(AppError.Timeout)
    } catch (e: IOException) {
        log.w(e) { "Network failure calling $model" }
        Attempt.Fatal(AppError.NoConnection)
    } catch (e: Exception) {
        log.e(e) { "Unexpected failure calling $model" }
        Attempt.Fatal(AppError.Unexpected(e.message))
    }

    private suspend fun classifyHttpError(response: HttpResponse): Attempt {
        val bodyText = runCatching { response.bodyAsText() }.getOrNull()
        val detail = bodyText
            ?.let { runCatching { json.decodeFromString<GeminiErrorEnvelope>(it) }.getOrNull() }
            ?.error
            ?.message
            ?: bodyText?.take(MAX_ERROR_DETAIL)

        return when (response.status.value) {
            HttpStatusCode.Unauthorized.value,
            HttpStatusCode.Forbidden.value,
            -> Attempt.Fatal(AppError.InvalidApiKey)

            HttpStatusCode.BadRequest.value ->
                if (detail?.contains("API key", ignoreCase = true) == true) {
                    Attempt.Fatal(AppError.InvalidApiKey)
                } else {
                    Attempt.Fatal(AppError.Api(response.status.value, detail))
                }

            // A retired or unknown model on this key: the next model in the chain may exist.
            HttpStatusCode.NotFound.value ->
                Attempt.Retryable(AppError.Api(response.status.value, detail))

            HttpStatusCode.TooManyRequests.value ->
                Attempt.Retryable(AppError.RateLimited(retryAfterSeconds(response)))

            in SERVER_ERROR_RANGE ->
                Attempt.Retryable(AppError.Api(response.status.value, detail))

            else -> Attempt.Fatal(AppError.Api(response.status.value, detail))
        }
    }

    private fun retryAfterSeconds(response: HttpResponse): Long? =
        response.headers["Retry-After"]?.toLongOrNull()

    private fun HttpStatusCode.isSuccess(): Boolean = value in 200..299

    private sealed interface Attempt {
        data class Success(val response: GenerateContentResponse) : Attempt
        /** Worth trying the next model. */
        data class Retryable(val error: AppError) : Attempt
        /** Trying another model cannot help. */
        data class Fatal(val error: AppError) : Attempt
    }

    private data class RawResponse(
        val response: GenerateContentResponse,
        val modelUsed: String,
    )

    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val HEADER_API_KEY = "x-goog-api-key"
        private const val MIME_JSON = "application/json"
        private const val ROLE_USER = "user"
        private const val DEFAULT_TEMPERATURE = 0.4f
        private const val CHAT_TEMPERATURE = 0.7f
        private const val THINKING_LOW = "low"
        private const val MAX_ERROR_DETAIL = 300
        private val SERVER_ERROR_RANGE = 500..599
    }
}

/** A parsed payload plus which model actually produced it, for display and debugging. */
internal data class StructuredResponse<T>(
    val value: T,
    val modelUsed: String,
)
