package com.streamliners.timify.feature.chat

import androidx.compose.runtime.mutableStateOf
import com.streamliners.base.BaseViewModel
import com.streamliners.base.exception.log
import com.streamliners.base.ext.execute
import com.streamliners.base.ext.executeOnMain
import com.streamliners.base.ext.hideLoader
import com.streamliners.base.ext.showLoader
import com.streamliners.base.taskState.taskStateOf
import com.streamliners.base.taskState.update
import com.streamliners.timify.BuildConfig
import com.streamliners.timify.android.helper.TTSHelper
import com.streamliners.timify.data.local.dao.ChatHistoryDao
import com.streamliners.timify.data.local.dao.TaskInfoDao
import com.streamliners.timify.domain.model.ChatHistoryItem
import com.streamliners.timify.domain.model.TaskInfo
import com.streamliners.timify.feature.ai.AIProvider
import com.streamliners.timify.feature.ai.ChatMessage
import com.streamliners.timify.feature.ai.SystemPrompts
import com.streamliners.timify.feature.chat.ChatViewModel.ChatType.Insights
import com.streamliners.timify.feature.chat.ChatViewModel.ChatType.Normal
import com.streamliners.timify.feature.chat.viewModelExt.insightResponseFor
import com.streamliners.timify.feature.chat.viewModelExt.toChatMessages
import com.streamliners.timify.feature.chat.viewModelExt.toUIItems
import com.streamliners.timify.other.ext.calculateTimeDiffInMins
import com.streamliners.utils.DateTimeUtils
import com.streamliners.utils.DateTimeUtils.formatTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatHistoryDao: ChatHistoryDao,
    val taskInfoDao: TaskInfoDao,
    val ttsHelper: TTSHelper,
    private val aiProvider: AIProvider
) : BaseViewModel() {

    data class ChatHistoryUIItem(
        val message: String,
        val time: Long,
        val formattedTime: String,
        val date: String,
        val role: Role
    ) {
        enum class Role { User, Model }
    }

    class Data(
        val chatHistoryUIItems: List<ChatHistoryUIItem>
    )

    enum class ChatType { Normal, Insights }

    enum class Mode { Text, Voice }

    val type = mutableStateOf(Normal)
    val mode = mutableStateOf(Mode.Text)
    val data = taskStateOf<Data>()

    private val currentDate = formatTime(DateTimeUtils.Format("yyyy/MM/dd"))

    var isNewChatHappened = mutableStateOf(true)

    private var collectJob: Job? = null

    // In-memory conversation history for the current session
    private var conversationHistory: List<ChatMessage> = emptyList()

    fun loadChat() {
        execute(false) {
            collectJob?.cancel()

            collectJob = launch {
                var isFirstFetch = true
                chatHistoryDao.getList(currentDate, type.value.name).collectLatest { chatHistory ->
                    if (isFirstFetch) {
                        isFirstFetch = false
                        // Load conversation history from database
                        conversationHistory = if (type.value == Normal) {
                            chatHistory.toChatMessages()
                        } else {
                            emptyList()
                        }
                    }
                    data.update(
                        Data(chatHistory.toUIItems())
                    )
                }
            }
        }
    }

    fun sendPrompt(
        prompt: String,
        onSuccess: () -> Unit,
        takeNextVoicePrompt: () -> Unit
    ) {
        execute(false) {
            showLoader()

            isNewChatHappened.value = true

            val systemPrompt = when (type.value) {
                Normal -> SystemPrompts.NORMAL_CHAT
                Insights -> SystemPrompts.INSIGHTS_CHAT
            }

            var response = aiProvider.chat(
                systemPrompt = systemPrompt,
                messages = conversationHistory,
                userMessage = prompt
            )

            if (type.value == Insights) {
                response = insightResponseFor(response)
            }

            // Update in-memory conversation history
            conversationHistory = conversationHistory + listOf(
                ChatMessage(ChatMessage.Role.USER, prompt),
                ChatMessage(ChatMessage.Role.ASSISTANT, response)
            )

            chatHistoryDao.add(
                ChatHistoryItem(
                    type = type.value.name,
                    role = "user",
                    message = prompt
                )
            )

            chatHistoryDao.add(
                ChatHistoryItem(
                    type = type.value.name,
                    role = "model",
                    message = response
                )
            )

            hideLoader()
            onSuccess()

            if (mode.value == Mode.Voice) {
                ttsHelper.speak(response)
                takeNextVoicePrompt()
            }
        }
    }

    fun saveTaskInfoToLocal(onSuccess: () -> Unit) {
        execute {
            val rowsCount = chatHistoryDao.getTotalRowCount()
            if (!isNewChatHappened.value || rowsCount == 0) {
                executeOnMain { onSuccess() }
                return@execute
            }

            val systemPrompt = SystemPrompts.NORMAL_CHAT

            val response = aiProvider.chat(
                systemPrompt = systemPrompt,
                messages = conversationHistory,
                userMessage = "give data in csv"
            )

            val lines = response.split("\n").dropLast(1)

            log(
                "response from model : '$response'",
                "pieChartDebug",
                buildType = BuildConfig.BUILD_TYPE
            )

            taskInfoDao.clearAllOf(currentDate)

            lines.forEach { line ->
                val data = line.split(",")

                taskInfoDao.add(
                    TaskInfo(
                        name = data[0],
                        startTime = data[1],
                        endTime = data[2],
                        date = currentDate,
                        durationInMins = calculateTimeDiffInMins(data[1], data[2])
                    )
                )
            }

            executeOnMain { onSuccess() }
        }
    }

}
