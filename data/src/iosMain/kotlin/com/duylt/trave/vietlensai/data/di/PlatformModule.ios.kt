package com.duylt.trave.vietlensai.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.duylt.trave.vietlensai.data.local.asset.BundledAssets
import com.duylt.trave.vietlensai.data.local.asset.IosBundledAssets
import com.duylt.trave.vietlensai.data.local.datastore.SETTINGS_FILE_NAME
import com.duylt.trave.vietlensai.data.local.datastore.createSettingsDataStore
import com.duylt.trave.vietlensai.data.local.db.VietLensDatabase
import com.duylt.trave.vietlensai.data.local.file.IosCaptureStore
import com.duylt.trave.vietlensai.data.ocr.VisionTextRecognizer
import com.duylt.trave.vietlensai.data.platform.CoreLocationRepository
import com.duylt.trave.vietlensai.data.platform.iosAppSupportDirectory
import com.duylt.trave.vietlensai.domain.repository.CaptureStore
import com.duylt.trave.vietlensai.domain.repository.LocationRepository
import com.duylt.trave.vietlensai.domain.repository.TextRecognizer
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

    single<VietLensDatabase> {
        Room.databaseBuilder<VietLensDatabase>(
            name = "${iosAppSupportDirectory()}/${VietLensDatabase.NAME}",
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
