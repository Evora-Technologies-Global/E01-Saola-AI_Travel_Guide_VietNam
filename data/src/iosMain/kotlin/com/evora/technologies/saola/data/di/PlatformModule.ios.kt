package com.evora.technologies.saola.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.evora.technologies.saola.data.local.asset.BundledAssets
import com.evora.technologies.saola.data.local.asset.IosBundledAssets
import com.evora.technologies.saola.data.local.datastore.SETTINGS_FILE_NAME
import com.evora.technologies.saola.data.local.datastore.createSettingsDataStore
import com.evora.technologies.saola.data.local.db.SaolaDatabase
import com.evora.technologies.saola.data.local.file.IosCaptureStore
import com.evora.technologies.saola.data.ocr.VisionTextRecognizer
import com.evora.technologies.saola.data.platform.CoreLocationRepository
import com.evora.technologies.saola.data.platform.iosAppSupportDirectory
import com.evora.technologies.saola.domain.repository.CaptureStore
import com.evora.technologies.saola.domain.repository.LocationRepository
import com.evora.technologies.saola.domain.repository.TextRecognizer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformDataModule: Module = module {

    single<CaptureStore> { IosCaptureStore(get<CoroutineDispatcher>(IoDispatcher)) }

    single<BundledAssets> { IosBundledAssets() }

    single<TextRecognizer> {
        VisionTextRecognizer(get(), get<CoroutineDispatcher>(IoDispatcher))
    }

    single<LocationRepository> { CoreLocationRepository() }

    single<SaolaDatabase> {
        Room.databaseBuilder<SaolaDatabase>(
            name = "${iosAppSupportDirectory()}/${SaolaDatabase.NAME}",
        )
            .applySharedConfiguration(get(IoDispatcher))
            .build()
    }

    single<DataStore<Preferences>> {
        createSettingsDataStore(
            absolutePath = "${iosAppSupportDirectory()}/$SETTINGS_FILE_NAME",
            scope = get<CoroutineScope>(ApplicationScope),
        )
    }
}
