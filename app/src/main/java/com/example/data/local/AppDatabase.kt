package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BellScheduleEntity
import com.example.data.model.CustomBellSoundEntity

@Database(
    entities = [BellScheduleEntity::class, CustomBellSoundEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bellScheduleDao(): BellScheduleDao
    abstract fun customBellSoundDao(): CustomBellSoundDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "academy_smart_bell.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
