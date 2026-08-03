package com.duylt.trave.vietlensai.data.repository

import com.duylt.trave.vietlensai.data.DataBuildConfig
import com.duylt.trave.vietlensai.data.local.datastore.SettingsDataStore
import com.duylt.trave.vietlensai.data.platform.deviceLanguage
import com.duylt.trave.vietlensai.domain.model.AppSettings
import com.duylt.trave.vietlensai.domain.model.GeminiModel
import com.duylt.trave.vietlensai.domain.model.ThemePreference
import com.duylt.trave.vietlensai.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Settings, with a floor under them.
 *
 * [SettingsDataStore] already turns an unreadable preferences file into defaults. What is
 * caught here is everything it deliberately lets through — a mapping mistake, a value of a
 * type the file has never held before — because of who calls this: recognition, chat,
 * translation and the diary all read [current] to find the language and the model *before*
 * they reach their own guards, and [settings] and [current] have no `AppResult` to report a
 * failure in. Settings that cannot be read are settings the traveller never changed, which
 * is a working app in the phone's own language on the default model; an exception here is
 * a camera that will not take a photograph.
 *
 * The writes are the other direction and do carry an `AppResult` — they delegate straight to
 * [SettingsDataStore], which folds a failed write into [AppError.Storage] rather than
 * dropping it, so the settings screen can stop confirming a save that did not happen.
 */
internal class SettingsRepositoryImpl(
    private val dataStore: SettingsDataStore,
) : SettingsRepository {

    // The floor keeps the device language even when everything else falls back. Language is
    // not read from the preferences file at all, so a file this app cannot parse is no
    // reason to start narrating in a language the phone is not set to.
    override val settings: Flow<AppSettings> = dataStore.settings
        .fallbackOnFailure(
            AppSettings.DEFAULT.copy(language = deviceLanguage()),
            what = "read settings",
        )

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun hasUsableApiKey(): Boolean =
        current().hasApiKey || DataBuildConfig.GEMINI_API_KEY.isNotBlank()

    override suspend fun setApiKey(key: String?) = dataStore.setApiKey(key)

    override suspend fun setModel(model: GeminiModel) = dataStore.setModel(model)

    override suspend fun setSpeakAnswers(enabled: Boolean) = dataStore.setSpeakAnswers(enabled)

    override suspend fun setLocationAsked() = dataStore.setLocationAsked()

    override suspend fun setThemePreference(preference: ThemePreference) =
        dataStore.setThemePreference(preference)
}
