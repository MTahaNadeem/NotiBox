package com.example.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppSummary
import com.example.data.HourlyStats
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SummaryCards(state = state)
            }
            item {
                HourlyPeakChart(hourlyStats = state.hourlyStats)
            }
            item {
                Text(
                    text = "App Volume Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(state.appSummaries) { app ->
                AppVolumeItem(app = app, maxCount = state.appSummaries.maxOfOrNull { it.count } ?: 1)
            }
        }
    }
}

@Composable
fun SummaryCards(state: AnalyticsState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Today", style = MaterialTheme.typography.labelMedium)
                Text(
                    "${state.todayCount}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Top Distractor", style = MaterialTheme.typography.labelMedium)
                val topApp = state.appSummaries.firstOrNull()?.appName ?: "N/A"
                Text(
                    topApp,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun HourlyPeakChart(hourlyStats: List<HourlyStats>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Peak Distraction Hours",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val maxCount = hourlyStats.maxOfOrNull { it.count }?.toFloat()?.coerceAtLeast(1f) ?: 1f
            val barColor = MaterialTheme.colorScheme.primary

            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                val barWidth = size.width / 24
                
                for (hour in 0..23) {
                    val count = hourlyStats.find { it.hourOfDay == hour }?.count ?: 0
                    val heightRatio = count / maxCount
                    val barHeight = size.height * heightRatio
                    
                    drawRect(
                        color = barColor,
                        topLeft = Offset(x = hour * barWidth + (barWidth * 0.1f), y = size.height - barHeight),
                        size = Size(width = barWidth * 0.8f, height = barHeight)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("12 AM", style = MaterialTheme.typography.labelSmall)
                Text("12 PM", style = MaterialTheme.typography.labelSmall)
                Text("11 PM", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun AppVolumeItem(app: AppSummary, maxCount: Int) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(app.appName, style = MaterialTheme.typography.bodyMedium)
            Text("${app.count}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { app.count.toFloat() / maxCount.toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
