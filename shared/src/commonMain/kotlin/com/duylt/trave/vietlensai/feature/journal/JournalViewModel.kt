package com.duylt.trave.vietlensai.feature.journal

import androidx.lifecycle.viewModelScope
import com.duylt.trave.vietlensai.core.mvi.MviViewModel
import com.duylt.trave.vietlensai.core.mvi.UiEffect
import com.duylt.trave.vietlensai.core.mvi.UiIntent
import com.duylt.trave.vietlensai.core.mvi.UiState
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.JournalDay
import com.duylt.trave.vietlensai.domain.model.JournalStats
import com.duylt.trave.vietlensai.domain.usecase.GenerateDaySummaryUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveCollectionUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveJournalStatsUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveJournalUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveSettingsUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveTravelPassportUseCase
import com.duylt.trave.vietlensai.domain.usecase.ToggleFavoriteUseCase
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class JournalState(
    val isLoading: Boolean = true,
    val days: List<JournalDay> = emptyList(),
    val stats: JournalStats = JournalStats(0, 0, 0, emptyMap()),
    val favoritesOnly: Boolean = false,
    val language: AppLanguage = AppLanguage.VIETNAMESE,
    /** Provinces stamped so far, and how many there are in total, for the passport row. */
    val provincesUnlocked: Int = 0,
    val provincesTotal: Int = 0,
    /** The same two numbers for the culture collection's row beneath it. */
    val itemsCollected: Int = 0,
    val itemsTotal: Int = 0,
    /** Which day is currently having its narrative written. */
    val generatingDate: LocalDate? = null,
    val error: AppError? = null,
) : UiState {
    /** 0f..1f for the passport progress bar; 0 while the province list is still loading. */
    val passportProgress: Float
        get() = if (provincesTotal == 0) 0f else provincesUnlocked.toFloat() / provincesTotal

    /** The same, for the collection; 0 while the catalogue asset is still being read. */
    val collectionProgress: Float
        get() = if (itemsTotal == 0) 0f else itemsCollected.toFloat() / itemsTotal

    val visibleDays: List<JournalDay>
        get() = if (!favoritesOnly) {
            days
        } else {
            days.mapNotNull { day ->
                day.discoveries.filter { it.isFavorite }
                    .takeIf { it.isNotEmpty() }
                    ?.let { day.copy(discoveries = it) }
            }
        }
}

sealed interface JournalIntent : UiIntent {
    data class GenerateSummary(val date: LocalDate) : JournalIntent
    data class SetFavoritesOnly(val enabled: Boolean) : JournalIntent
    data class ToggleFavorite(val discoveryId: String) : JournalIntent
    data object DismissError : JournalIntent
}

sealed interface JournalEffect : UiEffect {
    data class ShowMessage(val error: AppError) : JournalEffect
}

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
            JournalIntent.DismissError -> setState { copy(error = null) }
            is JournalIntent.GenerateSummary -> generate(intent.date)
            // The new state arrives through observeJournal(), so nothing is set here.
            is JournalIntent.ToggleFavorite -> launchSafely(onError = { setState { copy(error = it) } }) {
                toggleFavorite(intent.discoveryId)
            }
        }
    }

    private fun generate(date: LocalDate) {
        // One narrative at a time: each is a full model call, and two in flight
        // would let a slow one overwrite a fresh one when it lands.
        if (currentState.generatingDate != null) return

        launchSafely(onError = { setState { copy(error = it) } }) {
            setState { copy(generatingDate = date, error = null) }
            when (val result = generateDaySummary(date)) {
                is AppResult.Failure -> {
                    setState { copy(generatingDate = null, error = result.error) }
                    sendEffect(JournalEffect.ShowMessage(result.error))
                }
                // The written summary arrives through observeJournal(), so there is
                // nothing to assign here — Room stays the single source of truth.
                is AppResult.Success -> setState { copy(generatingDate = null) }
            }
        }
    }
}
