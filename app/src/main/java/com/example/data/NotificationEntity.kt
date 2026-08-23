package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["postTime"]),
        Index(value = ["appName"]),
        Index(value = ["title"]),
        Index(value = ["isRevoked"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val bigText: String? = null,
    val summaryText: String? = null,
    val infoText: String? = null,
    val subText: String? = null,
    val stackedLines: String? = null,
    val category: String? = null,
    val postTime: Long = System.currentTimeMillis(),
    val notificationId: Int = 0,
    val tag: String? = null,
    val key: String? = null,
    val isOngoing: Boolean = false,
    val isRevoked: Boolean = false,
    val revokedAt: Long? = null,
    val isCategoryPromo: Boolean = false
) {
    val bestDisplayContent: String
        get() {
            if (!stackedLines.isNullOrBlank()) return stackedLines
            if (!bigText.isNullOrBlank()) return bigText
            if (content.isNotBlank()) return content
            if (!summaryText.isNullOrBlank()) return summaryText
            return infoText.orEmpty()
        }
}
