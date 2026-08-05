package com.evora.technologies.saola.core.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Collects one-shot effects for a screen, once, while it is at least STARTED.
 *
 * Written here rather than at each screen because the same block was hand-rolled six times
 * and no two were the same: three used `collectLatest`, none was lifecycle-aware, and two
 * screens forgot the block entirely — which is the failure mode this helper exists to make
 * impossible, since an uncollected [kotlinx.coroutines.channels.Channel] fills at 64 and
 * every `send` after that suspends forever inside `viewModelScope`.
 *
 * `collect` rather than `collectLatest`: an effect is an instruction that has to be carried
 * out, and cancelling the handling of one the moment the next arrives leaves it half done —
 * a navigation begun and abandoned, a snackbar that never shows.
 *
 * `repeatOnLifecycle` rather than a bare `LaunchedEffect`: a destination that is stopped but
 * still composed must not act on effects. Nothing is lost — [MviViewModel] buffers them in a
 * Channel and delivers them when the screen returns.
 *
 * Work that must outlive the collector — a capture already in flight — belongs in a
 * `rememberCoroutineScope()` launched from inside [onEffect], not in the collector itself,
 * which now dies at `ON_STOP`.
 */
@Composable
fun <E : UiEffect> CollectEffects(
    effects: Flow<E>,
    onEffect: suspend (E) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    // The lambda is rebuilt on every recomposition and captures the callers' latest
    // navigation lambdas; without this the collector would keep handling effects with the
    // ones it was started with.
    val handler by rememberUpdatedState(onEffect)
    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            effects.collect { handler(it) }
        }
    }
}
