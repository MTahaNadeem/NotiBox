package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.NotificationEntity
import com.example.ui.components.DatePickerFilterDialog
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.FilterChipsRow
import com.example.ui.components.NotiBoxTopAppBar
import com.example.ui.components.NotificationCardItem
import com.example.ui.components.PermissionBanner
import com.example.utils.ExportHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotiBoxScreen(
    viewModel: NotificationViewModel,
    onExportDbRequested: () -> Unit,
    onExportCsvRequested: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedPackage by viewModel.selectedPackage.collectAsStateWithLifecycle()
    val feed by viewModel.notificationsFeed.collectAsStateWithLifecycle()
    val distinctApps by viewModel.distinctApps.collectAsStateWithLifecycle()
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsStateWithLifecycle()
    val selectedDetail by viewModel.selectedNotification.collectAsStateWithLifecycle()
    
    val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val itemPendingDelete by viewModel.itemPendingDelete.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    var showExportMenu by remember { mutableStateOf(false) }
    var showHyperOsGuideDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiMessage.collect { msg ->
            when (msg) {
                is UiMessage.Success -> snackbarHostState.showSnackbar(msg.text)
                is UiMessage.Error -> snackbarHostState.showSnackbar(msg.text)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notibox_main_scaffold"),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .widthIn(max = 720.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NotiBoxTopAppBar(
                currentThemeMode = currentThemeMode,
                onThemeModeSelected = { viewModel.setThemeMode(it) },
                onExportClick = { showExportMenu = true },
                isExportMenuExpanded = showExportMenu,
                onDismissExportMenu = { showExportMenu = false },
                onExportDb = {
                    showExportMenu = false
                    onExportDbRequested()
                },
                onExportCsv = {
                    showExportMenu = false
                    onExportCsvRequested()
                }
            )

            if (!isPermissionGranted) {
                PermissionBanner(
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onLearnHyperOs = { showHyperOsGuideDialog = true }
                )
            }

            SecondaryTabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = currentTab == NotificationTab.ALL,
                    onClick = { viewModel.setTab(NotificationTab.ALL) },
                    text = { Text("All Messages") }
                )
                Tab(
                    selected = currentTab == NotificationTab.REVOKED,
                    onClick = { viewModel.setTab(NotificationTab.REVOKED) },
                    text = { 
                        Text("Revoked / Unsent", color = if (currentTab == NotificationTab.REVOKED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) 
                    }
                )
            }

            val filterStartTime by viewModel.filterStartTime.collectAsStateWithLifecycle()
            val filterEndTime by viewModel.filterEndTime.collectAsStateWithLifecycle()

            if (currentTab == NotificationTab.ALL) {
                SearchBarSection(
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onClearQuery = { viewModel.updateSearchQuery("") },
                    filterStartTime = filterStartTime,
                    filterEndTime = filterEndTime,
                    onDateFilterChange = { start, end -> viewModel.setDateFilter(start, end) }
                )

                FilterChipsRow(
                    apps = distinctApps,
                    selectedPackage = selectedPackage,
                    totalCount = feed.size,
                    onSelectPackage = { viewModel.selectPackage(it) }
                )
            }

            if (feed.isEmpty()) {
                EmptyStateView(
                    searchQuery = searchQuery,
                    selectedPackage = selectedPackage,
                    isPermissionGranted = isPermissionGranted
                )
            } else {
                NotificationFeedList(
                    items = feed,
                    onItemClick = { viewModel.openDetail(it) },
                    onDeleteItem = { viewModel.confirmDelete(it) }
                )
            }
        }
    }

    selectedDetail?.let { notification ->
        NotificationDetailBottomSheet(
            notification = notification,
            onDismiss = { viewModel.closeDetail() },
            onDelete = { viewModel.confirmDelete(notification) },
            onCopy = { text ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Notification Text", text)
                clipboard.setPrimaryClip(clip)
                scope.launch {
                    snackbarHostState.showSnackbar("Copied notification text to clipboard")
                }
            },
            onOpenApp = { pkg ->
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(launchIntent)
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Cannot open application directly")
                    }
                }
            }
        )
    }

    itemPendingDelete?.let { _ ->
        DeleteConfirmationDialog(
            title = "Delete Notification?",
            message = "Are you sure you want to delete this notification? This action cannot be undone.",
            icon = Icons.Default.Delete,
            onConfirm = { viewModel.executeDelete() },
            onDismiss = { viewModel.cancelDelete() }
        )
    }

    if (showHyperOsGuideDialog) {
        AlertDialog(
            onDismissRequest = { showHyperOsGuideDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("HyperOS / MIUI Setup", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "On Android 13+, HyperOS, and MIUI devices, system privacy may block listener permissions with 'Restricted Setting'.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    GuideStepItem("1", "Open device Settings > Apps > Manage Apps.")
                    GuideStepItem("2", "Find and select 'NotiBox'.")
                    GuideStepItem("3", "Tap the 3-dots menu (top right) and tap 'Allow restricted settings'.")
                    GuideStepItem("4", "Return here and tap 'Grant Access' to enable the listener.")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                Button(onClick = { showHyperOsGuideDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }
}

@Composable
private fun GuideStepItem(step: String, instruction: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(step, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(instruction, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    filterStartTime: Long?,
    filterEndTime: Long?,
    onDateFilterChange: (Long?, Long?) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input"),
                placeholder = {
                    Text(
                        "Search logs...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = onClearQuery,
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showDatePicker = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Filter by Date",
                    tint = if (filterStartTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (filterStartTime != null) {
            Spacer(modifier = Modifier.height(8.dp))
            val dateLabel = if (filterEndTime != null && (filterEndTime - filterStartTime) > 24 * 60 * 60 * 1000L) {
                val format = SimpleDateFormat("MMM d", Locale.getDefault())
                "${format.format(Date(filterStartTime))} - ${format.format(Date(filterEndTime))}"
            } else {
                val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                format.format(Date(filterStartTime))
            }

            androidx.compose.material3.InputChip(
                selected = true,
                onClick = { onDateFilterChange(null, null) },
                label = { Text(dateLabel) },
                trailingIcon = {
                    Icon(Icons.Default.Clear, contentDescription = "Clear Date Filter", modifier = Modifier.size(16.dp))
                },
                colors = androidx.compose.material3.InputChipDefaults.inputChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }

    if (showDatePicker) {
        DatePickerFilterDialog(
            onDismiss = { showDatePicker = false },
            onDateRangeSelected = onDateFilterChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationFeedList(
    items: List<NotificationEntity>,
    onItemClick: (NotificationEntity) -> Unit,
    onDeleteItem: (NotificationEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notification_feed_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                        onDeleteItem(item)
                        false
                    } else false
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val color by animateColorAsState(
                        targetValue = if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) MaterialTheme.colorScheme.error else Color.Transparent,
                        label = "dismiss_color"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(color)
                            .padding(horizontal = 20.dp),
                        contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White
                        )
                    }
                }
            ) {
                NotificationCardItem(
                    item = item,
                    onClick = { onItemClick(item) },
                    onDelete = { onDeleteItem(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationDetailBottomSheet(
    notification: NotificationEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onCopy: (String) -> Unit,
    onOpenApp: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = notification.appName.ifBlank { notification.packageName },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = notification.title.ifBlank { "No Title" },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val formattedTime = remember(notification.postTime) {
                val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                sdf.format(Date(notification.postTime))
            }
            Text(
                text = formattedTime,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = notification.content,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onCopy("${notification.title}\n${notification.content}") },
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy")
                }
                
                Button(
                    onClick = { onOpenApp(notification.packageName) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open App")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EmptyStateView(
    searchQuery: String,
    selectedPackage: String?,
    isPermissionGranted: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = when {
                searchQuery.isNotBlank() -> "No matching notifications"
                selectedPackage != null -> "No logs for selected app"
                !isPermissionGranted -> "Waiting for permission"
                else -> "Your NotiBox inbox is clear"
            },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                searchQuery.isNotBlank() -> "Try adjusting your search keywords."
                selectedPackage != null -> "Notifications from this app will appear here once received."
                !isPermissionGranted -> "Enable Notification Access above so NotiBox can record incoming pushes."
                else -> "Incoming notifications (Instagram, WhatsApp, Telegram, etc.) will be parsed and logged here in real-time."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
