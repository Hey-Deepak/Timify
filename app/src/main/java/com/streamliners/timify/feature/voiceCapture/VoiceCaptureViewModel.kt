package com.streamliners.timify.feature.voiceCapture

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.streamliners.base.BaseViewModel
import com.streamliners.base.ext.execute
import com.streamliners.timify.data.local.dao.TaskInfoDao
import com.streamliners.timify.domain.model.TaskInfo
import com.streamliners.timify.feature.ai.AIProvider
import com.streamliners.timify.feature.voice.sarvam.SarvamSTTService
import com.streamliners.timify.feature.voice.sarvam.SarvamTTSService
import com.streamliners.utils.DateTimeUtils
import com.streamliners.utils.DateTimeUtils.formatTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class VoiceCaptureViewModel(
    private val sttService: SarvamSTTService,
    private val ttsService: SarvamTTSService,
    private val aiProvider: AIProvider,
    private val taskInfoDao: TaskInfoDao
) : BaseViewModel() {

    enum class State {
        IDLE,
        LISTENING,
        PROCESSING,
        CONFIRMING,
        SAVED
    }

    val state = mutableStateOf(State.IDLE)
    val transcribedText = mutableStateOf("")
    val extractedTasks = mutableStateListOf<EditableTask>()

    private val todayDate = formatTime(DateTimeUtils.Format("yyyy/MM/dd"))

    private val json = Json { ignoreUnknownKeys = true }

    data class EditableTask(
        val name: String,
        val startTime: String,
        val endTime: String,
        val durationInMins: Int,
        val category: String
    )

    /**
     * After Sarvam STT transcribes user's Hindi voice,
     * send it to Claude to extract structured tasks.
     */
    fun processTranscription(text: String) {
        transcribedText.value = text
        state.value = State.PROCESSING

        execute(false) {
            val response = aiProvider.chat(
                systemPrompt = TASK_EXTRACTION_PROMPT,
                messages = emptyList(),
                userMessage = text
            )

            val tasks = parseTasksFromAIResponse(response)
            extractedTasks.clear()
            extractedTasks.addAll(tasks)
            state.value = State.CONFIRMING
        }
    }

    fun updateTask(index: Int, task: EditableTask) {
        if (index in extractedTasks.indices) {
            extractedTasks[index] = task
        }
    }

    fun removeTask(index: Int) {
        if (index in extractedTasks.indices) {
            extractedTasks.removeAt(index)
        }
    }

    fun confirmAndSave() {
        execute(false) {
            val tasks = extractedTasks.map { editable ->
                TaskInfo(
                    date = todayDate,
                    startTime = editable.startTime,
                    endTime = editable.endTime,
                    durationInMins = editable.durationInMins,
                    name = editable.name,
                    category = editable.category,
                    source = TaskInfo.SOURCE_VOICE
                )
            }
            taskInfoDao.addAll(tasks)
            state.value = State.SAVED

            // Speak confirmation in Hindi
            try {
                val count = tasks.size
                ttsService.speak("$count tasks saved successfully")
            } catch (_: Exception) { }
        }
    }

    fun reset() {
        state.value = State.IDLE
        transcribedText.value = ""
        extractedTasks.clear()
    }

    private fun parseTasksFromAIResponse(response: String): List<EditableTask> {
        return try {
            // Try to find JSON array in the response
            val jsonStart = response.indexOf('[')
            val jsonEnd = response.lastIndexOf(']')
            if (jsonStart == -1 || jsonEnd == -1) return fallbackParse(response)

            val jsonStr = response.substring(jsonStart, jsonEnd + 1)
            val parsed = json.decodeFromString<List<TaskJson>>(jsonStr)
            parsed.map { t ->
                EditableTask(
                    name = t.name,
                    startTime = t.startTime,
                    endTime = t.endTime,
                    durationInMins = t.durationInMins,
                    category = t.category
                )
            }
        } catch (e: Exception) {
            fallbackParse(response)
        }
    }

    private fun fallbackParse(response: String): List<EditableTask> {
        // Try CSV fallback: "startTime, endTime, name"
        return response.lines()
            .filter { it.contains(",") && it.trim().isNotEmpty() }
            .mapNotNull { line ->
                val parts = line.split(",").map { it.trim() }
                if (parts.size >= 3) {
                    EditableTask(
                        name = parts[2],
                        startTime = parts[0],
                        endTime = parts[1],
                        durationInMins = 30, // default estimate
                        category = if (parts.size > 3) parts[3] else ""
                    )
                } else null
            }
    }

    @Serializable
    private data class TaskJson(
        val name: String,
        val startTime: String,
        val endTime: String,
        val durationInMins: Int,
        val category: String = ""
    )

    companion object {
        val TASK_EXTRACTION_PROMPT = """
            You are a time tracking assistant. The user will describe their activities in Hindi (or mixed Hindi-English).

            Extract structured task data from their description. For each activity mentioned, identify:
            - name: The activity/task name (translate to English if in Hindi)
            - startTime: Start time in "h:mm a" format (e.g. "9:00 AM")
            - endTime: End time in "h:mm a" format
            - durationInMins: Duration in minutes (integer)
            - category: A short category like "Work", "Health", "Food", "Travel", "Rest", "Study", "Personal", "Entertainment"

            If the user doesn't specify exact times, estimate reasonable times based on context.
            If the user describes their morning routine, start from morning. If afternoon, start from after lunch.

            Respond with ONLY a JSON array, no other text. Example:
            [
              {"name": "Gym", "startTime": "6:00 AM", "endTime": "7:30 AM", "durationInMins": 90, "category": "Health"},
              {"name": "Breakfast", "startTime": "7:30 AM", "endTime": "8:00 AM", "durationInMins": 30, "category": "Food"}
            ]
        """.trimIndent()
    }
}
