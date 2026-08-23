package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.data.LifespanPurge
import com.example.data.RetentionPreferences
import kotlinx.coroutines.launch

import com.example.data.StorageCap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetentionSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { RetentionPreferences(context) }
    
    val lifespan by prefs.lifespanPurge.collectAsState(initial = LifespanPurge.NEVER)
    val protectRevoked by prefs.protectRevoked.collectAsState(initial = true)
    val storageCap by prefs.storageCap.collectAsState(initial = StorageCap.GB_5)
    
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage & Retention") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Lifespan Purge
            Column {
                Text(
                    text = "Auto-Delete Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val options = listOf(
                    LifespanPurge.DAYS_3 to "After 3 Days",
                    LifespanPurge.DAYS_7 to "After 7 Days",
                    LifespanPurge.DAYS_30 to "After 30 Days",
                    LifespanPurge.NEVER to "Never"
                )
                
                Column(Modifier.selectableGroup()) {
                    options.forEach { (option, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (option == lifespan),
                                    onClick = { prefs.setLifespanPurge(option) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (option == lifespan),
                                onClick = null // null recommended for accessibility with row click
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
            
            HorizontalDivider()

            // Protect Revoked
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = "Protect Revoked Messages",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Never auto-delete messages that were unsent or deleted by the sender.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = protectRevoked,
                    onCheckedChange = { prefs.setProtectRevoked(it) }
                )
            }

            HorizontalDivider()

            // Storage Cap
            Column {
                Text(
                    text = "Storage Cap",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "If database exceeds this limit, oldest entries will be purged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val capOptions = listOf(
                    StorageCap.MB_500,
                    StorageCap.GB_1,
                    StorageCap.GB_2,
                    StorageCap.GB_5
                )
                
                Column(Modifier.selectableGroup()) {
                    capOptions.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (option == storageCap),
                                    onClick = { prefs.setStorageCap(option) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (option == storageCap),
                                onClick = null
                            )
                            Text(
                                text = option.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
            
        }
    }
}
