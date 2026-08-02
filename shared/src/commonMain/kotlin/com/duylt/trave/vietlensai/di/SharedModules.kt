package com.duylt.trave.vietlensai.di

import com.duylt.trave.vietlensai.MainViewModel
import com.duylt.trave.vietlensai.core.util.VolumeShutterBus
import com.duylt.trave.vietlensai.data.di.ApplicationScope
import com.duylt.trave.vietlensai.data.di.dataModules
import com.duylt.trave.vietlensai.domain.usecase.AskFollowUpUseCase
import com.duylt.trave.vietlensai.domain.usecase.BackfillProvincesUseCase
import com.duylt.trave.vietlensai.domain.usecase.ClearChatUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveCollectionUseCase
import com.duylt.trave.vietlensai.domain.usecase.ClearHistoryUseCase
import com.duylt.trave.vietlensai.domain.usecase.DeleteDiscoveryUseCase
import com.duylt.trave.vietlensai.domain.usecase.DeleteNoteUseCase
import com.duylt.trave.vietlensai.domain.usecase.GenerateDaySummaryUseCase
import com.duylt.trave.vietlensai.domain.usecase.GetCurrentLocationUseCase
import com.duylt.trave.vietlensai.domain.usecase.LoadNearbyPlacesUseCase
import com.duylt.trave.vietlensai.domain.usecase.LoadPlaceDetailsUseCase
import com.duylt.trave.vietlensai.domain.usecase.MarkLocationAskedUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveApiKeyAvailabilityUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveChatUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveDiscoveriesUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveDiscoveryUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveJournalStatsUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveJournalUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveNoteUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveProvinceDiscoveriesUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveSettingsUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveTravelPassportUseCase
import com.duylt.trave.vietlensai.domain.usecase.RecognizeImageUseCase
import com.duylt.trave.vietlensai.domain.usecase.SaveApiKeyUseCase
import com.duylt.trave.vietlensai.domain.usecase.SaveNoteUseCase
import com.duylt.trave.vietlensai.domain.usecase.SweepOrphanCapturesUseCase
import com.duylt.trave.vietlensai.domain.usecase.ToggleFavoriteUseCase
import com.duylt.trave.vietlensai.domain.usecase.TranslateImageUseCase
import com.duylt.trave.vietlensai.domain.usecase.UpdateLanguageUseCase
import com.duylt.trave.vietlensai.domain.usecase.UpdateModelUseCase
import com.duylt.trave.vietlensai.domain.usecase.UpdateThemeUseCase
import com.duylt.trave.vietlensai.feature.camera.LensViewModel
import com.duylt.trave.vietlensai.feature.chat.ChatViewModel
import com.duylt.trave.vietlensai.feature.collection.CollectionViewModel
import com.duylt.trave.vietlensai.feature.discovery.DiscoveryViewModel
import com.duylt.trave.vietlensai.feature.explore.ExploreViewModel
import com.duylt.trave.vietlensai.feature.journal.JournalViewModel
import com.duylt.trave.vietlensai.feature.passport.PassportViewModel
import com.duylt.trave.vietlensai.feature.settings.SettingsViewModel
import com.duylt.trave.vietlensai.feature.sovereignty.SovereigntyViewModel
import com.duylt.trave.vietlensai.feature.translate.TranslationViewModel
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
    factory { UpdateLanguageUseCase(get()) }
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
    viewModel { params ->
        ChatViewModel(
            savedStateHandle = params.get(),
            observeDiscovery = get(),
            observeChat = get(),
            observeSettings = get(),
            askFollowUp = get(),
            clearChat = get(),
            speechRecognizer = get(),
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
            updateLanguage = get(),
            updateModel = get(),
            updateTheme = get(),
            clearHistory = get(),
        )
    }

    viewModel { SovereigntyViewModel() }
}

/**
 * Whatever the platform has to supply that shared code cannot build for itself: the speech
 * engines, which need an Android `Context` on one side and nothing on the other.
 */
internal expect val platformUiModule: Module

/**
 * Everything the app needs, in the order Koin should see it.
 *
 * Both entry points — `VietLensApplication` on Android, `startVietLens` from the iOS framework —
 * pass exactly this list, so neither platform can end up with a graph the other does not have.
 *
 * @param isDebug true only for a development build. Nothing in the presentation layer reads it;
 *   it is threaded through to `:data`, which uses it to decide whether the HTTP client logs.
 *   Passing it in is what keeps a release build quiet — `:data` and `:shared` are single-variant
 *   multiplatform modules, so neither one can see which of `:app`'s build types it landed in.
 */
fun appModules(isDebug: Boolean): List<Module> =
    dataModules(isDebug) + useCaseModule + presentationModule + platformUiModule
