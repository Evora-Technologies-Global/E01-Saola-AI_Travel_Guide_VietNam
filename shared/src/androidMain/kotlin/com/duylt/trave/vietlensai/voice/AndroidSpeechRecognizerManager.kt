package com.duylt.trave.vietlensai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.duylt.trave.vietlensai.core.util.log
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Voice input for follow-up questions.
 *
 * Partial results are surfaced as they arrive so the traveller can see they are
 * being heard — over a noisy street that feedback matters more than the final
 * transcript being a beat faster.
 *
 * The recogniser is created per session rather than held open: `SpeechRecognizer`
 * holds a live microphone binding, and keeping one alive across a whole screen
 * shows a persistent recording indicator on modern Android.
 */
internal class AndroidSpeechRecognizerManager(
    private val context: Context,
) : SpeechRecognizerManager {

    private var recognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)
    override val state: StateFlow<VoiceInputState> = _state.asStateFlow()

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override fun startListening(language: AppLanguage) {
        if (!isAvailable()) {
            _state.value = VoiceInputState.Unavailable
            return
        }
        stopListening()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.bcp47)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(listener)
            startListening(intent)
        }
        _state.value = VoiceInputState.Listening(partialText = "")
    }

    override fun stopListening() {
        recognizer?.run {
            stopListening()
            destroy()
        }
        recognizer = null
        if (_state.value is VoiceInputState.Listening) {
            _state.value = VoiceInputState.Idle
        }
    }

    override fun consumeResult() {
        _state.value = VoiceInputState.Idle
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = VoiceInputState.Listening(partialText = "")
        }

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.let { _state.value = VoiceInputState.Listening(partialText = it) }
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
            _state.value = if (text.isNullOrEmpty()) {
                VoiceInputState.Idle
            } else {
                VoiceInputState.Result(text)
            }
            recognizer?.destroy()
            recognizer = null
        }

        override fun onError(error: Int) {
            log.w { "Speech recognition error $error" }
            _state.value = when (error) {
                // Silence is not a failure worth interrupting anyone over.
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                -> VoiceInputState.Idle
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceInputState.PermissionDenied
                else -> VoiceInputState.Error
            }
            recognizer?.destroy()
            recognizer = null
        }

        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }
}

