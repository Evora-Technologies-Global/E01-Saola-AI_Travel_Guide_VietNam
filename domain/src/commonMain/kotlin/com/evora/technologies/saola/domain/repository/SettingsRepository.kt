package com.evora.technologies.saola.domain.repository

import com.evora.technologies.saola.domain.model.AppSettings
import com.evora.technologies.saola.domain.model.ThemePreference
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val settings: Flow<AppSettings>

    suspend fun current(): AppSettings

    /**
     * True when a request would have a key to send.
     *
     * There is one key and the build supplies it, so this is a question about the build
     * rather than about the traveller — but it is still worth asking, because a build with
     * nothing in `local.properties` has a camera that cannot recognise anything, and the lens
     * says so instead of failing every shot. There is no in-app answer to it any more: the
     * key card was removed with the model picker on 06.08.2026.
     */
    suspend fun hasUsableApiKey(): Boolean

    // No setLanguage. `AppSettings.language` follows the phone's language and is not a
    // stored preference — see `deviceLanguage()` in :data.

    /**
     * Every write returns its outcome, like every other repository in this app.
     *
     * Not ceremony, even though both remaining writers are toggles that ignore it: they are
     * free to, because they render from [settings] and put themselves back when no write
     * arrives. Anything that confirms in words what it has just written must check — the
     * settings screen once cleared the key field and showed a green confirmation on a write
     * that had failed, which is the defect `SettingsViewModelTest` exists for.
     */
    suspend fun setSpeakAnswers(enabled: Boolean): AppResult<Unit>

    /**
     * Records that the location request has been made, whether it was granted or
     * not. One-way: a traveller who said no is not asked again on the next capture.
     */
    suspend fun setLocationAsked(): AppResult<Unit>

    suspend fun setThemePreference(preference: ThemePreference): AppResult<Unit>
}
