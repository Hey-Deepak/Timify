package com.streamliners.timify.feature.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.ext.agent.simpleSingleRunAgent
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.anthropic.simpleAnthropicExecutor

/**
 * Claude (Anthropic) AI provider using Koog framework.
 * Uses Claude Sonnet 4 as the default model for fast, high-quality responses.
 */
class ClaudeProvider(
    private val apiKey: String
) : AIProvider {

    private val executor by lazy { simpleAnthropicExecutor(apiKey) }

    override suspend fun chat(
        systemPrompt: String,
        messages: List<ChatMessage>,
        userMessage: String
    ): String {
        // Build the full prompt with conversation history
        val fullPrompt = buildPromptWithHistory(systemPrompt, messages, userMessage)

        val agent = AIAgent(
            promptExecutor = executor,
            llmModel = AnthropicModels.Sonnet_4_5
        )

        return agent.run(fullPrompt)
    }

    /**
     * Builds a single prompt string that includes the system instruction,
     * conversation history, and new user message.
     * This ensures the AI has full context for multi-turn conversations.
     */
    private fun buildPromptWithHistory(
        systemPrompt: String,
        messages: List<ChatMessage>,
        userMessage: String
    ): String {
        val sb = StringBuilder()

        sb.appendLine("System Instruction: $systemPrompt")
        sb.appendLine()

        if (messages.isNotEmpty()) {
            sb.appendLine("Previous conversation:")
            messages.forEach { msg ->
                val roleName = when (msg.role) {
                    ChatMessage.Role.USER -> "User"
                    ChatMessage.Role.ASSISTANT -> "Assistant"
                    ChatMessage.Role.SYSTEM -> "System"
                }
                sb.appendLine("$roleName: ${msg.content}")
            }
            sb.appendLine()
        }

        sb.appendLine("User: $userMessage")
        sb.appendLine()
        sb.appendLine("Respond as the Assistant based on the system instruction and conversation history above.")

        return sb.toString()
    }
}
