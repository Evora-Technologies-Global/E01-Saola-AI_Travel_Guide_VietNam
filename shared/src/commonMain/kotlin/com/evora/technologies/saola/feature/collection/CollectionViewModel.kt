package com.evora.technologies.saola.feature.collection

import androidx.lifecycle.viewModelScope
import com.evora.technologies.saola.core.mvi.MviViewModel
import com.evora.technologies.saola.domain.usecase.ObserveCollectionUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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
