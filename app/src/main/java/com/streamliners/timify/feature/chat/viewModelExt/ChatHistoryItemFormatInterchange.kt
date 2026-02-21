package com.streamliners.timify.feature.chat.viewModelExt

import com.streamliners.timify.domain.model.ChatHistoryItem
import com.streamliners.timify.feature.ai.ChatMessage
import com.streamliners.timify.feature.chat.ChatViewModel
import com.streamliners.utils.DateTimeUtils

/**
 * Convert database chat history items to ChatMessage objects
 * for the AI provider conversation history.
 */
fun List<ChatHistoryItem>.toChatMessages(): List<ChatMessage> {
    return map { item ->
        ChatMessage(
            role = if (item.role == "user") ChatMessage.Role.USER else ChatMessage.Role.ASSISTANT,
            content = item.message
        )
    }
}

/**
 * Convert database chat history items to UI items for display.
 */
fun List<ChatHistoryItem>.toUIItems(): List<ChatViewModel.ChatHistoryUIItem> {
    return map { item ->
        ChatViewModel.ChatHistoryUIItem(
            message = item.message,
            time = item.time,
            formattedTime = DateTimeUtils.formatTime(DateTimeUtils.Format.HOUR_MIN_12, item.time),
            date = DateTimeUtils.formatTime(DateTimeUtils.Format.DATE_MONTH_YEAR_1, item.time),
            role = if (item.role == "user") ChatViewModel.ChatHistoryUIItem.Role.User else ChatViewModel.ChatHistoryUIItem.Role.Model
        )
    }
}
