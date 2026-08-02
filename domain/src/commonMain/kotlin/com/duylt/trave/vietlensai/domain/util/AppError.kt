package com.duylt.trave.vietlensai.domain.util

/**
 * Every failure the app can surface, expressed in domain terms.
 *
 * The presentation layer maps these to localised copy; nothing here holds a
 * user-facing string, so the same error can be rendered in Vietnamese or English.
 */
sealed interface AppError {

    /** No Gemini API key configured yet — the user has to add one in Settings. */
    data object MissingApiKey : AppError

    /** The configured key was rejected by Google AI Studio. */
    data object InvalidApiKey : AppError

    /** Device is offline or the request never reached the server. */
    data object NoConnection : AppError

    /** Request reached the server but took too long. */
    data object Timeout : AppError

    /** Free-tier quota exhausted (HTTP 429). */
    data class RateLimited(val retryAfterSeconds: Long? = null) : AppError

    /**
     * Every candidate model answered with 503. Gemini returns this when a model is
     * under load, so it is worth telling the user to simply retry.
     */
    data class AllModelsBusy(val triedModels: List<String>) : AppError

    /** A model responded, but with a non-retryable HTTP error. */
    data class Api(val code: Int, val detail: String?) : AppError

    /** The model answered, but the payload did not match the requested schema. */
    data class Malformed(val detail: String?) : AppError

    /** Gemini looked at the photo and could not identify anything useful. */
    data class NotRecognized(val hint: String?) : AppError

    /** The captured file could not be read or decoded. */
    data class ImageUnavailable(val detail: String?) : AppError

    /** Location was requested but permission is missing or no fix is available. */
    data object LocationUnavailable : AppError

    /** Local persistence failed. */
    data class Storage(val detail: String?) : AppError

    data class Unexpected(val detail: String?) : AppError
}
