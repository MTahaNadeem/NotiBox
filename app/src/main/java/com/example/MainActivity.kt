package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.ThemeMode
import com.example.ui.NotiBoxScreen
import com.example.ui.NotificationViewModel
import com.example.ui.analytics.AnalyticsScreen
import com.example.ui.analytics.AnalyticsViewModel
import com.example.ui.settings.RetentionSettingsScreen
import com.example.ui.theme.NotiBoxTheme

class MainActivity : ComponentActivity() {

    private val notificationViewModel: NotificationViewModel by viewModels()

    private val exportDbLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri ->
        uri?.let { notificationViewModel.exportDatabase(it) }
    }

    private val exportCsvLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { notificationViewModel.exportCsv(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by notificationViewModel.themeMode.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            
            NotiBoxTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route

                            val colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )

                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Notifications, contentDescription = "Log") },
                                label = { Text("Log") },
                                selected = currentRoute == "log",
                                onClick = {
                                    navController.navigate("log") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = colors
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Analytics, contentDescription = "Analytics") },
                                label = { Text("Analytics") },
                                selected = currentRoute == "analytics",
                                onClick = {
                                    navController.navigate("analytics") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = colors
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                selected = currentRoute == "settings",
                                onClick = {
                                    navController.navigate("settings") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = colors
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = "log",
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        composable("log") {
                            NotiBoxScreen(
                                viewModel = notificationViewModel,
                                onExportDbRequested = {
                                    exportDbLauncher.launch("NotiBox_Backup.db")
                                },
                                onExportCsvRequested = {
                                    exportCsvLauncher.launch("NotiBox_Export.csv")
                                }
                            )
                        }
                        composable("analytics") {
                            val db = AppDatabase.getDatabase(applicationContext)
                            val analyticsViewModel: AnalyticsViewModel = viewModel(
                                factory = object : ViewModelProvider.Factory {
                                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                        @Suppress("UNCHECKED_CAST")
                                        return AnalyticsViewModel(db.notificationDao()) as T
                                    }
                                }
                            )
                            AnalyticsScreen(viewModel = analyticsViewModel, onBack = { navController.popBackStack() })
                        }
                        composable("settings") {
                            RetentionSettingsScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        notificationViewModel.checkPermission()
    }
}
