package com.streamliners.timify.feature.ai

/**
 * Platform-agnostic chat message used across the app.
 * Maps to the role/content format used by both Claude and Gemini APIs.
 */
data class ChatMessage(
    val role: Role,
    val content: String
) {
    enum class Role {
        SYSTEM,
        USER,
        ASSISTANT;

        fun toApiRole(): String = when (this) {
            SYSTEM -> "system"
            USER -> "user"
            ASSISTANT -> "assistant"
        }
    }
}
