package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SavedConfigEntity::class, BattleRecordEntity::class, SavedSkirmishBattleEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: runCatching {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "crimson_sector.db"
                    )
                        .fallbackToDestructiveMigration(dropAllTables = true)
                        .build()
                }.getOrElse {
                    Room.inMemoryDatabaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java
                    )
                        .build()
                }.also { INSTANCE = it }
            }
        }
    }
}
