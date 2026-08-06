package com.evora.technologies.saola.domain.model

import kotlin.time.Instant

/**
 * One turn in the conversation attached to a [Discovery].
 *
 * Threads are scoped per discovery so "who built this?" always resolves against
 * the right monument, and so history stays useful months after the trip.
 */
data class ChatMessage(
    val id: String,
    val discoveryId: String,
    val role: ChatRole,
    val content: String,
    val createdAt: Instant,
)

enum class ChatRole(val wireName: String) {
    USER("user"),
    ASSISTANT("model"),
    ;

    companion object {
        fun fromWire(value: String?): ChatRole =
            entries.firstOrNull { it.wireName.equals(value?.trim(), ignoreCase = true) } ?: ASSISTANT
    }
}
