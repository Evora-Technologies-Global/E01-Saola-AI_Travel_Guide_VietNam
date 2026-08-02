package com.duylt.trave.vietlensai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duylt.trave.vietlensai.core.util.log
import com.duylt.trave.vietlensai.domain.model.AppSettings
import com.duylt.trave.vietlensai.domain.usecase.ObserveSettingsUseCase
import com.duylt.trave.vietlensai.domain.usecase.SweepOrphanCapturesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns the settings the whole window depends on — theme and dynamic colour —
 * and gates the splash screen until they have been read from disk.
 */
class MainViewModel(
    private val observeSettings: ObserveSettingsUseCase,
    private val sweepOrphanCaptures: SweepOrphanCapturesUseCase,
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
        // Launch is the one moment nothing is mid-capture, which is what makes an
        // unreferenced file safely readable as abandoned rather than in flight. Failing
        // is not worth surfacing: the only cost is storage, and the next launch retries.
        viewModelScope.launch {
            runCatching { sweepOrphanCaptures() }
                .onFailure { log.w(it) { "Orphan capture sweep failed" } }
        }
    }
}
