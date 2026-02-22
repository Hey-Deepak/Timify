package com.streamliners.timify.feature.home

import androidx.compose.runtime.mutableStateOf
import com.streamliners.base.BaseViewModel
import com.streamliners.base.ext.execute
import com.streamliners.timify.data.local.dao.TaskInfoDao
import com.streamliners.timify.domain.model.TaskInfo
import com.streamliners.utils.DateTimeUtils
import com.streamliners.utils.DateTimeUtils.formatTime
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(
    private val taskInfoDao: TaskInfoDao
) : BaseViewModel() {

    data class DaySummary(
        val tasks: List<TaskInfo>,
        val totalMinutes: Int,
        val categoryBreakdown: Map<String, Int> // category -> total mins
    )

    val todayDate = formatTime(DateTimeUtils.Format("yyyy/MM/dd"))
    val displayDate = formatTime(DateTimeUtils.Format("dd MMM yyyy, EEEE"))
    val summary = mutableStateOf<DaySummary?>(null)

    fun loadToday() {
        execute(false) {
            launch {
                taskInfoDao.getListFlow(todayDate).collectLatest { tasks ->
                    val totalMins = tasks.sumOf { it.durationInMins }
                    val categoryBreakdown = tasks
                        .groupBy { it.category.ifEmpty { it.name } }
                        .mapValues { (_, items) -> items.sumOf { it.durationInMins } }
                        .toSortedMap()

                    summary.value = DaySummary(
                        tasks = tasks,
                        totalMinutes = totalMins,
                        categoryBreakdown = categoryBreakdown
                    )
                }
            }
        }
    }

    fun deleteTask(task: TaskInfo) {
        execute(false) {
            taskInfoDao.deleteById(task.id)
        }
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
}
