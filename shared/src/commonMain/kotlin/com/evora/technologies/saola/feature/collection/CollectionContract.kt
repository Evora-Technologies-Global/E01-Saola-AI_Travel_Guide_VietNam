package com.evora.technologies.saola.feature.collection

import com.evora.technologies.saola.core.mvi.MviViewModel
import com.evora.technologies.saola.core.mvi.UiEffect
import com.evora.technologies.saola.core.mvi.UiIntent
import com.evora.technologies.saola.core.mvi.UiState
import com.evora.technologies.saola.domain.model.CollectionEntry
import com.evora.technologies.saola.domain.model.CultureCollection

/**
 * @param selectedItemId which tile's sheet is open, held as an id rather than as the
 *   entry itself so that a photograph taken while the sheet is up re-renders it as
 *   collected instead of leaving a stale copy on screen.
 * @param isGuide whether the sixty-one recognition hints are spelled out beside the entries
 *   instead of waiting behind a tap. Defaults to the board, because that is what the screen
 *   is *for* — the traveller's own photographs — and the guide is what they switch to when
 *   they want to go and find the next one.
 *
 *   **On the state rather than in a `rememberSaveable` inside a screen, and the branch split
 *   is the reason.** The switch is drawn in the page header and the mode is read by the list
 *   below it, which on both arrangements are two different composables; lifted only as far as
 *   the nearest common parent it would be declared once under `mobile/` and once under
 *   `tablet/`, which is two answers to one question and the thing `LLM.md` §3 forbids. It is
 *   a fact about the screen, so it lives where the screen's facts live.
 */
data class CollectionState(
    val isLoading: Boolean = true,
    val collection: CultureCollection = CultureCollection.EMPTY,
    val selectedItemId: String? = null,
    val isGuide: Boolean = false,
) : UiState {

    val selected: CollectionEntry?
        get() = selectedItemId?.let { id ->
            collection.sections.firstNotNullOfOrNull { section ->
                section.entries.firstOrNull { it.item.id == id }
            }
        }
}

sealed interface CollectionIntent : UiIntent {
    data class Select(val itemId: String) : CollectionIntent
    data object DismissSelection : CollectionIntent

    /**
     * The traveller asked to see how to recognise these things, or to stop seeing it.
     *
     * One intent for both directions rather than `ShowGuide` / `ShowBoard`: the control is one
     * button, so "pressed the switch" is one user event, and the ViewModel is where what that
     * means is decided.
     */
    data object ToggleView : CollectionIntent
}

/**
 * Nothing to emit.
 *
 * The screen reads, and the three things it can do — go back, open a discovery, open
 * the lens — are navigation the route already owns. Declared rather than removed
 * because [MviViewModel] is typed on an effect, and a sealed interface with no cases
 * states exactly what is true: there are no effects to send.
 */
sealed interface CollectionEffect : UiEffect
