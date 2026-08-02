package com.duylt.trave.vietlensai.feature.translate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.duylt.trave.vietlensai.core.mvi.MviViewModel
import com.duylt.trave.vietlensai.core.mvi.UiEffect
import com.duylt.trave.vietlensai.core.mvi.UiIntent
import com.duylt.trave.vietlensai.core.mvi.UiState
import com.duylt.trave.vietlensai.core.util.DETECT_TIMEOUT_MILLIS
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.TranslateLanguage
import com.duylt.trave.vietlensai.domain.model.TranslationResult
import com.duylt.trave.vietlensai.domain.usecase.ObserveSettingsUseCase
import com.duylt.trave.vietlensai.domain.usecase.TranslateImageUseCase
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult
import com.duylt.trave.vietlensai.navigation.Routes
import com.duylt.trave.vietlensai.voice.TextToSpeechManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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

/**
 * Owns one photo's translation, from the moment the shutter released.
 *
 * The request lives here rather than behind the camera because this is the screen
 * with something to show while it waits — the traveller's own photo. Opening on it
 * and filling the words in as they arrive is the difference between a wait and a
 * blocked screen.
 */
class TranslationViewModel(
    savedStateHandle: SavedStateHandle,
    private val translateImage: TranslateImageUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val textToSpeech: TextToSpeechManager,
) : MviViewModel<TranslationState, TranslationIntent, TranslationEffect>(
    // The arguments *are* the initial state rather than something copied into it a
    // frame later: the photo has to be on screen in the first composition, and a
    // `setState { copy(imagePath = imagePath) }` in `init` silently reads the
    // state's own property instead of the field beside it.
    TranslationState(
        imagePath = savedStateHandle[Routes.ARG_IMAGE_PATH] ?: "",
        sourceLanguage = TranslateLanguage.fromCode(savedStateHandle[Routes.ARG_SOURCE_LANGUAGE]),
        targetLanguage = TranslateLanguage.fromCode(savedStateHandle[Routes.ARG_TARGET_LANGUAGE])
            ?: TranslateLanguage.ENGLISH,
    ),
) {

    private var translateJob: Job? = null

    init {
        observeSettings()
            .onEach { settings -> setState { copy(language = settings.language) } }
            .launchIn(viewModelScope)

        textToSpeech.isSpeaking
            .onEach { speaking -> setState { copy(isSpeaking = speaking) } }
            .launchIn(viewModelScope)

        translate()
    }

    override fun onIntent(intent: TranslationIntent) {
        when (intent) {
            is TranslationIntent.Retry -> translate()
            is TranslationIntent.SelectBlock -> setState { copy(selectedBlock = intent.index) }
            is TranslationIntent.DismissBlock -> setState { copy(selectedBlock = null) }
            is TranslationIntent.ShowOriginal -> setState { copy(showingOriginal = intent.showing) }
            is TranslationIntent.ToggleSpeech -> toggleSpeech()
            is TranslationIntent.BackPressed -> {
                // Only worth a question while there is something to lose: once the
                // translation has landed it is on screen and saved, and asking then
                // would be a dialog between the traveller and the way out.
                if (currentState.isLoading) {
                    setState { copy(confirmingExit = true) }
                } else {
                    sendEffect(TranslationEffect.Close)
                }
            }
            is TranslationIntent.ConfirmExit -> {
                // Cancelled here rather than left to the ViewModel's own death: the
                // pop takes a frame or two, and a request that finishes inside it
                // would write a translation for a screen nobody is looking at.
                translateJob?.cancel()
                setState { copy(confirmingExit = false) }
                sendEffect(TranslationEffect.Close)
            }
            is TranslationIntent.DismissExitPrompt -> setState { copy(confirmingExit = false) }
        }
    }

    private fun translate() {
        val photo = currentState.imagePath
        if (photo.isEmpty()) {
            setState { copy(isLoading = false, error = AppError.ImageUnavailable(null)) }
            return
        }
        // Retry replaces rather than queues: pressing it again means "that one,
        // now", not "both of them".
        translateJob?.cancel()
        translateJob = launchSafely(onError = { setState { copy(error = it) } }) {
            setState { copy(isLoading = true, error = null) }
            val from = currentState.sourceLanguage
            val to = currentState.targetLanguage

            // The same ceiling the camera puts on a recognition, for the same
            // reason: reading a sign is one press of the same shutter, so a wait
            // that would be unacceptable there is unacceptable here. The photo
            // stays on screen throughout, which softens the wait but does not make
            // an unbounded one any more useful than "try again".
            val result = withTimeoutOrNull(DETECT_TIMEOUT_MILLIS) {
                translateImage(photo, from, to)
            }

            when (result) {
                null -> setState { copy(isLoading = false, error = AppError.Timeout) }
                is AppResult.Failure -> setState { copy(isLoading = false, error = result.error) }
                is AppResult.Success -> setState {
                    copy(isLoading = false, translation = result.data, error = null)
                }
            }
        }
    }

    private fun toggleSpeech() {
        val translation = currentState.translation ?: return
        if (currentState.isSpeaking) {
            textToSpeech.stop()
            return
        }
        // The translation, not the original: someone reading a Vietnamese menu
        // wants to hear it in the language they know. Voiced in the language it was
        // translated *into* rather than the app's own — those parted company the
        // moment the traveller could pick a target from the camera.
        textToSpeech.speak(
            text = translation.translatedText,
            bcp47 = currentState.targetLanguage.bcp47,
            utteranceId = translation.id,
        )
    }

    override fun onCleared() {
        val spokenId = currentState.translation?.id
        if (spokenId != null && textToSpeech.currentUtteranceId.value == spokenId) {
            textToSpeech.stop()
        }
        super.onCleared()
    }
}
