package com.evora.technologies.saola.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import okio.Path.Companion.toPath

/** The settings file name, shared so both platforms resolve the same file. */
internal const val SETTINGS_FILE_NAME = "saola_settings.preferences_pb"

/**
 * Builds the preferences store from an absolute path.
 *
 * `PreferenceDataStoreFactory.createWithPath` rather than the Android `preferencesDataStoreFile`
 * helper: the helper lives in `datastore-preferences`, which is Android-only, while this
 * factory is in `datastore-preferences-core` and takes an okio path on every target. Only
 * *where* the file goes is platform business, and that decision is made in
 * [com.evora.technologies.saola.data.di.platformDataModule].
 */
internal fun createSettingsDataStore(
    absolutePath: String,
    scope: CoroutineScope,
): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    scope = scope,
    produceFile = { absolutePath.toPath() },
)
