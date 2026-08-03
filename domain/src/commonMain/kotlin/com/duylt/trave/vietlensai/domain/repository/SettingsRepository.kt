package com.duylt.trave.vietlensai.domain.repository

import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.AppSettings
import com.duylt.trave.vietlensai.domain.model.GeminiModel
import com.duylt.trave.vietlensai.domain.model.ThemePreference
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val settings: Flow<AppSettings>

    suspend fun current(): AppSettings

    /**
     * True when a request would have a key to send.
     *
     * Distinct from [AppSettings.hasApiKey], which only reports the key the user
     * pasted: a build can also ship with one, and the UI should not nag about a
     * missing key when recognition would in fact work.
     */
    suspend fun hasUsableApiKey(): Boolean

    /**
     * Every write returns its outcome, like every other repository in this app.
     *
     * Not ceremony: the settings screen clears the key field and shows a confirmation the
     * moment [setApiKey] returns, so a write that failed silently told the traveller their
     * key was stored while recognition went on refusing to run. A caller is still free to
     * ignore the result — the toggles do, because they render from [settings] and put
     * themselves back when no write arrives.
     */
    suspend fun setApiKey(key: String?): AppResult<Unit>

    suspend fun setLanguage(language: AppLanguage): AppResult<Unit>

    suspend fun setModel(model: GeminiModel): AppResult<Unit>

    suspend fun setSpeakAnswers(enabled: Boolean): AppResult<Unit>

    /**
     * Records that the location request has been made, whether it was granted or
     * not. One-way: a traveller who said no is not asked again on the next capture.
     */
    suspend fun setLocationAsked(): AppResult<Unit>

    suspend fun setThemePreference(preference: ThemePreference): AppResult<Unit>
}
