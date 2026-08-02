package com.duylt.trave.vietlensai.voice

import com.duylt.trave.vietlensai.domain.model.AppLanguage
import kotlinx.coroutines.flow.StateFlow

/**
 * Reads a narration aloud.
 *
 * The engine is created once and kept for the process lifetime on both platforms — TTS init
 * costs the better part of a second, and paying that on the first tap of the speak button
 * makes the feature feel broken.
 *
 * An interface rather than an `expect class`: the Android engine needs a `Context` and the
 * iOS one needs nothing, and each implementation says so in its own constructor.
 */
interface TextToSpeechManager {

    val isSpeaking: StateFlow<Boolean>

    /** Which utterance is being read, so a list can highlight the row that is speaking. */
    val currentUtteranceId: StateFlow<String?>

    /** Warms the engine up. Safe to call more than once. */
    fun initialise()

    fun speak(text: String, language: AppLanguage, utteranceId: String = DEFAULT_UTTERANCE_ID)

    /** For text in a language other than the app's — a translated sign, say. */
    fun speak(text: String, bcp47: String, utteranceId: String = DEFAULT_UTTERANCE_ID)

    fun stop()

    fun shutdown()

    companion object {
        /** Kept as it was before the split: utterance ids reach the platform engines verbatim. */
        const val DEFAULT_UTTERANCE_ID = "vietlens"
    }
}
