package com.evora.technologies.saola.data.di

import com.evora.technologies.saola.data.local.asset.CatalogAssetSource
import com.evora.technologies.saola.data.local.asset.ProvinceAssetSource
import com.evora.technologies.saola.data.remote.gemini.ApiKeyProvider
import com.evora.technologies.saola.data.remote.gemini.DefaultApiKeyProvider
import com.evora.technologies.saola.data.remote.gemini.GeminiClient
import com.evora.technologies.saola.data.remote.gemini.GeminiRemoteDataSource
import com.evora.technologies.saola.data.remote.openmap.OverpassClient
import com.evora.technologies.saola.data.remote.openmap.WikipediaClient
import com.evora.technologies.saola.data.repository.CaptureMaintenanceImpl
import com.evora.technologies.saola.data.repository.CatalogRepositoryImpl
import com.evora.technologies.saola.data.repository.ChatRepositoryImpl
import com.evora.technologies.saola.data.repository.DiscoveryRepositoryImpl
import com.evora.technologies.saola.data.repository.JournalRepositoryImpl
import com.evora.technologies.saola.data.repository.NoteRepositoryImpl
import com.evora.technologies.saola.data.repository.PlaceRepositoryImpl
import com.evora.technologies.saola.data.repository.ProvinceRepositoryImpl
import com.evora.technologies.saola.data.repository.SettingsRepositoryImpl
import com.evora.technologies.saola.data.repository.TranslationRepositoryImpl
import com.evora.technologies.saola.domain.repository.CaptureMaintenance
import com.evora.technologies.saola.domain.repository.CatalogRepository
import com.evora.technologies.saola.domain.repository.ChatRepository
import com.evora.technologies.saola.domain.repository.DiscoveryRepository
import com.evora.technologies.saola.domain.repository.JournalRepository
import com.evora.technologies.saola.domain.repository.NoteRepository
import com.evora.technologies.saola.domain.repository.PlaceRepository
import com.evora.technologies.saola.domain.repository.ProvinceRepository
import com.evora.technologies.saola.domain.repository.SettingsRepository
import com.evora.technologies.saola.domain.repository.TranslationRepository
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
