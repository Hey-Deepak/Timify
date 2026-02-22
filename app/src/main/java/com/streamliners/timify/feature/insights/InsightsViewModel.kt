package com.streamliners.timify.feature.insights

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.streamliners.base.BaseViewModel
import com.streamliners.base.ext.execute
import com.streamliners.timify.data.local.dao.TaskInfoDao
import com.streamliners.timify.domain.model.TaskInfo
import com.streamliners.timify.feature.ai.AIProvider
import com.streamliners.utils.DateTimeUtils
import com.streamliners.utils.DateTimeUtils.formatTime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InsightsViewModel(
    private val taskInfoDao: TaskInfoDao,
    private val aiProvider: AIProvider
) : BaseViewModel() {

    data class Insight(
        val title: String,
        val content: String,
        val type: InsightType = InsightType.INFO
    )

    enum class InsightType { INFO, TIP, ALERT }

    val insights = mutableStateListOf<Insight>()
    val isLoading = mutableStateOf(false)
    val customQuestion = mutableStateOf("")

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    fun loadInsights() {
        if (insights.isNotEmpty()) return
        isLoading.value = true

        execute(false) {
            val allTasks = taskInfoDao.getAll()
            if (allTasks.isEmpty()) {
                insights.add(
                    Insight(
                        title = "No data yet",
                        content = "Start tracking your tasks to get AI-powered insights about your time usage.",
                        type = InsightType.INFO
                    )
                )
                isLoading.value = false
                return@execute
            }

            // Generate quick local insights
            generateLocalInsights(allTasks)

            // Generate AI insights
            generateAIInsights(allTasks)

            isLoading.value = false
        }
    }

    fun askQuestion(question: String) {
        if (question.isBlank()) return
        isLoading.value = true

        execute(false) {
            val allTasks = taskInfoDao.getAll()
            val taskSummary = buildTaskSummary(allTasks)

            val response = aiProvider.chat(
                systemPrompt = INSIGHTS_PROMPT,
                messages = emptyList(),
                userMessage = """
                    Here is the user's time tracking data:
                    $taskSummary

                    User's question: $question

                    Give a concise, helpful answer.
                """.trimIndent()
            )

            insights.add(
                0,
                Insight(
                    title = question,
                    content = response,
                    type = InsightType.INFO
                )
            )
            customQuestion.value = ""
            isLoading.value = false
        }
    }

    private fun generateLocalInsights(tasks: List<TaskInfo>) {
        val totalMins = tasks.sumOf { it.durationInMins }
        val totalDays = tasks.map { it.date }.distinct().size
        val avgPerDay = if (totalDays > 0) totalMins / totalDays else 0

        insights.add(
            Insight(
                title = "Tracking Summary",
                content = "You've tracked ${tasks.size} tasks over $totalDays days. " +
                        "Average: ${formatDuration(avgPerDay)} per day.",
                type = InsightType.INFO
            )
        )

        // Most tracked category
        val topCategory = tasks
            .groupBy { it.category.ifEmpty { it.name } }
            .maxByOrNull { (_, items) -> items.sumOf { it.durationInMins } }

        if (topCategory != null) {
            val mins = topCategory.value.sumOf { it.durationInMins }
            val pct = if (totalMins > 0) (mins * 100 / totalMins) else 0
            insights.add(
                Insight(
                    title = "Top Activity",
                    content = "${topCategory.key} takes up ${formatDuration(mins)} " +
                            "($pct% of your tracked time).",
                    type = InsightType.TIP
                )
            )
        }

        // Today vs average
        val todayDate = formatTime(DateTimeUtils.Format("yyyy/MM/dd"))
        val todayTasks = tasks.filter { it.date == todayDate }
        val todayMins = todayTasks.sumOf { it.durationInMins }
        if (todayMins > 0 && avgPerDay > 0) {
            val diff = todayMins - avgPerDay
            val comparison = when {
                diff > 30 -> "You've tracked ${formatDuration(diff)} more than your daily average today."
                diff < -30 -> "You've tracked ${formatDuration(-diff)} less than your daily average today."
                else -> "Today's tracking is close to your daily average."
            }
            insights.add(
                Insight(
                    title = "Today vs Average",
                    content = comparison,
                    type = if (diff < -30) InsightType.ALERT else InsightType.INFO
                )
            )
        }
    }

    private suspend fun generateAIInsights(tasks: List<TaskInfo>) {
        try {
            val taskSummary = buildTaskSummary(tasks)
            val response = aiProvider.chat(
                systemPrompt = INSIGHTS_PROMPT,
                messages = emptyList(),
                userMessage = """
                    Analyze this time tracking data and give 2-3 actionable insights:
                    $taskSummary
                """.trimIndent()
            )

            insights.add(
                Insight(
                    title = "AI Analysis",
                    content = response,
                    type = InsightType.TIP
                )
            )
        } catch (_: Exception) {
            // AI insights are optional, don't fail the whole screen
        }
    }

    private fun buildTaskSummary(tasks: List<TaskInfo>): String {
        val sb = StringBuilder()
        tasks.groupBy { it.date }.forEach { (date, dayTasks) ->
            sb.appendLine("Date: $date")
            dayTasks.forEach { t ->
                sb.appendLine("  ${t.startTime}-${t.endTime}: ${t.name} (${t.durationInMins}m, ${t.category})")
            }
        }
        return sb.toString()
    }

    private fun formatDuration(mins: Int): String {
        val hours = mins / 60
        val minutes = mins % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    companion object {
        val INSIGHTS_PROMPT = """
            You are a time management coach. Analyze the user's time tracking data
            and provide concise, actionable insights. Focus on:
            - Patterns in how they spend time
            - Areas where they could improve
            - Positive habits to reinforce
            Keep responses brief (2-4 sentences per insight). Be encouraging but honest.
        """.trimIndent()
    }
}
