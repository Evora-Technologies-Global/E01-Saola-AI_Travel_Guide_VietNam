package com.evora.technologies.saola.feature.sovereignty

import com.evora.technologies.saola.core.mvi.MviViewModel
import com.evora.technologies.saola.core.mvi.UiEffect
import com.evora.technologies.saola.core.mvi.UiIntent
import com.evora.technologies.saola.core.mvi.UiState

data class SovereigntyState(
    /**
     * The asset has not been answered for yet, either way.
     *
     * Its own flag rather than `map == null`, which the old `StateFlow<RegionMap?>` could not
     * tell apart from the failure below it: still reading and read-but-unparseable are the
     * same null. Nothing renders it today — the card draws its placeholder from the null map
     * in both cases — and it is here because the distinction is real, not because a spinner
     * is planned.
     */
    val isLoading: Boolean = true,
    /** Null when the asset would not parse; the page draws its prose regardless. */
    val map: RegionMap? = null,
) : UiState

/**
 * Read-only screen — the traveller has nothing to send.
 *
 * Declared rather than removed because [MviViewModel] is typed on an intent, and a sealed
 * interface with no cases states exactly what is true.
 */
sealed interface SovereigntyIntent : UiIntent

/** Nothing to emit: leaving is the route's own back lambda, and nothing else happens here. */
sealed interface SovereigntyEffect : UiEffect
