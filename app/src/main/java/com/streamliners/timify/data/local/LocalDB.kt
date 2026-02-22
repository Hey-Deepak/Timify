package com.streamliners.timify.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.streamliners.timify.data.local.dao.ChatHistoryDao
import com.streamliners.timify.data.local.dao.CustomAttributeDao
import com.streamliners.timify.data.local.dao.TaskInfoDao
import com.streamliners.timify.domain.model.ChatHistoryItem
import com.streamliners.timify.domain.model.CustomAttribute
import com.streamliners.timify.domain.model.TaskInfo

@Database(entities = [ChatHistoryItem::class, TaskInfo::class, CustomAttribute::class], version = 2)
abstract class LocalDB : RoomDatabase(){

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE TasksInfo ADD COLUMN category TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE TasksInfo ADD COLUMN source TEXT NOT NULL DEFAULT 'chat'")
            }
        }

        fun create(context: Context): LocalDB {
            return Room.databaseBuilder(
                context = context,
                klass = LocalDB::class.java,
                name = "roomDB"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }

    abstract fun chatHistoryDao(): ChatHistoryDao

    abstract fun taskInfoDao(): TaskInfoDao

    abstract fun customAttributeDao(): CustomAttributeDao
}