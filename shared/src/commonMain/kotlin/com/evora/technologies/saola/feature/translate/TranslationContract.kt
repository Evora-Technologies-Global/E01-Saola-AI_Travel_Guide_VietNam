package com.evora.technologies.saola.feature.translate

import com.evora.technologies.saola.core.mvi.UiEffect
import com.evora.technologies.saola.core.mvi.UiIntent
import com.evora.technologies.saola.core.mvi.UiState
import com.evora.technologies.saola.domain.model.AppLanguage
import com.evora.technologies.saola.domain.model.TranslateLanguage
import com.evora.technologies.saola.domain.model.TranslationResult
import com.evora.technologies.saola.domain.util.AppError

data class TranslationState(
    /** The capture itself, on screen from the first frame while the rest is fetched. */
    val imagePath: String = "",
    val isLoading: Boolean = true,
    val translation: TranslationResult? = null,
    val error: AppError? = null,
    val sourceLanguage: TranslateLanguage? = null,
    val targetLanguage: TranslateLanguage = TranslateLanguage.ENGLISH,
    /** Which block's detail sheet is open, as an index into [TranslationResult.blocks]. */
    val selectedBlock: Int? = null,
    /** While true the overlay steps aside so the untouched photo can be read. */
    val showingOriginal: Boolean = false,
    /** Leaving mid-translation throws the work away, so it is asked about first. */
    val confirmingExit: Boolean = false,
    val language: AppLanguage = AppLanguage.VIETNAMESE,
    val isSpeaking: Boolean = false,
) : UiState

sealed interface TranslationIntent : UiIntent {
    data object ToggleSpeech : TranslationIntent
    data object Retry : TranslationIntent
    data class SelectBlock(val index: Int) : TranslationIntent
    data object DismissBlock : TranslationIntent
    data class ShowOriginal(val showing: Boolean) : TranslationIntent

    /**
     * The traveller wants out — by the arrow or by the system's own back.
     *
     * One intent for both because they mean the same thing, and because whether
     * leaving is free or costs an unfinished translation is not something the two
     * call sites should each have to work out.
     */
    data object BackPressed : TranslationIntent
    data object ConfirmExit : TranslationIntent
    data object DismissExitPrompt : TranslationIntent
}

sealed interface TranslationEffect : UiEffect {
    /** Leave the screen. Raised here rather than decided at the button. */
    data object Close : TranslationEffect
}
