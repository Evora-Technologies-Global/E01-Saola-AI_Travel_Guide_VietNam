package com.evora.technologies.saola.data.remote.gemini

import com.evora.technologies.saola.data.DataBuildConfig

/**
 * Resolves which Gemini key to send with a request.
 *
 * Still an interface with one implementation, because the tests need the other one:
 * `GeminiClientTest` drives the blank-key path, and it is the only way to reach
 * [com.evora.technologies.saola.domain.util.AppError.MissingApiKey] without editing a build
 * file.
 */
internal interface ApiKeyProvider {
    suspend fun apiKey(): String?
}

/**
 * The key baked in from `local.properties` at build time, and nothing else.
 *
 * It used to prefer a key the traveller had pasted into Settings and fall back to this one.
 * That card was removed on 06.08.2026 with the rest of the "Intelligence" section: asking a
 * traveller for a Gemini API key is asking them to hold a developer's credential, and the
 * fallback meant the app's behaviour depended on which of two keys happened to be in play.
 * One key, supplied by whoever builds the app.
 */
internal class DefaultApiKeyProvider : ApiKeyProvider {

    override suspend fun apiKey(): String? =
        DataBuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }
}
