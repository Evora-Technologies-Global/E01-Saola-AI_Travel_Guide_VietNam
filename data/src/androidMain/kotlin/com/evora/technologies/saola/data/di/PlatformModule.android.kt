package com.evora.technologies.saola.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.evora.technologies.saola.data.local.asset.AndroidBundledAssets
import com.evora.technologies.saola.data.local.asset.BundledAssets
import com.evora.technologies.saola.data.local.datastore.SETTINGS_FILE_NAME
import com.evora.technologies.saola.data.local.datastore.createSettingsDataStore
import com.evora.technologies.saola.data.local.db.SaolaDatabase
import com.evora.technologies.saola.data.local.file.AndroidCaptureStore
import com.evora.technologies.saola.data.ocr.MlKitTextRecognizer
import com.evora.technologies.saola.data.platform.FusedLocationRepository
import com.evora.technologies.saola.domain.repository.CaptureStore
import com.evora.technologies.saola.domain.repository.LocationRepository
import com.evora.technologies.saola.domain.repository.TextRecognizer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

internal actual val platformDataModule: Module = module {

    // `androidContext()` resolves the application Context the entry point handed Koin at
    // startup, which is the one thing the shared graph cannot produce for itself.
    single<AndroidCaptureStore> {
        AndroidCaptureStore(androidContext(), get<CoroutineDispatcher>(IoDispatcher))
    }

    // Bound under both types: the rest of the app asks for the domain port, while ML Kit
    // needs the concrete class for the `Bitmap` accessor that only exists on Android.
    single<CaptureStore> { get<AndroidCaptureStore>() }

    single<BundledAssets> { AndroidBundledAssets(androidContext()) }

    single<TextRecognizer> {
        MlKitTextRecognizer(get<AndroidCaptureStore>(), get<CoroutineDispatcher>(IoDispatcher))
    }

    single<LocationRepository> { FusedLocationRepository(androidContext()) }

    single<SaolaDatabase> {
        val context: Context = androidContext()
        // An absolute path rather than a bare name: the multiplatform builder takes a
        // path, and `getDatabasePath` is what puts it where the pre-KMP build had it, so
        // an existing install keeps its journal.
        Room.databaseBuilder<SaolaDatabase>(
            context = context,
            name = context.getDatabasePath(SaolaDatabase.NAME).absolutePath,
        )
            .applySharedConfiguration(get(IoDispatcher))
            .build()
    }

    single<DataStore<Preferences>> {
        val context: Context = androidContext()
        createSettingsDataStore(
            // The same `filesDir/datastore/…` location `preferencesDataStoreFile` used, so
            // an upgraded install finds its settings instead of starting from defaults.
            absolutePath = File(context.filesDir, "datastore/$SETTINGS_FILE_NAME").absolutePath,
            scope = get<CoroutineScope>(ApplicationScope),
        )
    }
}
