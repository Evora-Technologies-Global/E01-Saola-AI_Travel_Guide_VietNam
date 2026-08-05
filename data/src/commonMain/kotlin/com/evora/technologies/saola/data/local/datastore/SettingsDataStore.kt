package com.evora.technologies.saola.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException as DataStoreIOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.evora.technologies.saola.data.platform.deviceLanguage
import com.evora.technologies.saola.data.util.log
import com.evora.technologies.saola.domain.model.AppSettings
import com.evora.technologies.saola.domain.model.GeminiModel
import com.evora.technologies.saola.domain.model.ThemePreference
import com.evora.technologies.saola.domain.util.AppError
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException as OkioIOException

/**
 * Preferences-backed storage for [AppSettings].
 *
 * A corrupt preferences file falls back to defaults rather than propagating an
 * exception — settings are never important enough to stop the app from starting.
 */
internal class SettingsDataStore(
    private val dataStore: DataStore<Preferences>,
) {

    val settings: Flow<AppSettings> = dataStore.data
        .catch { throwable ->
            if (throwable.isUnreadablePreferences()) {
                log.w(throwable) { "Could not read settings, falling back to defaults" }
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { prefs ->
            AppSettings(
                apiKey = prefs[Keys.API_KEY]?.takeIf { it.isNotBlank() },
                // Read from the phone, not from storage. See [deviceLanguage].
                language = deviceLanguage(),
                preferredModel = GeminiModel.fromId(prefs[Keys.MODEL]),
                speakAnswers = prefs[Keys.SPEAK_ANSWERS] ?: AppSettings.DEFAULT.speakAnswers,
                hasAskedLocation = prefs[Keys.LOCATION_ASKED]
                    ?: AppSettings.DEFAULT.hasAskedLocation,
                darkTheme = prefs[Keys.THEME]
                    ?.let { stored -> ThemePreference.entries.firstOrNull { it.name == stored } }
                    ?: AppSettings.DEFAULT.darkTheme,
            )
        }

    suspend fun setApiKey(key: String?) = edit { prefs ->
        if (key.isNullOrBlank()) prefs.remove(Keys.API_KEY) else prefs[Keys.API_KEY] = key
    }

    suspend fun setModel(model: GeminiModel) = edit { it[Keys.MODEL] = model.id }

    suspend fun setSpeakAnswers(enabled: Boolean) = edit { it[Keys.SPEAK_ANSWERS] = enabled }

    suspend fun setLocationAsked() = edit { it[Keys.LOCATION_ASKED] = true }

    suspend fun setThemePreference(preference: ThemePreference) =
        edit { it[Keys.THEME] = preference.name }

    /**
     * A failed settings write is reported, not swallowed.
     *
     * It used to be logged and dropped, on the reasoning that "nothing on screen is waiting
     * on it, and the value the traveller typed is still in the field they typed it into".
     * That is true of the toggles and false of the one write that matters: on `SaveApiKey`
     * the screen clears the field *and* shows a green "API key saved", so a failed write
     * produced a discarded key, a confirmation that it was stored, and a status pill one row
     * below still reading "no key configured" — with nothing connecting the three. Returning
     * the outcome lets the caller decide; the toggles are free to ignore it, because their
     * own UI is driven off the settings flow and reverts by itself when no write lands.
     *
     * [CancellationException] is rethrown first. `runCatching` here would catch `Throwable`
     * and absorb the cancellation raised when the settings screen closes mid-write, reporting
     * an ordinary navigation as a failed save.
     */
    private suspend inline fun edit(
        crossinline block: (MutablePreferences) -> Unit,
    ): AppResult<Unit> =
        try {
            dataStore.edit { prefs -> block(prefs) }
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "Could not persist settings change" }
            AppResult.Failure(AppError.Storage(e.message))
        }

    private object Keys {
        val API_KEY = stringPreferencesKey("gemini_api_key")
        // No LANGUAGE key. The old "language" preference is left unread rather than
        // migrated: on an upgraded install it holds whatever the in-app picker was last
        // set to, which is exactly the value this change exists to stop honouring.
        val MODEL = stringPreferencesKey("gemini_model")
        val SPEAK_ANSWERS = booleanPreferencesKey("speak_answers")
        // A new key rather than a reuse of the old `use_location`: that one was
        // written as "the traveller wants location on", and on an upgraded install
        // it holds `true` for people who were never actually asked for the
        // permission. Read as "already asked", it would suppress the one prompt
        // this whole change exists to show.
        val LOCATION_ASKED = booleanPreferencesKey("location_asked")
        val THEME = stringPreferencesKey("theme_preference")
    }
}

/**
 * Whether a throwable out of `DataStore` means "the file is unreadable" rather than "there
 * is a bug here".
 *
 * Three unrelated classes on the way to that `catch` are called `IOException`, and only the
 * JVM collapses them: `androidx.datastore.core.IOException`, `okio.IOException` and
 * `kotlinx.io.IOException` are all `typealias`es for `java.io.IOException` there, and three
 * distinct types on Apple targets. The check used to name the `kotlinx.io` one, which
 * nothing in this stack ever throws — the typealias made it right by accident on Android,
 * while on iOS it matched nothing, the throwable was rethrown, and a corrupt preferences
 * file escaped every `AppResult` boundary in the app. Settings are read by every screen, so
 * that is not a settings bug, it is a launch failure.
 *
 * The two that genuinely arrive are named instead of being replaced with a broader
 * supertype. `PreferenceDataStoreFactory.createWithPath` builds an okio-backed store on
 * both platforms, and okio throws `okio.IOException` for a file it cannot read while the
 * preferences serialiser throws `CorruptionException` — an
 * `androidx.datastore.core.IOException` — for one it cannot parse. Catching `Exception`
 * here would also swallow a mistake in the `map` that follows the catch, and "the app
 * silently forgot your API key" is a far worse way to learn about that than a crash in
 * development.
 */
private fun Throwable.isUnreadablePreferences(): Boolean =
    this is DataStoreIOException || this is OkioIOException
