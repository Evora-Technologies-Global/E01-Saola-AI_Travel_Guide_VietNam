package com.duylt.trave.vietlensai.domain.usecase

import com.duylt.trave.vietlensai.domain.model.ChatMessage
import com.duylt.trave.vietlensai.domain.repository.ChatRepository
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.flow.Flow

class ObserveChatUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(discoveryId: String): Flow<List<ChatMessage>> =
        repository.observeMessages(discoveryId)
}

/**
 * Asks a follow-up question about a discovery.
 *
 * Rejects blank input here rather than in the ViewModel so every entry point —
 * typed text, a suggested-question chip, or a voice transcript that came back
 * empty — is guarded by the same rule.
 */
class AskFollowUpUseCase(
    private val repository: ChatRepository,
) {
    suspend operator fun invoke(discoveryId: String, question: String): AppResult<ChatMessage> {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) {
            return AppResult.Failure(AppError.Unexpected("Empty question"))
        }
        return repository.ask(discoveryId = discoveryId, question = trimmed)
    }
}

class ClearChatUseCase(
    private val repository: ChatRepository,
) {
    suspend operator fun invoke(discoveryId: String): AppResult<Unit> =
        repository.clearThread(discoveryId)
}
