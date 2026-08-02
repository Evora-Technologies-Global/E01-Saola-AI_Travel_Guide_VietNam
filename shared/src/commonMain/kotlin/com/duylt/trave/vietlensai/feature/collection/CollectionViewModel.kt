package com.duylt.trave.vietlensai.feature.collection

import androidx.lifecycle.viewModelScope
import com.duylt.trave.vietlensai.core.mvi.MviViewModel
import com.duylt.trave.vietlensai.core.mvi.UiEffect
import com.duylt.trave.vietlensai.core.mvi.UiIntent
import com.duylt.trave.vietlensai.core.mvi.UiState
import com.duylt.trave.vietlensai.domain.model.CollectionEntry
import com.duylt.trave.vietlensai.domain.model.CultureCollection
import com.duylt.trave.vietlensai.domain.usecase.ObserveCollectionUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @param selectedItemId which tile's sheet is open, held as an id rather than as the
 *   entry itself so that a photograph taken while the sheet is up re-renders it as
 *   collected instead of leaving a stale copy on screen.
 */
data class CollectionState(
    val isLoading: Boolean = true,
    val collection: CultureCollection = CultureCollection.EMPTY,
    val selectedItemId: String? = null,
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

/**
 * The culture collection.
 *
 * There is no loading of its own and no refresh: the catalogue is an asset and the
 * photographs come from Room, so the board is assembled from data already on the
 * device and re-emits by itself whenever something new is recognised. That is also
 * why the screen has no error state — nothing here can fail in a way the traveller
 * could act on.
 */
class CollectionViewModel(
    observeCollection: ObserveCollectionUseCase,
) : MviViewModel<CollectionState, CollectionIntent, CollectionEffect>(CollectionState()) {

    init {
        observeCollection()
            .onEach { collection ->
                setState { copy(isLoading = false, collection = collection) }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: CollectionIntent) {
        when (intent) {
            is CollectionIntent.Select -> setState { copy(selectedItemId = intent.itemId) }
            CollectionIntent.DismissSelection -> setState { copy(selectedItemId = null) }
        }
    }
}
