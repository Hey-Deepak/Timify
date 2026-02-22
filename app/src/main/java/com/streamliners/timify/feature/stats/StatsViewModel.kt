package com.streamliners.timify.feature.stats

import androidx.compose.runtime.mutableStateOf
import com.streamliners.base.BaseViewModel
import com.streamliners.base.ext.execute
import com.streamliners.timify.data.local.dao.TaskInfoDao
import com.streamliners.timify.domain.model.TaskInfo
import com.streamliners.utils.DateTimeUtils
import com.streamliners.utils.DateTimeUtils.formatTime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatsViewModel(
    private val taskInfoDao: TaskInfoDao
) : BaseViewModel() {

    enum class Period { DAY, WEEK, MONTH }

    data class StatsData(
        val period: Period,
        val dateLabel: String,
        val totalMinutes: Int,
        val taskCount: Int,
        val categoryBreakdown: Map<String, Int>, // category -> total mins
        val dailyBreakdown: Map<String, Int>,    // date -> total mins (for week/month)
        val topTasks: List<Pair<String, Int>>     // task name -> total mins
    )

    val period = mutableStateOf(Period.DAY)
    val stats = mutableStateOf<StatsData?>(null)
    val selectedDate = mutableStateOf(Calendar.getInstance())

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    fun loadStats() {
        execute(false) {
            val cal = selectedDate.value
            val data = when (period.value) {
                Period.DAY -> loadDayStats(cal)
                Period.WEEK -> loadWeekStats(cal)
                Period.MONTH -> loadMonthStats(cal)
            }
            stats.value = data
        }
    }

    fun changePeriod(newPeriod: Period) {
        period.value = newPeriod
        loadStats()
    }

    fun navigatePrevious() {
        val cal = selectedDate.value.clone() as Calendar
        when (period.value) {
            Period.DAY -> cal.add(Calendar.DAY_OF_MONTH, -1)
            Period.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, -1)
            Period.MONTH -> cal.add(Calendar.MONTH, -1)
        }
        selectedDate.value = cal
        loadStats()
    }

    fun navigateNext() {
        val cal = selectedDate.value.clone() as Calendar
        when (period.value) {
            Period.DAY -> cal.add(Calendar.DAY_OF_MONTH, 1)
            Period.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            Period.MONTH -> cal.add(Calendar.MONTH, 1)
        }
        selectedDate.value = cal
        loadStats()
    }

    fun formatDuration(mins: Int): String {
        val hours = mins / 60
        val minutes = mins % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    private suspend fun loadDayStats(cal: Calendar): StatsData {
        val date = dateFormat.format(cal.time)
        val tasks = taskInfoDao.getList(date)
        val displayFormat = SimpleDateFormat("dd MMM yyyy, EEEE", Locale.getDefault())

        return buildStatsData(Period.DAY, displayFormat.format(cal.time), tasks)
    }

    private suspend fun loadWeekStats(cal: Calendar): StatsData {
        val weekCal = cal.clone() as Calendar
        weekCal.set(Calendar.DAY_OF_WEEK, weekCal.firstDayOfWeek)
        val startDate = dateFormat.format(weekCal.time)
        weekCal.add(Calendar.DAY_OF_WEEK, 6)
        val endDate = dateFormat.format(weekCal.time)

        val tasks = taskInfoDao.getRange(startDate, endDate)
        val displayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

        weekCal.add(Calendar.DAY_OF_WEEK, -6)
        val startLabel = displayFormat.format(weekCal.time)
        weekCal.add(Calendar.DAY_OF_WEEK, 6)
        val endLabel = displayFormat.format(weekCal.time)

        return buildStatsData(Period.WEEK, "$startLabel - $endLabel", tasks)
    }

    private suspend fun loadMonthStats(cal: Calendar): StatsData {
        val monthCal = cal.clone() as Calendar
        monthCal.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = dateFormat.format(monthCal.time)
        monthCal.set(Calendar.DAY_OF_MONTH, monthCal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDate = dateFormat.format(monthCal.time)

        val tasks = taskInfoDao.getRange(startDate, endDate)
        val displayFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        return buildStatsData(Period.MONTH, displayFormat.format(cal.time), tasks)
    }

    private fun buildStatsData(
        period: Period,
        dateLabel: String,
        tasks: List<TaskInfo>
    ): StatsData {
        val totalMins = tasks.sumOf { it.durationInMins }
        val categoryBreakdown = tasks
            .groupBy { it.category.ifEmpty { it.name } }
            .mapValues { (_, items) -> items.sumOf { it.durationInMins } }
            .toSortedMap()

        val dailyBreakdown = tasks
            .groupBy { it.date }
            .mapValues { (_, items) -> items.sumOf { it.durationInMins } }

        val topTasks = tasks
            .groupBy { it.name }
            .mapValues { (_, items) -> items.sumOf { it.durationInMins } }
            .entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key to it.value }

        return StatsData(
            period = period,
            dateLabel = dateLabel,
            totalMinutes = totalMins,
            taskCount = tasks.size,
            categoryBreakdown = categoryBreakdown,
            dailyBreakdown = dailyBreakdown,
            topTasks = topTasks
        )
    }
}
