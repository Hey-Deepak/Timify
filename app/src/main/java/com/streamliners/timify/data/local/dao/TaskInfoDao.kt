package com.streamliners.timify.data.local.dao

import android.database.sqlite.SQLiteCursor
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.streamliners.timify.domain.model.TaskInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskInfoDao {

    @Query("SELECT * FROM TasksInfo WHERE date = :date ORDER BY startTime ASC")
    suspend fun getList(date: String): List<TaskInfo>

    @Query("SELECT * FROM TasksInfo WHERE date = :date ORDER BY startTime ASC")
    fun getListFlow(date: String): Flow<List<TaskInfo>>

    @Query("SELECT * FROM TasksInfo")
    suspend fun getAll(): List<TaskInfo>

    @Query("SELECT * FROM TasksInfo WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    suspend fun getRange(startDate: String, endDate: String): List<TaskInfo>

    @Query("SELECT DISTINCT category FROM TasksInfo WHERE category != '' ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>

    @Query("SELECT SUM(durationInMins) FROM TasksInfo WHERE date = :date")
    suspend fun getTotalMinutesForDate(date: String): Int?

    @Query("SELECT SUM(durationInMins) FROM TasksInfo WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalMinutesForRange(startDate: String, endDate: String): Int?

    @Query("DELETE FROM TasksInfo")
    suspend fun clear()

    @Query("DELETE FROM TasksInfo WHERE date = :date")
    suspend fun clearAllOf(date: String)

    @Query("DELETE FROM TasksInfo WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Insert
    suspend fun add(taskInfo: TaskInfo): Long

    @Insert
    suspend fun addAll(taskInfo: List<TaskInfo>)

    @Update
    suspend fun update(taskInfo: TaskInfo)

    @Query("SELECT COUNT(*) FROM TasksInfo")
    suspend fun getTotalRowCount(): Int

    @Query("SELECT COUNT(*) FROM TasksInfo WHERE date = :date")
    suspend fun getCountForDate(date: String): Int

    @Query("SELECT id FROM TasksInfo ORDER BY id ASC LIMIT 1")
    fun getFirstId(): Int

    // Raw Queries

    @RawQuery
    suspend fun rawQueryAsInt(query: SupportSQLiteQuery): Int?

    @RawQuery
    suspend fun rawQueryAsIntList(query: SupportSQLiteQuery): List<Int>

}