package com.duylt.trave.vietlensai.voice

import com.duylt.trave.vietlensai.domain.model.AppLanguage
import kotlinx.coroutines.flow.StateFlow

/** What the microphone is doing, and what it heard. */
sealed interface VoiceInputState {
    data object Idle : VoiceInputState
    data class Listening(val partialText: String) : VoiceInputState
    data class Result(val text: String) : VoiceInputState
    data object PermissionDenied : VoiceInputState
    data object Unavailable : VoiceInputState
    data object Error : VoiceInputState
}

/**
 * Dictation for the chat box.
 *
 * Partial results are surfaced while the traveller is still talking, because a question
 * appearing word by word is the only feedback that the microphone is actually hearing
 * anything — a spinner for four seconds reads as a hang.
 *
 * An interface rather than an `expect class`: the Android recogniser needs a `Context` and
 * the iOS one needs nothing, and each implementation says so in its own constructor.
 */
interface SpeechRecognizerManager {

    val state: StateFlow<VoiceInputState>

    /** False when the device cannot do speech recognition at all; the button stays hidden. */
    fun isAvailable(): Boolean

    fun startListening(language: AppLanguage)

    fun stopListening()

    /** Acknowledges a [VoiceInputState.Result] so the next utterance starts from Idle. */
    fun consumeResult()
}
