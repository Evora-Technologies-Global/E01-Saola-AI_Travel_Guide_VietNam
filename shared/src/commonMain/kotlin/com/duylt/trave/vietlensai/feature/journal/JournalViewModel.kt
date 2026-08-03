package com.duylt.trave.vietlensai.feature.journal

import androidx.lifecycle.viewModelScope
import com.duylt.trave.vietlensai.core.mvi.MviViewModel
import com.duylt.trave.vietlensai.domain.usecase.GenerateDaySummaryUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveCollectionUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveJournalStatsUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveJournalUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveSettingsUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveTravelPassportUseCase
import com.duylt.trave.vietlensai.domain.usecase.ToggleFavoriteUseCase
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDate

class JournalViewModel(
    observeJournal: ObserveJournalUseCase,
    observeStats: ObserveJournalStatsUseCase,
    observeSettings: ObserveSettingsUseCase,
    observePassport: ObserveTravelPassportUseCase,
    observeCollection: ObserveCollectionUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val generateDaySummary: GenerateDaySummaryUseCase,
) : MviViewModel<JournalState, JournalIntent, JournalEffect>(JournalState()) {

    init {
        observeJournal()
            .onEach { days -> setState { copy(isLoading = false, days = days) } }
            .launchIn(viewModelScope)

        observeStats()
            .onEach { stats -> setState { copy(stats = stats) } }
            .launchIn(viewModelScope)

        observeSettings()
            .onEach { settings -> setState { copy(language = settings.language) } }
            .launchIn(viewModelScope)

        // Only the two counters, not the stamps: the journal shows a progress bar,
        // and holding 34 provinces with their cover photos here would keep the map's
        // data alive for a screen that never draws it.
        observePassport()
            .onEach { passport ->
                setState {
                    copy(
                        provincesUnlocked = passport.unlockedCount,
                        provincesTotal = passport.totalCount,
                    )
                }
            }
            .launchIn(viewModelScope)

        // Two counters again, for the same reason: the row draws a bar, and holding
        // sixty-one entries with the photographs behind them would keep the whole
        // board's data alive for a screen that shows a number.
        observeCollection()
            .onEach { collection ->
                setState {
                    copy(
                        itemsCollected = collection.collectedCount,
                        itemsTotal = collection.total,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: JournalIntent) {
        when (intent) {
            is JournalIntent.SetFavoritesOnly -> setState { copy(favoritesOnly = intent.enabled) }
            is JournalIntent.GenerateSummary -> generate(intent.date)
            // The new state arrives through observeJournal(), so nothing is set here.
            is JournalIntent.ToggleFavorite ->
                launchSafely(onError = { sendEffect(JournalEffect.ShowMessage(it)) }) {
                    toggleFavorite(intent.discoveryId)
                }
        }
    }

    private fun generate(date: LocalDate) {
        // One narrative at a time: each is a full model call, and two in flight
        // would let a slow one overwrite a fresh one when it lands.
        if (currentState.generatingDate != null) return

        // The crash floor reports too, rather than only lowering the spinner. An unwrapped
        // throw on this path used to leave the header stopping exactly as it does on success,
        // which is the same silence the screen had for an ordinary failure.
        launchSafely(
            onError = {
                setState { copy(generatingDate = null) }
                sendEffect(JournalEffect.ShowMessage(it))
            },
        ) {
            setState { copy(generatingDate = date) }
            when (val result = generateDaySummary(date)) {
                is AppResult.Failure -> {
                    setState { copy(generatingDate = null) }
                    sendEffect(JournalEffect.ShowMessage(result.error))
                }
                // The written summary arrives through observeJournal(), so there is
                // nothing to assign here — Room stays the single source of truth.
                is AppResult.Success -> setState { copy(generatingDate = null) }
            }
        }
    }
}
