package com.duylt.trave.vietlensai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duylt.trave.vietlensai.core.util.log
import com.duylt.trave.vietlensai.domain.model.AppSettings
import com.duylt.trave.vietlensai.domain.repository.DemoDataSeeder
import com.duylt.trave.vietlensai.domain.usecase.ObserveSettingsUseCase
import com.duylt.trave.vietlensai.domain.usecase.SweepOrphanCapturesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns the settings the whole window depends on — theme and dynamic colour —
 * and gates the splash screen until they have been read from disk.
 *
 * The one ViewModel in this project that is a plain [ViewModel] rather than an
 * [com.duylt.trave.vietlensai.core.mvi.MviViewModel], and deliberately so.
 * `MviViewModel` is for **screens**: something with a route and a back-stack entry, a user
 * who acts on it, and a state to render. This is the **window host** — no route, read by the
 * Android Activity and by `MainViewController` on iOS before any screen exists. An intent
 * channel with no sender and an effect channel with no collector would be ceremony, so the
 * two StateFlows above are the exception the rule is stated against rather than a deviation
 * from it. Anything reached by navigation extends `MviViewModel`, without exception.
 */
class MainViewModel(
    private val observeSettings: ObserveSettingsUseCase,
    private val sweepOrphanCaptures: SweepOrphanCapturesUseCase,
    private val seedDemoData: DemoDataSeeder,
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    val settings: StateFlow<AppSettings> = observeSettings()
        .onEach { _isReady.value = true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSettings.DEFAULT,
        )

    init {
        viewModelScope.launch {
            // Seeding first, and the order is load-bearing rather than tidy: the demo
            // photographs are written straight into the captures directory, so a sweep
            // running alongside would be free to see them before their rows exist and delete
            // the lot as orphans. On a release build this is the no-op binding, and there are
            // no assets to seed from either — see `seedModule` and `:app`'s `syncSeedAssets`.
            silently("Seeding the demo trip") { seedDemoData.seedIfEmpty() }

            // Launch is the one moment nothing is mid-capture, which is what makes an
            // unreferenced file safely readable as abandoned rather than in flight. Failing
            // is not worth surfacing: the only cost is storage, and the next launch retries.
            silently("Orphan capture sweep") { sweepOrphanCaptures() }
        }
    }

    /**
     * Runs startup work that nobody is waiting on, and that must never take the app down.
     *
     * Not `runCatching`: it catches Throwable, so it swallows the [CancellationException]
     * that ends this coroutine when the app is torn down mid-sweep — reporting an ordinary
     * shutdown as a failure, and completing normally instead of cancelling. The layer below
     * rethrows cancellation on purpose (StorageGuards, launchSafely); this is the same rule.
     *
     * Both callers promise not to throw. The guard is here anyway because they are the first
     * thing that runs on every launch and they share one coroutine: an unwrapped throw from
     * either would reach the platform's default handler, which on Android is the app dying
     * before the traveller has seen it do anything at all.
     */
    private suspend fun silently(what: String, work: suspend () -> Unit) {
        try {
            work()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            log.w(e) { "$what failed" }
        }
    }
}
