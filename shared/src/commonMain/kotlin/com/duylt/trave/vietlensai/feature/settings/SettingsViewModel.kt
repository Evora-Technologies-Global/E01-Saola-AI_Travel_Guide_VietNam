package com.duylt.trave.vietlensai.feature.settings

import androidx.lifecycle.viewModelScope
import com.duylt.trave.vietlensai.core.mvi.MviViewModel
import com.duylt.trave.vietlensai.domain.repository.SettingsRepository
import com.duylt.trave.vietlensai.domain.usecase.ClearHistoryUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveApiKeyAvailabilityUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveSettingsUseCase
import com.duylt.trave.vietlensai.domain.usecase.SaveApiKeyUseCase
import com.duylt.trave.vietlensai.domain.usecase.UpdateModelUseCase
import com.duylt.trave.vietlensai.domain.usecase.UpdateThemeUseCase
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SettingsViewModel(
    observeSettings: ObserveSettingsUseCase,
    observeApiKeyAvailability: ObserveApiKeyAvailabilityUseCase,
    private val settingsRepository: SettingsRepository,
    private val saveApiKey: SaveApiKeyUseCase,
    private val updateModel: UpdateModelUseCase,
    private val updateTheme: UpdateThemeUseCase,
    private val clearHistory: ClearHistoryUseCase,
) : MviViewModel<SettingsState, SettingsIntent, SettingsEffect>(SettingsState()) {

    init {
        observeSettings()
            .onEach { settings -> setState { copy(settings = settings) } }
            .launchIn(viewModelScope)

        observeApiKeyAvailability()
            .onEach { usable -> setState { copy(hasUsableKey = usable) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ApiKeyDraftChanged -> setState { copy(apiKeyDraft = intent.value) }

            // Both the cleared field and the confirmation are gated on the write actually
            // landing. Unconditionally they were a lie with teeth: a failed write discarded
            // the key the traveller had just pasted, told them it was saved, and left the
            // status pill one row below still reading "no key configured".
            SettingsIntent.SaveApiKey -> launchSafely {
                when (val result = saveApiKey(currentState.apiKeyDraft)) {
                    is AppResult.Failure -> sendEffect(SettingsEffect.ShowMessage(result.error))
                    // Clearing the draft means the field never keeps a secret on screen
                    // after it has been stored.
                    is AppResult.Success -> {
                        setState { copy(apiKeyDraft = "") }
                        sendEffect(SettingsEffect.ApiKeySaved)
                    }
                }
            }

            SettingsIntent.ClearApiKey -> launchSafely {
                settingsRepository.setApiKey(null)
                setState { copy(apiKeyDraft = "") }
            }

            is SettingsIntent.SelectModel -> launchSafely { updateModel(intent.model) }

            is SettingsIntent.SelectTheme -> launchSafely { updateTheme(intent.preference) }

            is SettingsIntent.SetSpeakAnswers -> launchSafely {
                settingsRepository.setSpeakAnswers(intent.enabled)
            }

            SettingsIntent.RequestClearHistory -> setState { copy(showClearConfirm = true) }
            SettingsIntent.CancelClearHistory -> setState { copy(showClearConfirm = false) }

            SettingsIntent.ConfirmClearHistory -> launchSafely {
                setState { copy(showClearConfirm = false) }
                clearHistory()
                sendEffect(SettingsEffect.HistoryCleared)
            }
        }
    }
}
