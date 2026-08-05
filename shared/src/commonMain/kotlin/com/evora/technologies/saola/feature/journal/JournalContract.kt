package com.evora.technologies.saola.feature.journal

import com.evora.technologies.saola.core.mvi.UiEffect
import com.evora.technologies.saola.core.mvi.UiIntent
import com.evora.technologies.saola.core.mvi.UiState
import com.evora.technologies.saola.domain.model.AppLanguage
import com.evora.technologies.saola.domain.model.JournalDay
import com.evora.technologies.saola.domain.model.JournalStats
import com.evora.technologies.saola.domain.util.AppError
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
    /**
     * Which day is currently having its narrative written.
     *
     * The only trace a generation leaves in state. There is deliberately no `error` field
     * beside it: this screen draws no inline error anywhere, so a failure has nowhere to be
     * rendered and belongs entirely to [JournalEffect.ShowMessage]. A field written on every
     * failure and read by nobody is what let a failed day summary look like a spinner that
     * simply stopped — see `LLM.md` §11 row #15.
     */
    val generatingDate: LocalDate? = null,
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
}

sealed interface JournalEffect : UiEffect {
    /**
     * Something the traveller asked for did not happen.
     *
     * The only way this screen reports a failure, and therefore the only thing that must not
     * be dropped: the spinner on the day header is all that moves while a summary is being
     * written, and it stops the same way whether the story arrived or not — which reads as
     * "finished" rather than "failed".
     *
     * @param error carried on the effect rather than read back out of state by the route.
     *   The route resolves it with `userMessage()` at the moment it handles this; resolving
     *   from state instead depends on a recomposition that has not happened yet, which is
     *   exactly how the message used to be lost.
     */
    data class ShowMessage(val error: AppError) : JournalEffect
}
