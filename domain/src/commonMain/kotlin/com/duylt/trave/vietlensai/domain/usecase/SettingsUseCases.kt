package com.duylt.trave.vietlensai.domain.usecase

import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.AppSettings
import com.duylt.trave.vietlensai.domain.model.GeminiModel
import com.duylt.trave.vietlensai.domain.model.ThemePreference
import com.duylt.trave.vietlensai.domain.repository.DiscoveryRepository
import com.duylt.trave.vietlensai.domain.repository.SettingsRepository
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveSettingsUseCase(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<AppSettings> = repository.settings
}

/**
 * Whether recognition can run at all right now.
 *
 * Re-evaluated whenever settings change, so clearing a key in Settings
 * immediately puts the "add a key" prompt back on the camera screen.
 */
class ObserveApiKeyAvailabilityUseCase(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<Boolean> =
        repository.settings.map { repository.hasUsableApiKey() }
}

/**
 * Persists a pasted Gemini key.
 *
 * Blank input clears the key rather than storing an empty string, so the app can
 * fall back to the build-time key baked in from local.properties.
 */
class SaveApiKeyUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(key: String) =
        repository.setApiKey(key.trim().takeIf { it.isNotEmpty() })
}

class UpdateLanguageUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(language: AppLanguage) = repository.setLanguage(language)
}

class UpdateModelUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(model: GeminiModel) = repository.setModel(model)
}

class UpdateThemeUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(preference: ThemePreference) =
        repository.setThemePreference(preference)
}

/**
 * Marks the location request as made.
 *
 * Called when the request is *sent*, not when it is granted — a traveller who
 * declined has answered the question, and asking again on the next capture would
 * be the app arguing with them. The system permission dialog stops appearing after
 * two refusals anyway, so a second ask would show nothing and look like a dead
 * button.
 */
class MarkLocationAskedUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke() = repository.setLocationAsked()
}

class ClearHistoryUseCase(
    private val repository: DiscoveryRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> = repository.deleteAll()
}
