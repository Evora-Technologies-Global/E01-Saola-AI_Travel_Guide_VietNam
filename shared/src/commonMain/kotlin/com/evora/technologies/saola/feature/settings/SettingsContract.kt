package com.evora.technologies.saola.feature.settings

import com.evora.technologies.saola.core.mvi.UiEffect
import com.evora.technologies.saola.core.mvi.UiIntent
import com.evora.technologies.saola.core.mvi.UiState
import com.evora.technologies.saola.domain.model.AppSettings
import com.evora.technologies.saola.domain.model.ThemePreference
import com.evora.technologies.saola.domain.util.AppError

/**
 * No `error` field: nothing on this page draws a failure, so a failure is entirely
 * [SettingsEffect.ShowMessage]. It used to be held here as well, on the belief that the route
 * had to resolve the message from state — which is what silently swallowed the first failed
 * write, because the effect is handled before the recomposition that would read it.
 *
 * Nothing about the Gemini key or the model is here either, since 06.08.2026: both are build
 * decisions now — `DataBuildConfig.GEMINI_API_KEY` and `GeminiModel.CONFIGURED` — and a screen
 * holds state for what it can change.
 */
data class SettingsState(
    val settings: AppSettings = AppSettings.DEFAULT,
    val showClearConfirm: Boolean = false,
) : UiState

sealed interface SettingsIntent : UiIntent {
    data class SelectTheme(val preference: ThemePreference) : SettingsIntent
    data class SetSpeakAnswers(val enabled: Boolean) : SettingsIntent
    data object RequestClearHistory : SettingsIntent
    data object CancelClearHistory : SettingsIntent
    data object ConfirmClearHistory : SettingsIntent
}

sealed interface SettingsEffect : UiEffect {
    data object HistoryCleared : SettingsEffect

    /**
     * A write that did not land.
     *
     * Raised by the one act on this page that says in words what it has done — clearing the
     * history — because [HistoryCleared] on a delete that failed is the same lie the API-key
     * card used to tell: a confirmation over unchanged data. The two toggles deliberately do
     * not raise it; they redraw from the settings flow and put themselves back on their own.
     *
     * @param error carries the failure itself, not just the fact of one — the route turns it
     *   into words with `userMessage()` when it handles this, so the message never depends on
     *   a recomposition having already happened.
     */
    data class ShowMessage(val error: AppError) : SettingsEffect
}
