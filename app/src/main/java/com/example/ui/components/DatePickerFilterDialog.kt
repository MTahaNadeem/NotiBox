package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerFilterDialog(
    onDismiss: () -> Unit,
    onDateRangeSelected: (Long?, Long?) -> Unit
) {
    val datePickerState = rememberDatePickerState()
    var showCustomPicker by remember { mutableStateOf(false) }

    if (showCustomPicker) {
        DatePickerDialog(
            onDismissRequest = { showCustomPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        // Start of day
                        val start = getStartOfDay(it)
                        // End of day
                        val end = getEndOfDay(it)
                        onDateRangeSelected(start, end)
                    }
                    showCustomPicker = false
                    onDismiss()
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Filter by Date") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterPresetButton("Today") {
                        val start = getStartOfDay(System.currentTimeMillis())
                        val end = getEndOfDay(System.currentTimeMillis())
                        onDateRangeSelected(start, end)
                        onDismiss()
                    }
                    FilterPresetButton("Yesterday") {
                        val yesterday = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                        val start = getStartOfDay(yesterday)
                        val end = getEndOfDay(yesterday)
                        onDateRangeSelected(start, end)
                        onDismiss()
                    }
                    FilterPresetButton("Past 7 Days") {
                        val end = System.currentTimeMillis()
                        val start = getStartOfDay(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
                        onDateRangeSelected(start, end)
                        onDismiss()
                    }
                    FilterPresetButton("Custom Date...") {
                        showCustomPicker = true
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FilterPresetButton(text: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text)
    }
}

private fun getStartOfDay(timeMillis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun getEndOfDay(timeMillis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    return calendar.timeInMillis
}
