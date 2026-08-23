package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

@Database(
    entities = [NotificationEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun notificationDao(): NotificationDao

    fun checkpointWal() {
        try {
            val cursor = openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)")
            cursor.moveToFirst()
            cursor.close()
            
            // Optionally VACUUM to reclaim space in main db file
            openHelper.writableDatabase.execSQL("VACUUM")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val DATABASE_NAME = "notibox_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getDatabaseFile(context: Context): File {
            return context.getDatabasePath(DATABASE_NAME)
        }
    }
}
