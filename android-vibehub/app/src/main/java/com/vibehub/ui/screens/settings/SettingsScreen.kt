package com.vibehub.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.vibehub.ui.components.GradientText
import com.vibehub.ui.components.VibeAvatar
import com.vibehub.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToBlocked: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onLogout: () -> Unit,
) {
    var darkModeEnabled by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            // Profile card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).clickable { },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VibeAvatar(imageUrl = "", size = 60.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Armenam", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("@armenam244", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            Text("View and edit profile →", style = MaterialTheme.typography.labelSmall, color = VibePink)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    }
                }
            }

            // Account section
            item { SettingsSectionHeader("Account") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.Person, "Personal Information", onClick = {})
                    SettingsRow(Icons.Outlined.Lock, "Password & Security", onClick = onNavigateToSecurity)
                    SettingsRow(Icons.Outlined.Email, "Email & Phone", onClick = {})
                    SettingsRow(Icons.Outlined.Shield, "Privacy", onClick = onNavigateToPrivacy, showBadge = false)
                    SettingsRow(Icons.Outlined.Block, "Blocked Accounts", onClick = onNavigateToBlocked)
                }
            }

            // Preferences section
            item { SettingsSectionHeader("Preferences") }
            item {
                SettingsGroup {
                    SettingsToggleRow(
                        icon = Icons.Outlined.DarkMode,
                        label = "Dark Mode",
                        checked = darkModeEnabled,
                        onToggle = { darkModeEnabled = it },
                    )
                    SettingsToggleRow(
                        icon = Icons.Outlined.Notifications,
                        label = "Push Notifications",
                        checked = notificationsEnabled,
                        onToggle = { notificationsEnabled = it },
                    )
                    SettingsRow(Icons.Outlined.Palette, "Appearance & Theme", onClick = onNavigateToAppearance)
                    SettingsRow(Icons.Outlined.Language, "Language", trailing = "English", onClick = {})
                    SettingsRow(Icons.Outlined.Download, "Download & Storage", onClick = {})
                }
            }

            // Content section
            item { SettingsSectionHeader("Content & Display") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.AutoFixHigh, "AI Personalization", onClick = {})
                    SettingsRow(Icons.Outlined.AccessibilityNew, "Accessibility", onClick = {})
                    SettingsRow(Icons.Outlined.ClosedCaption, "Captions & Subtitles", onClick = {})
                    SettingsRow(Icons.Outlined.DataSaverOn, "Data Usage", onClick = {})
                }
            }

            // Support section
            item { SettingsSectionHeader("Support") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.Help, "Help Center", onClick = onNavigateToHelp)
                    SettingsRow(Icons.Outlined.BugReport, "Report a Problem", onClick = {})
                    SettingsRow(Icons.Outlined.Info, "About VibeHub", trailing = "1.0.0", onClick = {})
                    SettingsRow(Icons.Outlined.Description, "Terms & Privacy Policy", onClick = {})
                }
            }

            // Logout
            item {
                Spacer(Modifier.height(8.dp))
                SettingsGroup {
                    ListItem(
                        headlineContent = {
                            Text("Log Out", color = VibeError, fontWeight = FontWeight.SemiBold)
                        },
                        leadingContent = {
                            Icon(Icons.Outlined.Logout, null, tint = VibeError)
                        },
                        modifier = Modifier.clickable(onClick = onLogout),
                    )
                    ListItem(
                        headlineContent = {
                            Text("Delete Account", color = VibeError.copy(0.7f))
                        },
                        leadingContent = {
                            Icon(Icons.Outlined.PersonRemove, null, tint = VibeError.copy(0.7f))
                        },
                        modifier = Modifier.clickable {},
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(0.45f),
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    label: String,
    trailing: String? = null,
    showBadge: Boolean = false,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent  = { Icon(icon, null, tint = VibePink) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (trailing != null) {
                    Text(trailing, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                }
                if (showBadge) {
                    Badge { Text("New") }
                }
                Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.3f),
                    modifier = Modifier.size(18.dp))
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent  = { Icon(icon, null, tint = VibePink) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VibePink),
            )
        },
    )
}
