package com.example.makebreak.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.makebreak.core.data.local.dao.UserSettingsDao
import com.example.makebreak.core.data.local.entities.UserSettingsEntity

@Database(
    entities = [UserSettingsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MakeBreakDb : RoomDatabase() {

    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        const val DATABASE_NAME = "make_break_db"
    }
}
