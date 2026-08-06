package com.evora.technologies.saola.voice

import com.evora.technologies.saola.domain.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.cinterop.ObjCSignatureOverride
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.darwin.NSObject

/**
 * `AVSpeechSynthesizer`, which is the whole engine — there is nothing to initialise and no
 * asynchronous readiness to wait for, so [initialise] has nothing to do.
 *
 * That difference from Android is why the pending-utterance replay the Android manager needs
 * has no counterpart here: a `speak` issued a millisecond after launch is simply spoken.
 */
internal class IosTextToSpeechManager : TextToSpeechManager {

    private val synthesizer = AVSpeechSynthesizer()

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentUtteranceId = MutableStateFlow<String?>(null)
    override val currentUtteranceId: StateFlow<String?> = _currentUtteranceId.asStateFlow()

    /**
     * Which id belongs to the utterance in flight.
     *
     * `AVSpeechUtterance` carries no identifier of its own, so the mapping is kept here and
     * read back in the delegate callbacks — the same job Android's `utteranceId` extra does.
     */
    private var pendingId: String? = null

    private val delegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        @ObjCSignatureOverride
        override fun speechSynthesizer(
            synthesizer: AVSpeechSynthesizer,
            didStartSpeechUtterance: AVSpeechUtterance,
        ) {
            _isSpeaking.value = true
            _currentUtteranceId.value = pendingId
        }

        @ObjCSignatureOverride
        override fun speechSynthesizer(
            synthesizer: AVSpeechSynthesizer,
            didFinishSpeechUtterance: AVSpeechUtterance,
        ) {
            finish()
        }

        @ObjCSignatureOverride
        override fun speechSynthesizer(
            synthesizer: AVSpeechSynthesizer,
            didCancelSpeechUtterance: AVSpeechUtterance,
        ) {
            finish()
        }

        private fun finish() {
            _isSpeaking.value = false
            _currentUtteranceId.value = null
            pendingId = null
        }
    }

    init {
        synthesizer.delegate = delegate
    }

    override fun initialise() = Unit

    override fun speak(text: String, language: AppLanguage, utteranceId: String) =
        speak(text = text, bcp47 = language.bcp47, utteranceId = utteranceId)

    override fun speak(text: String, bcp47: String, utteranceId: String) {
        if (text.isBlank()) return
        stop()
        pendingId = utteranceId
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text).apply {
            // Null when the device has no voice for that tag; leaving it null lets iOS pick
            // the system default rather than refusing to speak.
            AVSpeechSynthesisVoice.voiceWithLanguage(bcp47)?.let { setVoice(it) }
        }
        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        if (synthesizer.isSpeaking()) {
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        }
    }

    /**
     * Nothing to release.
     *
     * Android holds a `TextToSpeech` engine that leaks a service connection if it is not shut
     * down; `AVSpeechSynthesizer` is a plain object the collector reclaims. Stopping mid-
     * sentence is still the right thing to do when the window goes away.
     */
    override fun shutdown() {
        stop()
    }
}
