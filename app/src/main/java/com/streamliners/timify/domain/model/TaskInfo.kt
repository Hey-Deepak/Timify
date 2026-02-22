package com.streamliners.timify.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("TasksInfo")
data class TaskInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val startTime: String,
    val endTime: String,
    val durationInMins: Int,
    val name: String,
    val category: String = "",
    val source: String = SOURCE_VOICE
) {
    companion object {
        const val SOURCE_VOICE = "voice"
        const val SOURCE_MANUAL = "manual"
        const val SOURCE_CHAT = "chat"
    }
}
