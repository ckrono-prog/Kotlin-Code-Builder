package com.vibehub.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibehub.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(onBack: () -> Unit) {
    var accountPrivacy     by remember { mutableStateOf("Public") }
    var whoCanSeeStories   by remember { mutableStateOf("Everyone") }
    var whoCanComment      by remember { mutableStateOf("Everyone") }
    var whoCanSeeFollowers by remember { mutableStateOf("Everyone") }
    var whoCanSeeFollowing by remember { mutableStateOf("Everyone") }
    var whoCanDM           by remember { mutableStateOf("Everyone") }
    var showActivityStatus by remember { mutableStateOf(true) }
    var showReadReceipts   by remember { mutableStateOf(true) }
    var allowTagging       by remember { mutableStateOf(true) }
    var allowMentions      by remember { mutableStateOf(true) }
    var twoFAEnabled       by remember { mutableStateOf(false) }
    var loginAlerts        by remember { mutableStateOf(true) }

    val audienceOptions = listOf("Everyone", "Friends", "Friends of Friends", "Only Me")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { SettingsSectionHeader("Account") }
            item {
                SettingsGroup {
                    PrivacyChooserRow(
                        icon    = Icons.Outlined.LockOpen,
                        label   = "Account Privacy",
                        current = accountPrivacy,
                        options = listOf("Public", "Private"),
                        onSelect = { accountPrivacy = it },
                    )
                }
            }

            item { SettingsSectionHeader("Posts & Content") }
            item {
                SettingsGroup {
                    PrivacyChooserRow(
                        icon    = Icons.Outlined.Visibility,
                        label   = "Who can see your posts",
                        current = whoCanSeeStories,
                        options = audienceOptions,
                        onSelect = { whoCanSeeStories = it },
                    )
                    PrivacyChooserRow(
                        icon    = Icons.Outlined.Comment,
                        label   = "Who can comment",
                        current = whoCanComment,
                        options = audienceOptions + listOf("No One"),
                        onSelect = { whoCanComment = it },
                    )
                    PrivacyChooserRow(
                        icon    = Icons.Outlined.Tag,
                        label   = "Who can tag you",
                        current = if (allowTagging) "Everyone" else "No One",
                        options = audienceOptions,
                        onSelect = { allowTagging = it != "No One" },
                    )
                    PrivacyChooserRow(
                        icon    = Icons.Outlined.AlternateEmail,
                        label   = "Who can mention you",
                        current = if (allowMentions) "Everyone" else "No One",
                        options = audienceOptions,
                        onSelect = { allowMentions = it != "No One" },
                    )
                }
            }

            item { SettingsSectionHeader("Profile") }
            item {
                SettingsGroup {
                    PrivacyChooserRow(
                        icon    = Icons.Outlined.Group,
                        label   = "Who can see your followers",
                        current = whoCanSeeFollowers,
                        options = audienceOptions,
                        onSelect = { whoCanSeeFollowers = it },
                    )
                    PrivacyChooserRow(
                        icon    = Icons.Outlined.PersonSearch,
                        label   = "Who can see who you follow",
                        current = whoCanSeeFollowing,
                        options = audienceOptions,
                        onSelect = { whoCanSeeFollowing = it },
                    )
                }
            }

            item { SettingsSectionHeader("Messages") }
            item {
                SettingsGroup {
                    PrivacyChooserRow(
                        icon    = Icons.Outlined.Message,
                        label   = "Who can send you DMs",
                        current = whoCanDM,
                        options = listOf("Everyone", "Friends", "No One"),
                        onSelect = { whoCanDM = it },
                    )
                    SettingsToggleRow(Icons.Outlined.Visibility, "Show Activity Status", showActivityStatus) { showActivityStatus = it }
                    SettingsToggleRow(Icons.Outlined.DoneAll, "Read Receipts (Seen)", showReadReceipts) { showReadReceipts = it }
                }
            }

            item { SettingsSectionHeader("Security") }
            item {
                SettingsGroup {
                    SettingsToggleRow(Icons.Outlined.VerifiedUser, "Two-Factor Authentication", twoFAEnabled) { twoFAEnabled = it }
                    SettingsToggleRow(Icons.Outlined.Notifications, "Login Alerts", loginAlerts) { loginAlerts = it }
                    SettingsRow(Icons.Outlined.Devices, "Manage Devices", onClick = {})
                    SettingsRow(Icons.Outlined.History, "Login Activity", onClick = {})
                }
            }

            item { SettingsSectionHeader("Data") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.Download, "Download Your Data", onClick = {})
                    SettingsRow(Icons.Outlined.DeleteForever, "Delete Account", onClick = {})
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacyChooserRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    current: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        ListItem(
            headlineContent = { Text(label) },
            leadingContent  = { Icon(icon, null, tint = VibePink) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(current, style = MaterialTheme.typography.bodySmall, color = VibePink)
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier.menuAnchor().clickable { expanded = true },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(option, color = if (option == current) VibePink
                        else MaterialTheme.colorScheme.onSurface)
                    },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}
