package com.example.studymateai.ui.screen.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.studymateai.R
import kotlinx.coroutines.launch

enum class ThemeOption(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Theme"),
    DARK("Dark Theme")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // State variables
    var notificationsEnabled by remember { mutableStateOf(true) }
    var selectedTheme by remember { mutableStateOf(ThemeOption.SYSTEM) }
    var showThemeOptions by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Theme Section
            item {
                SettingsCategory(title = "Appearance")
                SettingItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.theme),
                            contentDescription = "theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = "Theme",
                    subtitle = selectedTheme.label,
                    onClick = { showThemeOptions = true }
                )
            }

//            // Preferences Section
//            item {
//                SettingsCategory(title = "Preferences")
//                SettingItem(
//                    icon = {Icons.Default.Notifications},
//                    title = "Notifications",
//                    subtitle = "Enable/disable app notifications"
//                ) {
//                    Switch(
//                        checked = notificationsEnabled,
//                        onCheckedChange = { notificationsEnabled = it }
//                    )
//                }
//            }

            // Storage Section
            item {
                SettingsCategory(title = "Storage")
                SettingItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.storage),
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                    title = "Clear Cache",
                    subtitle = "Free up 24.5 MB of space",
                    onClick = { showClearCacheDialog = true }
                )
            }

            // Account Section
            item {
                SettingsCategory(title = "Account")
                SettingItem(
                    icon = {Icons.Default.Delete},
                    title = "Delete Account",
                    subtitle = "Permanently remove your account",
                    onClick = { showDeleteDialog = true }
                )
            }

            // About Section
            item {
                SettingsCategory(title = "About")
                SettingItem(
                    icon = {Icons.Default.Info},
                    title = "About StudyMate AI",
                    subtitle = "App version 1.0.0",
                    onClick = { /* Show about dialog */ }
                )

                SettingItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.language),
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = "Language",
                    subtitle = "English (US)",
                    onClick = { /* Show language options */ }
                )
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Theme Selection Dialog
    if (showThemeOptions) {
        ThemeSelectionDialog(
            currentTheme = selectedTheme,
            onThemeSelected = { theme ->
                selectedTheme = theme
                showThemeOptions = false
                // Implement theme change logic
            },
            onDismiss = { showThemeOptions = false }
        )
    }

    // Delete Account Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?") },
            text = { Text("Are you sure you want to permanently delete your account? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        // Implement account deletion logic
                        scope.launch {
                            snackbarHostState.showSnackbar("Account deleted successfully")
                            navController.navigate("login") { popUpTo(0) }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Cache Confirmation Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Cache?") },
            text = { Text("This will free up storage space but won't delete your personal data.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        // Implement cache clearing logic
                        scope.launch {
                            snackbarHostState.showSnackbar("Cache cleared successfully")
                        }
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearCacheDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
    Divider()
}

@Composable
fun SettingItem(
    icon: @Composable () -> Unit, // Changed to composable function for custom icons
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (action != null) {
            action()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Go to $title",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Divider(thickness = 0.5.dp)
}

@Composable
fun ThemeSelectionDialog(
    currentTheme: ThemeOption,
    onThemeSelected: (ThemeOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            Column(Modifier.selectableGroup()) {
                ThemeOption.values().forEach { theme ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (theme == currentTheme),
                                onClick = { onThemeSelected(theme) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (theme == currentTheme),
                            onClick = null
                        )
                        Text(
                            text = theme.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}