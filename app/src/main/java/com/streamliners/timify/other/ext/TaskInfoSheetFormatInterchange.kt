package com.streamliners.timify.other.ext

import com.streamliners.timify.domain.model.TaskInfo

fun List<TaskInfo>.toRows(): List<List<String>> {
    return buildList {
        add(
            listOf("Date", "Start", "End", "Task Name")
        )

        var currentDate = ""

        this@toRows.forEach {
            with(it) {
                if (it.date != currentDate) {
                    val isFirst = currentDate.isBlank()
                    currentDate = it.date
                    if (!isFirst) add(listOf("-", "-", "-", "-"))
                }

                add(listOf(date, startTime, endTime, name))
            }
        }
    }
}

fun List<List<String>>.parseAsTaskInfoList(): List<TaskInfo> {
    return drop(1).mapNotNull { fields ->
        if (fields.all { it.isBlank() || it == "-" }) null else
        TaskInfo(
            date = fields[0],
            startTime = fields[1],
            endTime = fields[2],
            name = fields[3]
        )
    }
}