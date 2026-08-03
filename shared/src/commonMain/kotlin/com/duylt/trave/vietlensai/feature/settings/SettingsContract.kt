package com.duylt.trave.vietlensai.feature.settings

import com.duylt.trave.vietlensai.core.mvi.UiEffect
import com.duylt.trave.vietlensai.core.mvi.UiIntent
import com.duylt.trave.vietlensai.core.mvi.UiState
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.AppSettings
import com.duylt.trave.vietlensai.domain.model.GeminiModel
import com.duylt.trave.vietlensai.domain.model.ThemePreference
import com.duylt.trave.vietlensai.domain.util.AppError

/**
 * No `error` field: nothing on this page draws a failure, so a failure is entirely
 * [SettingsEffect.ShowMessage]. It used to be held here as well, on the belief that the route
 * had to resolve the message from state — which is what silently swallowed the first failed
 * write, because the effect is handled before the recomposition that would read it.
 */
data class SettingsState(
    val settings: AppSettings = AppSettings.DEFAULT,
    val apiKeyDraft: String = "",
    val hasUsableKey: Boolean = false,
    val showClearConfirm: Boolean = false,
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

    /**
     * A write that did not land.
     *
     * @param error carries the failure itself, not just the fact of one — the route turns it
     *   into words with `userMessage()` when it handles this, so the message never depends on
     *   a recomposition having already happened.
     */
    data class ShowMessage(val error: AppError) : SettingsEffect
}
