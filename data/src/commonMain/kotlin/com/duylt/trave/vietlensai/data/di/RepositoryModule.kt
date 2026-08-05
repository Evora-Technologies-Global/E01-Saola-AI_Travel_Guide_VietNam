package com.duylt.trave.vietlensai.data.di

import com.duylt.trave.vietlensai.data.local.asset.CatalogAssetSource
import com.duylt.trave.vietlensai.data.local.asset.ProvinceAssetSource
import com.duylt.trave.vietlensai.data.remote.gemini.ApiKeyProvider
import com.duylt.trave.vietlensai.data.remote.gemini.DefaultApiKeyProvider
import com.duylt.trave.vietlensai.data.remote.gemini.GeminiClient
import com.duylt.trave.vietlensai.data.remote.gemini.GeminiRemoteDataSource
import com.duylt.trave.vietlensai.data.remote.openmap.OverpassClient
import com.duylt.trave.vietlensai.data.remote.openmap.WikipediaClient
import com.duylt.trave.vietlensai.data.repository.CaptureMaintenanceImpl
import com.duylt.trave.vietlensai.data.repository.CatalogRepositoryImpl
import com.duylt.trave.vietlensai.data.repository.ChatRepositoryImpl
import com.duylt.trave.vietlensai.data.repository.DiscoveryRepositoryImpl
import com.duylt.trave.vietlensai.data.repository.JournalRepositoryImpl
import com.duylt.trave.vietlensai.data.repository.NoteRepositoryImpl
import com.duylt.trave.vietlensai.data.repository.PlaceRepositoryImpl
import com.duylt.trave.vietlensai.data.repository.ProvinceRepositoryImpl
import com.duylt.trave.vietlensai.data.repository.SettingsRepositoryImpl
import com.duylt.trave.vietlensai.data.repository.TranslationRepositoryImpl
import com.duylt.trave.vietlensai.domain.repository.CaptureMaintenance
import com.duylt.trave.vietlensai.domain.repository.CatalogRepository
import com.duylt.trave.vietlensai.domain.repository.ChatRepository
import com.duylt.trave.vietlensai.domain.repository.DiscoveryRepository
import com.duylt.trave.vietlensai.domain.repository.JournalRepository
import com.duylt.trave.vietlensai.domain.repository.NoteRepository
import com.duylt.trave.vietlensai.domain.repository.PlaceRepository
import com.duylt.trave.vietlensai.domain.repository.ProvinceRepository
import com.duylt.trave.vietlensai.domain.repository.SettingsRepository
import com.duylt.trave.vietlensai.domain.repository.TranslationRepository
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Binds every implementation in `:data` to the interface `:domain` declares.
 *
 * This module and [dataModules] are the only things the app module needs to know about
 * the data layer — every impl below is `internal`, so nothing outside `:data` can reach
 * past an interface even by accident.
 *
 * The platform pieces — image storage, OCR, location, the database file — come from
 * [platformDataModule]. Everything below is declared once and is identical on both
 * platforms, so neither can accidentally end up with a different set of repositories.
 */
val repositoryModule: Module = module {

    single { ProvinceAssetSource(get(), get<CoroutineDispatcher>(IoDispatcher)) }

    single { CatalogAssetSource(get(), get<CoroutineDispatcher>(IoDispatcher)) }

    single<ApiKeyProvider> { DefaultApiKeyProvider(settingsRepository = get()) }

    single { GeminiClient(httpClient = get(), apiKeyProvider = get(), json = get()) }

    single { GeminiRemoteDataSource(client = get()) }

    // The same Ktor client the Gemini calls go through: one engine, one connection
    // pool, one place where a proxy or a timeout is configured. Neither takes a key —
    // OpenStreetMap, Wikipedia and Commons are all callable without one, which is the
    // whole reason the Explore map is built on them.
    single { OverpassClient(httpClient = get()) }

    single { WikipediaClient(httpClient = get()) }

    single<SettingsRepository> { SettingsRepositoryImpl(dataStore = get()) }

    single<ProvinceRepository> {
        ProvinceRepositoryImpl(
            assetSource = get(),
            discoveryDao = get(),
            captureStore = get(),
            ioDispatcher = get(IoDispatcher),
        )
    }

    single<DiscoveryRepository> {
        DiscoveryRepositoryImpl(
            discoveryDao = get(),
            noteDao = get(),
            remote = get(),
            captureStore = get(),
            settingsRepository = get(),
            provinceRepository = get(),
            ioDispatcher = get(IoDispatcher),
        )
    }

    single<CatalogRepository> {
        CatalogRepositoryImpl(
            assetSource = get(),
            discoveryRepository = get(),
            ioDispatcher = get(IoDispatcher),
        )
    }

    single<NoteRepository> {
        NoteRepositoryImpl(
            noteDao = get(),
            captureStore = get(),
            ioDispatcher = get(IoDispatcher),
        )
    }

    single<CaptureMaintenance> {
        CaptureMaintenanceImpl(
            discoveryDao = get(),
            noteDao = get(),
            translationDao = get(),
            captureStore = get(),
            ioDispatcher = get(IoDispatcher),
        )
    }

    single<ChatRepository> {
        ChatRepositoryImpl(
            chatDao = get(),
            remote = get(),
            discoveryRepository = get(),
            settingsRepository = get(),
            ioDispatcher = get(IoDispatcher),
        )
    }

    single<TranslationRepository> {
        TranslationRepositoryImpl(
            translationDao = get(),
            remote = get(),
            textRecognizer = get(),
            captureStore = get(),
            settingsRepository = get(),
            ioDispatcher = get(IoDispatcher),
        )
    }

    single<PlaceRepository> {
        PlaceRepositoryImpl(
            overpass = get(),
            wikipedia = get(),
            ioDispatcher = get(IoDispatcher),
        )
    }

    single<JournalRepository> {
        JournalRepositoryImpl(
            discoveryDao = get(),
            summaryDao = get(),
            noteDao = get(),
            remote = get(),
            captureStore = get(),
            settingsRepository = get(),
            timeZone = get(),
            ioDispatcher = get(IoDispatcher),
        )
    }
}
