package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class AppSummary(
    val packageName: String,
    val appName: String,
    val count: Int
)

data class HourlyStats(
    val hourOfDay: Int,
    val count: Int
)

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY postTime DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isRevoked = 1 ORDER BY postTime DESC")
    fun getRevokedNotifications(): Flow<List<NotificationEntity>>

    @Query(
        """
        SELECT * FROM notifications 
        WHERE title LIKE '%' || :query || '%' 
           OR content LIKE '%' || :query || '%' 
           OR bigText LIKE '%' || :query || '%'
           OR stackedLines LIKE '%' || :query || '%'
           OR appName LIKE '%' || :query || '%'
           OR packageName LIKE '%' || :query || '%'
        ORDER BY postTime DESC
        """
    )
    fun searchNotifications(query: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE packageName = :packageName ORDER BY postTime DESC")
    fun getNotificationsByPackage(packageName: String): Flow<List<NotificationEntity>>

    @Query(
        """
        SELECT * FROM notifications 
        WHERE packageName = :packageName 
          AND (title LIKE '%' || :query || '%' 
               OR content LIKE '%' || :query || '%' 
               OR bigText LIKE '%' || :query || '%'
               OR stackedLines LIKE '%' || :query || '%'
               OR appName LIKE '%' || :query || '%')
        ORDER BY postTime DESC
        """
    )
    fun searchNotificationsByPackage(packageName: String, query: String): Flow<List<NotificationEntity>>

    @Query(
        """
        SELECT * FROM notifications 
        WHERE (:query = '' OR title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR bigText LIKE '%' || :query || '%' OR stackedLines LIKE '%' || :query || '%' OR appName LIKE '%' || :query || '%')
          AND (:packageName IS NULL OR packageName = :packageName)
          AND (:startTime IS NULL OR postTime >= :startTime)
          AND (:endTime IS NULL OR postTime <= :endTime)
          AND (:isRevoked IS NULL OR isRevoked = :isRevoked)
        ORDER BY postTime DESC
        """
    )
    fun getFilteredNotifications(
        query: String,
        packageName: String?,
        startTime: Long?,
        endTime: Long?,
        isRevoked: Boolean?
    ): Flow<List<NotificationEntity>>

    @Query(
        """
        SELECT packageName, appName, COUNT(*) as count 
        FROM notifications 
        GROUP BY packageName 
        ORDER BY count DESC, MAX(postTime) DESC
        """
    )
    fun getDistinctApps(): Flow<List<AppSummary>>

    @Query(
        """
        SELECT CAST(strftime('%H', postTime / 1000, 'unixepoch', 'localtime') AS INTEGER) as hourOfDay, COUNT(*) as count 
        FROM notifications 
        GROUP BY hourOfDay 
        ORDER BY hourOfDay ASC
        """
    )
    fun getHourlyStats(): Flow<List<HourlyStats>>

    @Query("SELECT COUNT(*) FROM notifications WHERE postTime >= :startTime")
    fun getNotificationsCountSince(startTime: Long): Flow<Int>

    @Query("SELECT * FROM notifications ORDER BY postTime DESC")
    suspend fun getAllForExport(): List<NotificationEntity>

    @Query("SELECT postTime FROM notifications WHERE packageName = :packageName AND title = :title AND content = :content ORDER BY postTime DESC LIMIT 1")
    suspend fun getLatestDuplicateTime(packageName: String, title: String, content: String): Long?

    @Query("SELECT * FROM notifications WHERE packageName = :packageName AND title = :title AND isRevoked = 0 ORDER BY postTime DESC LIMIT 1")
    suspend fun getLatestMessageForRevocation(packageName: String, title: String): NotificationEntity?

    @Query("UPDATE notifications SET isRevoked = 1, revokedAt = :revokedAt WHERE id = :id")
    suspend fun markAsRevoked(id: Long, revokedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Long): Int

    @Query("DELETE FROM notifications WHERE packageName = :packageName")
    suspend fun deleteNotificationsByPackage(packageName: String): Int

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications(): Int

    @Query("DELETE FROM notifications WHERE postTime < :olderThan AND (isRevoked = 0 OR :protectRevoked = 0)")
    suspend fun deleteOldNotifications(olderThan: Long, protectRevoked: Boolean): Int

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getTotalCount(): Int

    @Query("SELECT id FROM notifications WHERE (isRevoked = 0 OR :protectRevoked = 0) ORDER BY postTime ASC LIMIT :limit")
    suspend fun getOldestNotificationIds(limit: Int, protectRevoked: Boolean): List<Long>

    @Query("DELETE FROM notifications WHERE id IN (:ids)")
    suspend fun deleteNotificationsByIds(ids: List<Long>): Int

    @Query("SELECT COUNT(*) FROM notifications")
    fun getCountFlow(): Flow<Int>
}
