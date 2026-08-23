package com.example.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.LifespanPurge
import com.example.data.RetentionPreferences
import java.io.File

class DatabaseCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.notificationDao()
            val prefs = RetentionPreferences(applicationContext)

            val protectRevoked = prefs.protectRevoked.value
            val lifespan = prefs.lifespanPurge.value
            val capBytes = prefs.storageCap.value.bytes

            // 1. Lifespan Purge
            lifespan.days?.let { days ->
                val olderThanMillis = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
                dao.deleteOldNotifications(olderThanMillis, protectRevoked)
            }

            // 2. Storage Cap Management
            val dbFile = applicationContext.getDatabasePath(AppDatabase.DATABASE_NAME)
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")
            
            var totalBytes = dbFile.length() + walFile.length() + shmFile.length()
            
            if (totalBytes > capBytes) {
                // Batch delete 5000 records at a time until under cap
                while (totalBytes > capBytes) {
                    val deleteCount = 5000
                    val oldestIds = dao.getOldestNotificationIds(deleteCount, protectRevoked)
                    if (oldestIds.isEmpty()) break // Nothing left to delete
                    
                    dao.deleteNotificationsByIds(oldestIds)
                    
                    // Force checkpoint to reclaim WAL space and update file sizes
                    db.checkpointWal()
                    
                    totalBytes = dbFile.length() + walFile.length() + shmFile.length()
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
