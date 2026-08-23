package com.example.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppSummary
import com.example.data.HourlyStats
import com.example.data.NotificationDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class AnalyticsState(
    val appSummaries: List<AppSummary> = emptyList(),
    val hourlyStats: List<HourlyStats> = emptyList(),
    val todayCount: Int = 0
)

class AnalyticsViewModel(private val dao: NotificationDao) : ViewModel() {

    private val startOfToday: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    val state: StateFlow<AnalyticsState> = combine(
        dao.getDistinctApps(),
        dao.getHourlyStats(),
        dao.getNotificationsCountSince(startOfToday)
    ) { apps, hourly, today ->
        AnalyticsState(
            appSummaries = apps,
            hourlyStats = hourly,
            todayCount = today
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsState()
    )
}
