package com.duylt.trave.vietlensai.domain.repository

import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.AppSettings
import com.duylt.trave.vietlensai.domain.model.GeminiModel
import com.duylt.trave.vietlensai.domain.model.ThemePreference
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

    suspend fun setApiKey(key: String?)

    suspend fun setLanguage(language: AppLanguage)

    suspend fun setModel(model: GeminiModel)

    suspend fun setSpeakAnswers(enabled: Boolean)

    /**
     * Records that the location request has been made, whether it was granted or
     * not. One-way: a traveller who said no is not asked again on the next capture.
     */
    suspend fun setLocationAsked()

    suspend fun setThemePreference(preference: ThemePreference)
}
