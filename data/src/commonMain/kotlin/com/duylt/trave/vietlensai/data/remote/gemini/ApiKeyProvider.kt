package com.duylt.trave.vietlensai.data.remote.gemini

import com.duylt.trave.vietlensai.data.DataBuildConfig
import com.duylt.trave.vietlensai.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/** Resolves which Gemini key to send with a request. */
internal interface ApiKeyProvider {
    suspend fun apiKey(): String?
}

/**
 * Prefers a key the user pasted into Settings, and falls back to the one baked in
 * from `local.properties` at build time.
 *
 * That ordering is what lets the app ship to a judge or teammate with a working
 * key while still letting anyone swap in their own quota without a rebuild.
 */
internal class DefaultApiKeyProvider(
    private val settingsRepository: SettingsRepository,
) : ApiKeyProvider {

    override suspend fun apiKey(): String? {
        val userKey = settingsRepository.settings.first().apiKey
        return userKey?.takeIf { it.isNotBlank() }
            ?: DataBuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }
    }
}
