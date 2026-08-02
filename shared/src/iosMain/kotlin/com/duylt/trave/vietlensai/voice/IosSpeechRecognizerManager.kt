package com.duylt.trave.vietlensai.voice

import com.duylt.trave.vietlensai.core.util.log
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.setActive
import platform.Foundation.NSLocale
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus

/**
 * The Speech framework, driven from a live microphone tap.
 *
 * iOS has no equivalent of Android's `RecognizerIntent` — there is no system dictation UI to
 * hand off to — so the audio session, the engine tap and the recognition request are all set
 * up here. That is also why partial results come for free: `shouldReportPartialResults` is a
 * flag on the request rather than a separate callback.
 *
 * Authorisation is requested on first use. The traveller sees two prompts the first time
 * (speech recognition and the microphone), which is simply how the platform works.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
internal class IosSpeechRecognizerManager : SpeechRecognizerManager {

    private val _state = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)
    override val state: StateFlow<VoiceInputState> = _state.asStateFlow()

    private val audioEngine = AVAudioEngine()
    private var recognizer: SFSpeechRecognizer? = null
    private var request: SFSpeechAudioBufferRecognitionRequest? = null
    private var task: platform.Speech.SFSpeechRecognitionTask? = null

    override fun isAvailable(): Boolean = SFSpeechRecognizer().isAvailable()

    override fun startListening(language: AppLanguage) {
        SFSpeechRecognizer.requestAuthorization { status ->
            when (status) {
                SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized ->
                    begin(language)
                SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusDenied,
                SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusRestricted,
                -> _state.value = VoiceInputState.PermissionDenied
                else -> _state.value = VoiceInputState.Idle
            }
        }
    }

    private fun begin(language: AppLanguage) {
        val locale = NSLocale(localeIdentifier = language.bcp47)
        val speechRecognizer = SFSpeechRecognizer(locale) ?: run {
            _state.value = VoiceInputState.Unavailable
            return
        }
        if (!speechRecognizer.isAvailable()) {
            _state.value = VoiceInputState.Unavailable
            return
        }

        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryRecord, AVAudioSessionModeMeasurement, 0uL, null)
            session.setActive(true, null)

            val bufferRequest = SFSpeechAudioBufferRecognitionRequest().apply {
                // A question appearing word by word is the only feedback that the microphone
                // is actually hearing anything — a spinner for four seconds reads as a hang.
                shouldReportPartialResults = true
            }

            val inputNode = audioEngine.inputNode
            inputNode.installTapOnBus(
                bus = 0uL,
                bufferSize = 1024u,
                format = inputNode.outputFormatForBus(0uL),
            ) { buffer, _ ->
                buffer?.let { bufferRequest.appendAudioPCMBuffer(it) }
            }

            audioEngine.prepare()
            audioEngine.startAndReturnError(null)

            _state.value = VoiceInputState.Listening(partialText = "")

            task = speechRecognizer.recognitionTaskWithRequest(bufferRequest) { result, error ->
                when {
                    result != null -> {
                        val text = result.bestTranscription.formattedString
                        _state.value = if (result.isFinal()) {
                            teardown()
                            VoiceInputState.Result(text)
                        } else {
                            VoiceInputState.Listening(text)
                        }
                    }
                    error != null -> {
                        log.w { "Speech recognition error: ${error.localizedDescription}" }
                        teardown()
                        _state.value = VoiceInputState.Error
                    }
                }
            }

            recognizer = speechRecognizer
            request = bufferRequest
        } catch (e: Exception) {
            log.w(e) { "Could not start speech recognition" }
            teardown()
            _state.value = VoiceInputState.Error
        }
    }

    override fun stopListening() {
        // `endAudio` rather than cancelling: the traveller has stopped talking, and the last
        // partial should still be finalised into a result they can send.
        request?.endAudio()
        teardown()
        if (_state.value is VoiceInputState.Listening) _state.value = VoiceInputState.Idle
    }

    override fun consumeResult() {
        if (_state.value is VoiceInputState.Result) _state.value = VoiceInputState.Idle
    }

    private fun teardown() {
        if (audioEngine.isRunning()) {
            audioEngine.stop()
            audioEngine.inputNode.removeTapOnBus(0uL)
        }
        request = null
        task = null
        recognizer = null
        runCatching { AVAudioSession.sharedInstance().setActive(false, null) }
    }
}
