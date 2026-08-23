package com.example.utils

import android.content.Context
import android.net.Uri
import com.example.data.AppDatabase
import com.example.data.NotificationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ExportHelper {

    private val exactDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val timeOnlyFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateWithTimeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    fun formatExactTimestamp(epochMillis: Long): String {
        return exactDateFormat.format(Date(epochMillis))
    }

    fun formatRelativeTime(epochMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - epochMillis

        if (diff < 0) return "Just now"
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        if (seconds < 45) return "Just now"
        if (minutes < 60) return "${minutes}m ago"
        if (hours < 24) {
            val notifCal = Calendar.getInstance().apply { timeInMillis = epochMillis }
            val nowCal = Calendar.getInstance().apply { timeInMillis = now }
            if (notifCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)) {
                return "${hours}h ago (${timeOnlyFormat.format(Date(epochMillis))})"
            }
        }

        val notifCal = Calendar.getInstance().apply { timeInMillis = epochMillis }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }
        if (nowCal.get(Calendar.DAY_OF_YEAR) - notifCal.get(Calendar.DAY_OF_YEAR) == 1 &&
            nowCal.get(Calendar.YEAR) == notifCal.get(Calendar.YEAR)
        ) {
            return "Yesterday, ${timeOnlyFormat.format(Date(epochMillis))}"
        }

        return dateWithTimeFormat.format(Date(epochMillis))
    }

    suspend fun exportDatabaseToUri(context: Context, destinationUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            // Flush WAL before copying
            db.checkpointWal()

            val dbFile = AppDatabase.getDatabaseFile(context)
            if (!dbFile.exists()) {
                return@withContext Result.failure(Exception("Database file does not exist yet."))
            }

            val outputStream = context.contentResolver.openOutputStream(destinationUri)
                ?: return@withContext Result.failure(Exception("Could not open destination storage."))

            outputStream.use { out ->
                FileInputStream(dbFile).use { input ->
                    input.copyTo(out)
                }
                out.flush()
            }
            Result.success(1)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportCsvToUri(context: Context, destinationUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val items = db.notificationDao().getAllForExport()

            val outputStream = context.contentResolver.openOutputStream(destinationUri)
                ?: return@withContext Result.failure(Exception("Could not open destination storage."))

            OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                // Write CSV Header
                writer.write("ID,Package,AppName,Title,Message,ExactTimestamp,EpochMillis,Category,NotificationId\r\n")

                for (item in items) {
                    val line = listOf(
                        item.id.toString(),
                        escapeCsvField(item.packageName),
                        escapeCsvField(item.appName),
                        escapeCsvField(item.title),
                        escapeCsvField(item.bestDisplayContent),
                        escapeCsvField(formatExactTimestamp(item.postTime)),
                        item.postTime.toString(),
                        escapeCsvField(item.category ?: ""),
                        item.notificationId.toString()
                    ).joinToString(",")

                    writer.write(line)
                    writer.write("\r\n")
                }
                writer.flush()
            }
            Result.success(items.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeCsvField(value: String): String {
        val containsSpecial = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        return if (containsSpecial) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
