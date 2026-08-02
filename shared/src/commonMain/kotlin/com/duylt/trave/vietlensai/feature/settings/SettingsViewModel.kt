package com.duylt.trave.vietlensai.feature.settings

import androidx.lifecycle.viewModelScope
import com.duylt.trave.vietlensai.core.mvi.MviViewModel
import com.duylt.trave.vietlensai.core.mvi.UiEffect
import com.duylt.trave.vietlensai.core.mvi.UiIntent
import com.duylt.trave.vietlensai.core.mvi.UiState
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.AppSettings
import com.duylt.trave.vietlensai.domain.model.GeminiModel
import com.duylt.trave.vietlensai.domain.model.ThemePreference
import com.duylt.trave.vietlensai.domain.repository.SettingsRepository
import com.duylt.trave.vietlensai.domain.usecase.ClearHistoryUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveApiKeyAvailabilityUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveSettingsUseCase
import com.duylt.trave.vietlensai.domain.usecase.SaveApiKeyUseCase
import com.duylt.trave.vietlensai.domain.usecase.UpdateLanguageUseCase
import com.duylt.trave.vietlensai.domain.usecase.UpdateModelUseCase
import com.duylt.trave.vietlensai.domain.usecase.UpdateThemeUseCase
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SettingsState(
    val settings: AppSettings = AppSettings.DEFAULT,
    val apiKeyDraft: String = "",
    val hasUsableKey: Boolean = false,
    val showClearConfirm: Boolean = false,
    /**
     * Held only so the screen can resolve the message in the traveller's language —
     * `toUserMessage` is a composable and the effect collector is not. Same arrangement as
     * the explore and journal routes.
     */
    val error: AppError? = null,
) : UiState

sealed interface SettingsIntent : UiIntent {
    data class ApiKeyDraftChanged(val value: String) : SettingsIntent
    data object SaveApiKey : SettingsIntent
    data object ClearApiKey : SettingsIntent
    data class SelectLanguage(val language: AppLanguage) : SettingsIntent
    data class SelectModel(val model: GeminiModel) : SettingsIntent
    data class SelectTheme(val preference: ThemePreference) : SettingsIntent
    data class SetSpeakAnswers(val enabled: Boolean) : SettingsIntent
    data object RequestClearHistory : SettingsIntent
    data object CancelClearHistory : SettingsIntent
    data object ConfirmClearHistory : SettingsIntent
}

sealed interface SettingsEffect : UiEffect {
    data object ApiKeySaved : SettingsEffect
    data object HistoryCleared : SettingsEffect

    /** A write that did not land. Carries the error so the screen shows why, not just that. */
    data class ShowMessage(val error: AppError) : SettingsEffect
}

class SettingsViewModel(
    observeSettings: ObserveSettingsUseCase,
    observeApiKeyAvailability: ObserveApiKeyAvailabilityUseCase,
    private val settingsRepository: SettingsRepository,
    private val saveApiKey: SaveApiKeyUseCase,
    private val updateLanguage: UpdateLanguageUseCase,
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
                    is AppResult.Failure -> {
                        setState { copy(error = result.error) }
                        sendEffect(SettingsEffect.ShowMessage(result.error))
                    }
                    // Clearing the draft means the field never keeps a secret on screen
                    // after it has been stored.
                    is AppResult.Success -> {
                        setState { copy(apiKeyDraft = "", error = null) }
                        sendEffect(SettingsEffect.ApiKeySaved)
                    }
                }
            }

            SettingsIntent.ClearApiKey -> launchSafely {
                settingsRepository.setApiKey(null)
                setState { copy(apiKeyDraft = "") }
            }

            is SettingsIntent.SelectLanguage -> launchSafely {
                updateLanguage(intent.language)
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
