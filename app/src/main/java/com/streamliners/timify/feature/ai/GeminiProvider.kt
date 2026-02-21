package com.streamliners.timify.feature.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig

/**
 * Gemini (Google) AI provider using the Google Generative AI SDK.
 * Kept as a fallback/alternative to Claude.
 */
class GeminiProvider(
    private val apiKey: String
) : AIProvider {

    override suspend fun chat(
        systemPrompt: String,
        messages: List<ChatMessage>,
        userMessage: String
    ): String {
        val model = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 1f
                topK = 64
                topP = 0.95f
                maxOutputTokens = 8192
                responseMimeType = "text/plain"
            },
            systemInstruction = content { text(systemPrompt) }
        )

        // Build history from previous messages
        val history = messages.map { msg ->
            content(role = if (msg.role == ChatMessage.Role.USER) "user" else "model") {
                text(msg.content)
            }
        }

        val chat = model.startChat(history)
        val response = chat.sendMessage(userMessage)
        return response.text ?: error("Unable to get response from Gemini API")
    }
}
