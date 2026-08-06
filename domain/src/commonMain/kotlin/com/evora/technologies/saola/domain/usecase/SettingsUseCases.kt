package com.evora.technologies.saola.domain.usecase

import com.evora.technologies.saola.domain.model.AppSettings
import com.evora.technologies.saola.domain.model.ThemePreference
import com.evora.technologies.saola.domain.repository.DiscoveryRepository
import com.evora.technologies.saola.domain.repository.SettingsRepository
import com.evora.technologies.saola.domain.util.AppResult
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
 * One answer for the life of the process now that the key is the build's — it is still a
 * flow rather than a `suspend fun` because the lens reads it as one, and because the thing
 * being asked ("can this build recognise anything") is the same question it always was.
 */
class ObserveApiKeyAvailabilityUseCase(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<Boolean> =
        repository.settings.map { repository.hasUsableApiKey() }
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
