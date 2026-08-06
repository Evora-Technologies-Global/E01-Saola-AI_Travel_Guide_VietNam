package com.evora.technologies.saola.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.evora.technologies.saola.core.util.log
import com.evora.technologies.saola.domain.model.AppLanguage
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reads descriptions and chat replies aloud.
 *
 * The engine is created once and kept for the process lifetime — TTS init costs
 * hundreds of milliseconds, and a guide that pauses before every sentence is worse
 * than no voice at all. Speech is best-effort throughout: a device without a
 * Vietnamese voice should still show the text rather than surface an error the
 * traveller can do nothing about.
 */
internal class AndroidTextToSpeechManager(
    private val context: Context,
) : TextToSpeechManager {

    private var engine: TextToSpeech? = null
    private var isReady = false
    private var pendingUtterance: PendingUtterance? = null

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentUtteranceId = MutableStateFlow<String?>(null)
    override val currentUtteranceId: StateFlow<String?> = _currentUtteranceId.asStateFlow()

    override fun initialise() {
        if (engine != null) return
        engine = TextToSpeech(context) { status ->
            isReady = status == TextToSpeech.SUCCESS
            if (!isReady) {
                log.w { "Text-to-speech unavailable (status $status)" }
                return@TextToSpeech
            }
            engine?.setOnUtteranceProgressListener(progressListener)
            // A speak() that arrived while the engine was still initialising is
            // replayed here rather than dropped — otherwise the very first tap on
            // "Listen" after a cold start silently does nothing.
            pendingUtterance?.let { pending ->
                pendingUtterance = null
                speak(pending.text, pending.bcp47, pending.utteranceId)
            }
        }
    }

    /** Reads narration in the language the app is speaking to the traveller in. */
    override fun speak(text: String, language: AppLanguage, utteranceId: String) =
        speak(text, language.bcp47, utteranceId)

    /**
     * Reads text in a named language, whatever the app's own language is.
     *
     * A tag rather than [AppLanguage] because the translator reaches past the two
     * languages the app speaks: a menu turned into Japanese has to be read by a
     * Japanese voice, and there is no [AppLanguage] to name it with.
     */
    override fun speak(text: String, bcp47: String, utteranceId: String) {
        if (text.isBlank()) return
        val tts = engine
        if (tts == null || !isReady) {
            pendingUtterance = PendingUtterance(text, bcp47, utteranceId)
            initialise()
            return
        }

        val locale = Locale.forLanguageTag(bcp47)
        when (tts.setLanguage(locale)) {
            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                log.w { "No installed voice for $bcp47, falling back to device default" }
            }
        }
        _currentUtteranceId.value = utteranceId
        // QUEUE_FLUSH: tapping "Listen" on a second card should replace the first,
        // not queue behind two minutes of narration.
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun stop() {
        engine?.stop()
        _isSpeaking.value = false
        _currentUtteranceId.value = null
    }

    override fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        isReady = false
        _isSpeaking.value = false
        _currentUtteranceId.value = null
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            _isSpeaking.value = true
            _currentUtteranceId.value = utteranceId
        }

        override fun onDone(utteranceId: String?) {
            _isSpeaking.value = false
            _currentUtteranceId.value = null
        }

        @Deprecated("Superseded by onError(String, Int)", ReplaceWith(""))
        override fun onError(utteranceId: String?) {
            _isSpeaking.value = false
            _currentUtteranceId.value = null
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            log.w { "Text-to-speech failed for $utteranceId (code $errorCode)" }
            _isSpeaking.value = false
            _currentUtteranceId.value = null
        }
    }

    private data class PendingUtterance(
        val text: String,
        val bcp47: String,
        val utteranceId: String,
    )

}
