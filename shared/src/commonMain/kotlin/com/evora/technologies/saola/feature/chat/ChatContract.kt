package com.evora.technologies.saola.feature.chat

import com.evora.technologies.saola.core.mvi.UiEffect
import com.evora.technologies.saola.core.mvi.UiIntent
import com.evora.technologies.saola.core.mvi.UiState
import com.evora.technologies.saola.domain.model.AppLanguage
import com.evora.technologies.saola.domain.model.ChatMessage
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.domain.util.AppError

data class ChatState(
    val discovery: Discovery? = null,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
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
    data class SpeakMessage(val messageId: String) : ChatIntent
    data object StopSpeaking : ChatIntent
    data object ClearThread : ChatIntent
    data object DismissError : ChatIntent
}

/**
 * Deliberately empty: this screen has nothing one-shot to say.
 *
 * It is kept rather than replaced with `Nothing` only because `MviViewModel` is declared over
 * three types and a named one is what the other screens' contracts look like; the alternative
 * would change ChatViewModel's supertype to say something no other ViewModel says.
 *
 * The two members that used to live here both had a better home in state. A scroll effect was
 * never collected — ChatRoute scrolls off `messages.size` instead, so that the scroll happens
 * after the new row has actually been laid out, which an effect fired from the ViewModel
 * cannot guarantee. And a message effect only repeated [ChatState.error], which ChatScreen
 * already draws inline. Because ChatRoute collects no effects at all, every send parked a
 * coroutine once the channel's 64-slot buffer filled — until the nav entry cleared the
 * ViewModel and cancelled its scope, so the cost was a long conversation's worth of stuck
 * coroutines rather than a permanent leak.
 */
sealed interface ChatEffect : UiEffect
