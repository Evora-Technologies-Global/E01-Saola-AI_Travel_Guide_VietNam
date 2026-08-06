package com.evora.technologies.saola.feature.settings

import androidx.lifecycle.viewModelScope
import com.evora.technologies.saola.core.mvi.MviViewModel
import com.evora.technologies.saola.domain.repository.SettingsRepository
import com.evora.technologies.saola.domain.usecase.ClearHistoryUseCase
import com.evora.technologies.saola.domain.usecase.ObserveSettingsUseCase
import com.evora.technologies.saola.domain.usecase.UpdateThemeUseCase
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SettingsViewModel(
    observeSettings: ObserveSettingsUseCase,
    private val settingsRepository: SettingsRepository,
    private val updateTheme: UpdateThemeUseCase,
    private val clearHistory: ClearHistoryUseCase,
) : MviViewModel<SettingsState, SettingsIntent, SettingsEffect>(SettingsState()) {

    init {
        observeSettings()
            .onEach { settings -> setState { copy(settings = settings) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SelectTheme -> launchSafely { updateTheme(intent.preference) }

            is SettingsIntent.SetSpeakAnswers -> launchSafely {
                settingsRepository.setSpeakAnswers(intent.enabled)
            }

            SettingsIntent.RequestClearHistory -> setState { copy(showClearConfirm = true) }
            SettingsIntent.CancelClearHistory -> setState { copy(showClearConfirm = false) }

            // The confirmation is gated on the delete actually landing, the way saving a key
            // used to be — that card is gone, and this is now the one act on the page that
            // reports in words. Announced unconditionally it was the same lie with teeth: a
            // failed delete told the traveller their whole journal had been erased while
            // every photograph was still on the device.
            SettingsIntent.ConfirmClearHistory -> launchSafely {
                setState { copy(showClearConfirm = false) }
                when (val result = clearHistory()) {
                    is AppResult.Failure -> sendEffect(SettingsEffect.ShowMessage(result.error))
                    is AppResult.Success -> sendEffect(SettingsEffect.HistoryCleared)
                }
            }
        }
    }
}
