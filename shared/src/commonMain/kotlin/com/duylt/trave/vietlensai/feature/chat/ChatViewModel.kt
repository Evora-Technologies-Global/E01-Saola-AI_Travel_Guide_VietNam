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
import com.duylt.trave.vietlensai.voice.TextToSpeechManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ChatViewModel(
    savedStateHandle: SavedStateHandle,
    observeDiscovery: ObserveDiscoveryUseCase,
    observeChat: ObserveChatUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val askFollowUp: AskFollowUpUseCase,
    private val clearChat: ClearChatUseCase,
    private val textToSpeech: TextToSpeechManager,
) : MviViewModel<ChatState, ChatIntent, ChatEffect>(ChatState()) {

    private val discoveryId: String = checkNotNull(savedStateHandle[Routes.ARG_DISCOVERY_ID])

    init {
        observeDiscovery(discoveryId)
            .onEach { discovery -> setState { copy(discovery = discovery) } }
            .launchIn(viewModelScope)

        observeChat(discoveryId)
            .onEach { messages -> setState { copy(messages = messages) } }
            .launchIn(viewModelScope)

        observeSettings()
            .onEach { settings ->
                setState { copy(language = settings.language, speakAnswers = settings.speakAnswers) }
            }
            .launchIn(viewModelScope)

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

    private fun send(question: String) {
        launchSafely(onError = { setState { copy(error = it) } }) {
            setState { copy(isSending = true, input = "", error = null) }
            when (val result = askFollowUp(discoveryId, question)) {
                // The failure goes into state and nowhere else: ChatScreen draws the error
                // inline over the thread, so a one-shot message would say the same thing twice.
                is AppResult.Failure -> setState { copy(isSending = false, error = result.error) }

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
                }
            }
        }
    }

    override fun onCleared() {
        textToSpeech.stop()
        super.onCleared()
    }
}
