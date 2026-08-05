package com.evora.technologies.saola.domain.repository

import com.evora.technologies.saola.domain.model.ChatMessage
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * The follow-up conversation grounded on a single [com.evora.technologies.saola.domain.model.Discovery].
 *
 * Context is rebuilt from stored history on every turn rather than held in memory,
 * so closing the app mid-conversation and coming back the next morning still lets
 * the traveller ask "and who built the one you mentioned?".
 */
interface ChatRepository {

    fun observeMessages(discoveryId: String): Flow<List<ChatMessage>>

    /**
     * Persists the question, asks Gemini with the discovery and prior turns as
     * context, then persists and returns the answer.
     */
    suspend fun ask(discoveryId: String, question: String): AppResult<ChatMessage>

    suspend fun clearThread(discoveryId: String): AppResult<Unit>
}
