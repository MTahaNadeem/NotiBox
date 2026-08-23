package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LifespanPurge {
    DAYS_3, DAYS_7, DAYS_30, NEVER;
    
    val days: Long?
        get() = when (this) {
            DAYS_3 -> 3
            DAYS_7 -> 7
            DAYS_30 -> 30
            NEVER -> null
        }
}

enum class StorageCap {
    MB_500, GB_1, GB_2, GB_5;

    val bytes: Long
        get() = when (this) {
            MB_500 -> 500L * 1024L * 1024L
            GB_1 -> 1L * 1024L * 1024L * 1024L
            GB_2 -> 2L * 1024L * 1024L * 1024L
            GB_5 -> 5L * 1024L * 1024L * 1024L
        }
    
    val displayName: String
        get() = when (this) {
            MB_500 -> "500 MB"
            GB_1 -> "1 GB"
            GB_2 -> "2 GB"
            GB_5 -> "5 GB (Default / Max)"
        }
}

class RetentionPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("notibox_retention_prefs", Context.MODE_PRIVATE)

    private val _lifespanPurge = MutableStateFlow(getSavedLifespanPurge())
    val lifespanPurge: StateFlow<LifespanPurge> = _lifespanPurge.asStateFlow()

    private val _protectRevoked = MutableStateFlow(getSavedProtectRevoked())
    val protectRevoked: StateFlow<Boolean> = _protectRevoked.asStateFlow()

    private val _storageCap = MutableStateFlow(getSavedStorageCap())
    val storageCap: StateFlow<StorageCap> = _storageCap.asStateFlow()

    fun setLifespanPurge(lifespan: LifespanPurge) {
        prefs.edit().putString("lifespan_purge", lifespan.name).apply()
        _lifespanPurge.value = lifespan
    }

    fun setProtectRevoked(protect: Boolean) {
        prefs.edit().putBoolean("protect_revoked", protect).apply()
        _protectRevoked.value = protect
    }

    fun setStorageCap(cap: StorageCap) {
        prefs.edit().putString("storage_cap", cap.name).apply()
        _storageCap.value = cap
    }

    private fun getSavedLifespanPurge(): LifespanPurge {
        val saved = prefs.getString("lifespan_purge", LifespanPurge.NEVER.name) ?: LifespanPurge.NEVER.name
        return try {
            LifespanPurge.valueOf(saved)
        } catch (e: Exception) {
            LifespanPurge.NEVER
        }
    }

    private fun getSavedProtectRevoked(): Boolean {
        return prefs.getBoolean("protect_revoked", true)
    }

    private fun getSavedStorageCap(): StorageCap {
        val saved = prefs.getString("storage_cap", StorageCap.GB_5.name) ?: StorageCap.GB_5.name
        return try {
            StorageCap.valueOf(saved)
        } catch (e: Exception) {
            StorageCap.GB_5
        }
    }
}
