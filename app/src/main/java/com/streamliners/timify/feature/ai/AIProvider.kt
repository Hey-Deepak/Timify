package com.streamliners.timify.feature.ai

/**
 * Abstraction for AI/LLM providers.
 * Supports both Claude (Anthropic) and Gemini (Google) via Koog framework.
 */
interface AIProvider {

    /**
     * Send a message with full conversation history and get a response.
     * @param systemPrompt The system instruction for the AI
     * @param messages The conversation history (user + assistant messages)
     * @param userMessage The new user message to send
     * @return The AI's response text
     */
    suspend fun chat(
        systemPrompt: String,
        messages: List<ChatMessage>,
        userMessage: String
    ): String
}

/**
 * Available AI provider types that the user can choose from.
 */
enum class AIProviderType {
    CLAUDE,
    GEMINI
}
