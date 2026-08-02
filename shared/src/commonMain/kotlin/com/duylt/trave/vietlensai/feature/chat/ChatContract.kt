package com.duylt.trave.vietlensai.feature.chat

import com.duylt.trave.vietlensai.core.mvi.UiEffect
import com.duylt.trave.vietlensai.core.mvi.UiIntent
import com.duylt.trave.vietlensai.core.mvi.UiState
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.ChatMessage
import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.util.AppError

data class ChatState(
    val discovery: Discovery? = null,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
    val isListening: Boolean = false,
    val language: AppLanguage = AppLanguage.VIETNAMESE,
    val speakAnswers: Boolean = true,
    val speakingMessageId: String? = null,
    val error: AppError? = null,
) : UiState {
    val canSend: Boolean get() = input.isNotBlank() && !isSending
    val suggestions: List<String>
        get() = if (messages.isEmpty()) discovery?.suggestedQuestions.orEmpty() else emptyList()
}

sealed interface ChatIntent : UiIntent {
    data class InputChanged(val text: String) : ChatIntent
    data class SubmitQuestion(val text: String? = null) : ChatIntent
    data object StartListening : ChatIntent
    data object StopListening : ChatIntent
    data class SpeakMessage(val messageId: String) : ChatIntent
    data object StopSpeaking : ChatIntent
    data object ClearThread : ChatIntent
    data object DismissError : ChatIntent
}

sealed interface ChatEffect : UiEffect {
    data object ScrollToBottom : ChatEffect
    data object RequestMicPermission : ChatEffect
    data class ShowMessage(val error: AppError) : ChatEffect
}
