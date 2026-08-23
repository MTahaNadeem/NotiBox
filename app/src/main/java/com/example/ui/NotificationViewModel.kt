package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppSummary
import com.example.data.NotificationEntity
import com.example.data.ThemeMode
import com.example.data.ThemePreferences
import com.example.service.NotificationCaptureService
import com.example.utils.ExportHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.example.workers.DatabaseCleanupWorker

enum class NotificationTab {
    ALL, REVOKED
}

sealed interface UiMessage {
    data class Success(val text: String) : UiMessage
    data class Error(val text: String) : UiMessage
}

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.notificationDao()
    private val themePrefs = ThemePreferences(application)

    // Theme Management
    val themeMode: StateFlow<ThemeMode> = themePrefs.themeMode

    fun setThemeMode(mode: ThemeMode) {
        themePrefs.setThemeMode(mode)
    }

    // Standard State
    private val _currentTab = MutableStateFlow(NotificationTab.ALL)
    val currentTab: StateFlow<NotificationTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPackage = MutableStateFlow<String?>(null)
    val selectedPackage: StateFlow<String?> = _selectedPackage.asStateFlow()

    private val _selectedNotification = MutableStateFlow<NotificationEntity?>(null)
    val selectedNotification: StateFlow<NotificationEntity?> = _selectedNotification.asStateFlow()

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UiMessage>()
    val uiMessage: SharedFlow<UiMessage> = _uiMessage.asSharedFlow()

    // Dialog & Deletion State
    private val _itemPendingDelete = MutableStateFlow<NotificationEntity?>(null)
    val itemPendingDelete: StateFlow<NotificationEntity?> = _itemPendingDelete.asStateFlow()

    private val _filterStartTime = MutableStateFlow<Long?>(null)
    val filterStartTime: StateFlow<Long?> = _filterStartTime.asStateFlow()
    
    private val _filterEndTime = MutableStateFlow<Long?>(null)
    val filterEndTime: StateFlow<Long?> = _filterEndTime.asStateFlow()

    val distinctApps: StateFlow<List<AppSummary>> = dao.getDistinctApps()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalCount: StateFlow<Int> = dao.getCountFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val notificationsFeed: StateFlow<List<NotificationEntity>> = combine(
        _searchQuery.debounce(150).distinctUntilChanged(),
        _selectedPackage,
        _currentTab,
        _filterStartTime,
        _filterEndTime
    ) { query, pkg, tab, start, end ->
        val isRevoked = if (tab == NotificationTab.REVOKED) true else null
        dao.getFilteredNotifications(
            query = query.trim(),
            packageName = pkg,
            startTime = start,
            endTime = end,
            isRevoked = isRevoked
        )
    }.flatMapLatest { it }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        checkPermission()
        checkAndSeedSamplesIfEmpty()
        setupCleanupWorker()
    }

    private fun setupCleanupWorker() {
        val workRequest = PeriodicWorkRequestBuilder<DatabaseCleanupWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            "notibox_cleanup_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun setTab(tab: NotificationTab) {
        _currentTab.value = tab
    }

    fun checkPermission() {
        val context = getApplication<Application>()
        val cn = ComponentName(context, NotificationCaptureService::class.java)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val granted = flat != null && flat.contains(cn.flattenToString())
        val systemGranted = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        _isPermissionGranted.value = granted || systemGranted
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectPackage(packageName: String?) {
        _selectedPackage.value = packageName
    }

    fun openDetail(item: NotificationEntity) {
        _selectedNotification.value = item
    }

    fun closeDetail() {
        _selectedNotification.value = null
    }

    // --- Deletion Flow (Single Item) ---
    fun confirmDelete(item: NotificationEntity) {
        _itemPendingDelete.value = item
    }

    fun cancelDelete() {
        _itemPendingDelete.value = null
    }

    fun executeDelete() {
        val item = _itemPendingDelete.value ?: return
        viewModelScope.launch {
            dao.deleteNotificationById(item.id)
            if (_selectedNotification.value?.id == item.id) {
                _selectedNotification.value = null
            }
            _itemPendingDelete.value = null
            _uiMessage.emit(UiMessage.Success("Notification log removed"))
        }
    }

    fun setDateFilter(startTime: Long?, endTime: Long?) {
        _filterStartTime.value = startTime
        _filterEndTime.value = endTime
    }

    // --- Exports ---
    fun exportDatabase(destinationUri: Uri) {
        viewModelScope.launch {
            val result = ExportHelper.exportDatabaseToUri(getApplication(), destinationUri)
            result.onSuccess {
                _uiMessage.emit(UiMessage.Success("SQLite database file successfully exported!"))
            }.onFailure { error ->
                _uiMessage.emit(UiMessage.Error("Database export failed: ${error.localizedMessage}"))
            }
        }
    }

    fun exportCsv(destinationUri: Uri) {
        viewModelScope.launch {
            val result = ExportHelper.exportCsvToUri(getApplication(), destinationUri)
            result.onSuccess { count ->
                _uiMessage.emit(UiMessage.Success("Exported $count records to CSV file successfully!"))
            }.onFailure { error ->
                _uiMessage.emit(UiMessage.Error("CSV export failed: ${error.localizedMessage}"))
            }
        }
    }

    private fun checkAndSeedSamplesIfEmpty() {
        viewModelScope.launch {
            val existing = dao.getAllForExport()
            if (existing.isEmpty()) {
                val now = System.currentTimeMillis()
                val samples = listOf(
                    NotificationEntity(
                        packageName = "com.instagram.android",
                        appName = "Instagram",
                        title = "alex_design",
                        content = "Sent a reel: 'Top 10 Modern AMOLED UI Concepts in Jetpack Compose'",
                        bigText = "Sent a reel: 'Top 10 Modern AMOLED UI Concepts in Jetpack Compose' - Tap to watch and reply.",
                        stackedLines = "alex_design: Hey, check this new UI reel!\nalex_design: Sent a reel: Top 10 Modern AMOLED UI Concepts",
                        category = "msg",
                        postTime = now - 1000 * 60 * 3, // 3 mins ago
                        notificationId = 101
                    ),
                    NotificationEntity(
                        packageName = "com.whatsapp",
                        appName = "WhatsApp",
                        title = "Android Architecture Team (3 messages)",
                        content = "Sarah: Room WAL checkpointing is integrated flawlessly!",
                        stackedLines = "Elena: Did we test the SQLite streaming export?\nDavid: Yes, works without root!\nSarah: Room WAL checkpointing is integrated flawlessly!",
                        category = "msg",
                        postTime = now - 1000 * 60 * 18, // 18 mins ago
                        notificationId = 102
                    ),
                    NotificationEntity(
                        packageName = "org.telegram.messenger",
                        appName = "Telegram",
                        title = "DevSecOps Channel",
                        content = "New Release: NotiBox 1.0 - Zero Network Permission, 100% On-Device Privacy.",
                        category = "social",
                        postTime = now - 1000 * 60 * 60 * 2, // 2 hours ago
                        notificationId = 103
                    )
                )
                for (sample in samples) {
                    dao.insertNotification(sample)
                }
            }
        }
    }
}
