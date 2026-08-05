package com.evora.technologies.saola.di

import com.evora.technologies.saola.MainViewModel
import com.evora.technologies.saola.core.util.VolumeShutterBus
import com.evora.technologies.saola.data.di.ApplicationScope
import com.evora.technologies.saola.data.di.dataModules
import com.evora.technologies.saola.domain.usecase.AskFollowUpUseCase
import com.evora.technologies.saola.domain.usecase.BackfillProvincesUseCase
import com.evora.technologies.saola.domain.usecase.ClearChatUseCase
import com.evora.technologies.saola.domain.usecase.ObserveCollectionUseCase
import com.evora.technologies.saola.domain.usecase.ClearHistoryUseCase
import com.evora.technologies.saola.domain.usecase.DeleteDiscoveryUseCase
import com.evora.technologies.saola.domain.usecase.DeleteNoteUseCase
import com.evora.technologies.saola.domain.usecase.GenerateDaySummaryUseCase
import com.evora.technologies.saola.domain.usecase.GetCurrentLocationUseCase
import com.evora.technologies.saola.domain.usecase.LoadNearbyPlacesUseCase
import com.evora.technologies.saola.domain.usecase.LoadPlaceDetailsUseCase
import com.evora.technologies.saola.domain.usecase.MarkLocationAskedUseCase
import com.evora.technologies.saola.domain.usecase.ObserveApiKeyAvailabilityUseCase
import com.evora.technologies.saola.domain.usecase.ObserveChatUseCase
import com.evora.technologies.saola.domain.usecase.ObserveDiscoveriesUseCase
import com.evora.technologies.saola.domain.usecase.ObserveDiscoveryUseCase
import com.evora.technologies.saola.domain.usecase.ObserveJournalStatsUseCase
import com.evora.technologies.saola.domain.usecase.ObserveJournalUseCase
import com.evora.technologies.saola.domain.usecase.ObserveNoteUseCase
import com.evora.technologies.saola.domain.usecase.ObserveProvinceDiscoveriesUseCase
import com.evora.technologies.saola.domain.usecase.ObserveSettingsUseCase
import com.evora.technologies.saola.domain.usecase.ObserveTravelPassportUseCase
import com.evora.technologies.saola.domain.usecase.RecognizeImageUseCase
import com.evora.technologies.saola.domain.usecase.SaveApiKeyUseCase
import com.evora.technologies.saola.domain.usecase.SaveNoteUseCase
import com.evora.technologies.saola.domain.usecase.SweepOrphanCapturesUseCase
import com.evora.technologies.saola.domain.usecase.ToggleFavoriteUseCase
import com.evora.technologies.saola.domain.usecase.TranslateImageUseCase
import com.evora.technologies.saola.domain.usecase.UpdateModelUseCase
import com.evora.technologies.saola.domain.usecase.UpdateThemeUseCase
import com.evora.technologies.saola.feature.camera.LensViewModel
import com.evora.technologies.saola.feature.chat.ChatViewModel
import com.evora.technologies.saola.feature.collection.CollectionViewModel
import com.evora.technologies.saola.feature.discovery.DiscoveryViewModel
import com.evora.technologies.saola.feature.explore.ExploreViewModel
import com.evora.technologies.saola.feature.journal.JournalViewModel
import com.evora.technologies.saola.feature.passport.PassportViewModel
import com.evora.technologies.saola.feature.settings.SettingsViewModel
import com.evora.technologies.saola.feature.sovereignty.SovereigntyViewModel
import com.evora.technologies.saola.feature.translate.TranslationViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Use cases, one line each.
 *
 * Listed explicitly rather than discovered: Dagger's `@Inject` annotations are gone with Hilt,
 * and no annotation processor runs for Kotlin/Native to replace them. The upside is that this
 * file is the whole graph above the repositories — a missing binding is one grep away rather
 * than a generated-code puzzle.
 */
val useCaseModule: Module = module {
    factory { ObserveChatUseCase(get()) }
    factory { AskFollowUpUseCase(get()) }
    factory { ClearChatUseCase(get()) }

    factory { RecognizeImageUseCase(get(), get()) }
    factory { ObserveDiscoveriesUseCase(get()) }
    factory { ObserveDiscoveryUseCase(get()) }
    factory { ObserveProvinceDiscoveriesUseCase(get()) }
    factory { ToggleFavoriteUseCase(get()) }
    factory { DeleteDiscoveryUseCase(get()) }

    factory { ObserveNoteUseCase(get()) }
    factory { SaveNoteUseCase(get()) }
    factory { DeleteNoteUseCase(get()) }
    factory { SweepOrphanCapturesUseCase(get()) }

    factory { ObserveJournalUseCase(get()) }
    factory { ObserveJournalStatsUseCase(get()) }
    factory { GenerateDaySummaryUseCase(get()) }

    factory { ObserveTravelPassportUseCase(get()) }
    factory { BackfillProvincesUseCase(get()) }

    factory { GetCurrentLocationUseCase(get()) }
    factory { LoadNearbyPlacesUseCase(get()) }
    factory { LoadPlaceDetailsUseCase(get()) }

    factory { ObserveCollectionUseCase(get()) }

    factory { TranslateImageUseCase(get()) }

    factory { ObserveSettingsUseCase(get()) }
    factory { ObserveApiKeyAvailabilityUseCase(get()) }
    factory { SaveApiKeyUseCase(get()) }
    factory { UpdateModelUseCase(get()) }
    factory { UpdateThemeUseCase(get()) }
    factory { MarkLocationAskedUseCase(get()) }
    factory { ClearHistoryUseCase(get()) }
}

/**
 * The presentation layer.
 *
 * `viewModel { }` rather than `single { }` so each screen gets one tied to its own
 * `ViewModelStoreOwner` — the navigation entry on both platforms — and is cleared with it.
 */
val presentationModule: Module = module {
    // Shared by the window and the lens screen: the volume keys arrive at the Activity on
    // Android, and nothing but a process-wide object can carry them to the viewfinder.
    single { VolumeShutterBus() }

    viewModel {
        MainViewModel(
            observeSettings = get(),
            sweepOrphanCaptures = get(),
            seedDemoData = get(),
        )
    }

    viewModel {
        LensViewModel(
            recognizeImage = get(),
            captureStore = get(),
            volumeShutter = get(),
            markLocationAsked = get(),
            observeDiscoveries = get(),
            observeApiKeyAvailability = get(),
            observeSettings = get(),
        )
    }

    // `SavedStateHandle` is resolved by Koin's ViewModel support from the navigation entry's
    // own saved state, which is how the chat, discovery and translation screens read the
    // route arguments they were opened with.
    //
    // Chat also accepts the id outright, because the tablet's guide column is not a
    // destination and has no route of its own to read — `parametersOf(discoveryId)` at the
    // call site lands in the same holder, looked up by type. `getOrNull` rather than `get`:
    // the phone passes nothing and falls back to the handle. See `ChatViewModel`'s KDoc.
    viewModel { params ->
        ChatViewModel(
            savedStateHandle = params.get(),
            explicitDiscoveryId = params.getOrNull<String>(),
            observeDiscovery = get(),
            observeChat = get(),
            observeSettings = get(),
            askFollowUp = get(),
            clearChat = get(),
            textToSpeech = get(),
        )
    }

    viewModel { params ->
        DiscoveryViewModel(
            savedStateHandle = params.get(),
            observeDiscovery = get(),
            observeSettings = get(),
            observeNote = get(),
            toggleFavorite = get(),
            deleteDiscovery = get(),
            saveNote = get(),
            deleteNote = get(),
            captureStore = get(),
            applicationScope = get(ApplicationScope),
            textToSpeech = get(),
        )
    }

    viewModel { params ->
        TranslationViewModel(
            savedStateHandle = params.get(),
            translateImage = get(),
            observeSettings = get(),
            textToSpeech = get(),
        )
    }

    viewModel {
        JournalViewModel(
            observeJournal = get(),
            observeStats = get(),
            observeSettings = get(),
            observePassport = get(),
            observeCollection = get(),
            toggleFavorite = get(),
            generateDaySummary = get(),
        )
    }

    viewModel {
        PassportViewModel(
            observePassport = get(),
            observeJournalStats = get(),
            observeSettings = get(),
            observeProvinceDiscoveries = get(),
            backfillProvinces = get(),
            captureStore = get(),
        )
    }

    viewModel {
        CollectionViewModel(observeCollection = get())
    }

    viewModel {
        ExploreViewModel(
            getCurrentLocation = get(),
            loadNearbyPlaces = get(),
            loadPlaceDetails = get(),
            observeSettings = get(),
        )
    }

    viewModel {
        SettingsViewModel(
            observeSettings = get(),
            observeApiKeyAvailability = get(),
            settingsRepository = get(),
            saveApiKey = get(),
            updateModel = get(),
            updateTheme = get(),
            clearHistory = get(),
        )
    }

    viewModel { SovereigntyViewModel() }
}

/**
 * Whatever the platform has to supply that shared code cannot build for itself: the narration
 * engine, which needs an Android `Context` on one side and nothing on the other.
 */
internal expect val platformUiModule: Module

/**
 * Everything the app needs, in the order Koin should see it.
 *
 * Both entry points — `SaolaApplication` on Android, `startSaola` from the iOS framework —
 * pass exactly this list, so neither platform can end up with a graph the other does not have.
 *
 * @param isDebug true only for a development build. Nothing in the presentation layer reads it;
 *   it is threaded through to `:data`, which uses it to decide whether the HTTP client logs.
 *   Passing it in is what keeps a release build quiet — `:data` and `:shared` are single-variant
 *   multiplatform modules, so neither one can see which of `:app`'s build types it landed in.
 */
fun appModules(isDebug: Boolean): List<Module> =
    dataModules(isDebug) + useCaseModule + presentationModule + platformUiModule
