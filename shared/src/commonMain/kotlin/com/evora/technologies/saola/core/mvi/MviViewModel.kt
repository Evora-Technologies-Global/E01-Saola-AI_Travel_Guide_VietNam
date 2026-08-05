package com.evora.technologies.saola.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evora.technologies.saola.core.util.log
import com.evora.technologies.saola.domain.util.AppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything a screen renders, in one immutable snapshot. */
interface UiState

/** Something the user did. */
interface UiIntent

/**
 * A one-shot instruction to the UI: navigate, show a snackbar, speak a sentence.
 *
 * Deliberately not part of [UiState] — putting a "show snackbar" flag in state
 * means re-showing it on every configuration change and then needing a "consumed"
 * flag to undo that.
 */
interface UiEffect

/**
 * Base for every screen's ViewModel.
 *
 * Effects go through a [Channel] rather than a SharedFlow so they are buffered
 * while the screen is backgrounded and delivered exactly once when it returns —
 * a replaying hot flow would either drop a navigation event or fire it twice.
 */
abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(
    initialState: S,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = Channel<E>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    protected val currentState: S get() = _state.value

    /** The single entry point for user actions. */
    abstract fun onIntent(intent: I)

    protected fun setState(reducer: S.() -> S) {
        _state.update { it.reducer() }
    }

    protected fun sendEffect(effect: E) {
        viewModelScope.launch { _effects.send(effect) }
    }

    /**
     * Runs [block] in the ViewModel's scope with a floor under it.
     *
     * `viewModelScope` carries a `SupervisorJob` but no `CoroutineExceptionHandler`, so an
     * exception escaping a plain `viewModelScope.launch` goes to the platform's default
     * handler — which on Android is the process dying, mid-trip, with the traveller's
     * unsaved note in it. Every screen action in this app therefore goes through here
     * instead, and the worst case becomes a rendered error rather than a stack trace.
     *
     * This is a floor, not a licence. The layer below already promises not to throw: every
     * repository returns [AppResult] and folds its own failures into [AppError]. What this
     * catches is the gap between that promise and reality — a Room read that was never
     * wrapped, a `DataStore` file the OS could not open, a mapper meeting a column written
     * by a version that has since moved on. Those are the ones nobody writes a branch for,
     * because nobody expects them.
     *
     * [CancellationException] is deliberately rethrown. It is how a coroutine is *told to
     * stop* — a superseded recognition, a screen that has left — and swallowing it would
     * turn every ordinary cancellation into a spurious error banner and break structured
     * concurrency besides.
     *
     * @param onError what the screen shows. Defaults to logging alone, which is right for
     *   background work the traveller did not ask for and must not be interrupted by; pass
     *   a reducer for anything they are waiting on.
     */
    protected fun launchSafely(
        onError: (AppError) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit,
    ): Job = viewModelScope.launch {
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            log.e(throwable) { "Unhandled failure in ${this@MviViewModel::class.simpleName}" }
            onError(AppError.Unexpected(throwable.message))
        }
    }

}
