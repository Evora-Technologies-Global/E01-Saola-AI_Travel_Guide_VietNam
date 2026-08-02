package com.duylt.trave.vietlensai.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.duylt.trave.vietlensai.core.mvi.MviViewModel
import com.duylt.trave.vietlensai.domain.model.ChatRole
import com.duylt.trave.vietlensai.domain.usecase.AskFollowUpUseCase
import com.duylt.trave.vietlensai.domain.usecase.ClearChatUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveChatUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveDiscoveryUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveSettingsUseCase
import com.duylt.trave.vietlensai.domain.util.AppResult
import com.duylt.trave.vietlensai.navigation.Routes
import com.duylt.trave.vietlensai.voice.SpeechRecognizerManager
import com.duylt.trave.vietlensai.voice.TextToSpeechManager
import com.duylt.trave.vietlensai.voice.VoiceInputState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ChatViewModel(
    savedStateHandle: SavedStateHandle,
    observeDiscovery: ObserveDiscoveryUseCase,
    observeChat: ObserveChatUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val askFollowUp: AskFollowUpUseCase,
    private val clearChat: ClearChatUseCase,
    private val speechRecognizer: SpeechRecognizerManager,
    private val textToSpeech: TextToSpeechManager,
) : MviViewModel<ChatState, ChatIntent, ChatEffect>(ChatState()) {

    private val discoveryId: String = checkNotNull(savedStateHandle[Routes.ARG_DISCOVERY_ID])

    init {
        observeDiscovery(discoveryId)
            .onEach { discovery -> setState { copy(discovery = discovery) } }
            .launchIn(viewModelScope)

        observeChat(discoveryId)
            .onEach { messages ->
                setState { copy(messages = messages) }
                sendEffect(ChatEffect.ScrollToBottom)
            }
            .launchIn(viewModelScope)

        observeSettings()
            .onEach { settings ->
                setState { copy(language = settings.language, speakAnswers = settings.speakAnswers) }
            }
            .launchIn(viewModelScope)

        speechRecognizer.state.onEach(::handleVoiceState).launchIn(viewModelScope)

        textToSpeech.currentUtteranceId
            .onEach { id -> setState { copy(speakingMessageId = id) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.InputChanged -> setState { copy(input = intent.text) }

            is ChatIntent.SubmitQuestion -> {
                val question = intent.text ?: currentState.input
                if (question.isBlank() || currentState.isSending) return
                send(question)
            }

            ChatIntent.StartListening -> {
                if (!speechRecognizer.isAvailable()) {
                    setState { copy(isListening = false) }
                    return
                }
                sendEffect(ChatEffect.RequestMicPermission)
            }

            ChatIntent.StopListening -> {
                speechRecognizer.stopListening()
                setState { copy(isListening = false) }
            }

            is ChatIntent.SpeakMessage -> {
                val message = currentState.messages.firstOrNull { it.id == intent.messageId } ?: return
                textToSpeech.speak(message.content, currentState.language, message.id)
            }

            ChatIntent.StopSpeaking -> textToSpeech.stop()

            ChatIntent.ClearThread -> launchSafely(onError = { setState { copy(error = it) } }) {
                textToSpeech.stop()
                clearChat(discoveryId)
            }

            ChatIntent.DismissError -> setState { copy(error = null) }
        }
    }

    /** Called once the microphone permission has actually been granted. */
    fun onMicPermissionGranted() {
        speechRecognizer.startListening(currentState.language)
        setState { copy(isListening = true) }
    }

    private fun send(question: String) {
        launchSafely(onError = { setState { copy(error = it) } }) {
            setState { copy(isSending = true, input = "", error = null) }
            when (val result = askFollowUp(discoveryId, question)) {
                is AppResult.Failure -> {
                    setState { copy(isSending = false, error = result.error) }
                    sendEffect(ChatEffect.ShowMessage(result.error))
                }
                is AppResult.Success -> {
                    setState { copy(isSending = false) }
                    // Auto-speaking only the reply the user just waited for; older
                    // messages stay silent unless tapped.
                    if (currentState.speakAnswers && result.data.role == ChatRole.ASSISTANT) {
                        textToSpeech.speak(
                            result.data.content,
                            currentState.language,
                            result.data.id,
                        )
                    }
                    sendEffect(ChatEffect.ScrollToBottom)
                }
            }
        }
    }

    private fun handleVoiceState(voiceState: VoiceInputState) {
        when (voiceState) {
            is VoiceInputState.Listening ->
                setState { copy(isListening = true, input = voiceState.partialText) }

            is VoiceInputState.Result -> {
                setState { copy(isListening = false, input = voiceState.text) }
                speechRecognizer.consumeResult()
                // Sending straight away is what makes voice feel like talking to a
                // guide rather than dictating into a text box.
                send(voiceState.text)
            }

            VoiceInputState.Idle -> setState { copy(isListening = false) }

            VoiceInputState.PermissionDenied,
            VoiceInputState.Unavailable,
            VoiceInputState.Error,
            -> setState { copy(isListening = false) }
        }
    }

    override fun onCleared() {
        speechRecognizer.stopListening()
        textToSpeech.stop()
        super.onCleared()
    }
}
