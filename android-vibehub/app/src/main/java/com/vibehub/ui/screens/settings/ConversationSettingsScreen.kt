package com.vibehub.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.vibehub.domain.model.User
import com.vibehub.ui.components.GradientButton
import com.vibehub.ui.components.VibeAvatar
import com.vibehub.ui.components.VerifiedBadge
import com.vibehub.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationSettingsScreen(
    remoteUser: User,
    onBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit,
    onDeleteConversation: () -> Unit,
    onArchive: () -> Unit,
) {
    var nickname by remember { mutableStateOf(remoteUser.displayName) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var disappearingMessages by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var selectedTheme by remember { mutableStateOf("Default") }

    if (showNicknameDialog) {
        NicknameDialog(
            current = nickname,
            userName = remoteUser.displayName,
            onDismiss = { showNicknameDialog = false },
            onConfirm = { newNickname ->
                nickname = newNickname
                showNicknameDialog = false
            },
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete Chat?",
            body  = "This will permanently delete all messages in this conversation. This cannot be undone.",
            confirmText = "Delete",
            onConfirm = { onDeleteConversation(); showDeleteDialog = false },
            onDismiss = { showDeleteDialog = false },
            isDestructive = true,
        )
    }

    if (showBlockDialog) {
        ConfirmDialog(
            title = "Block @${remoteUser.username}?",
            body  = "They won't be able to message you or view your profile. You can unblock them anytime from Settings.",
            confirmText = "Block",
            onConfirm = { onBlock(); showBlockDialog = false },
            onDismiss = { showBlockDialog = false },
            isDestructive = true,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Info", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            // User card
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    VibeAvatar(imageUrl = remoteUser.avatarUrl, size = 90.dp)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (nickname != remoteUser.displayName) "$nickname (${remoteUser.displayName})"
                            else remoteUser.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (remoteUser.isVerified) VerifiedBadge(16.dp)
                    }
                    Text("@${remoteUser.username}", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { onNavigateToProfile(remoteUser.id) }, shape = RoundedCornerShape(20.dp)) {
                            Icon(Icons.Outlined.Person, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("View Profile")
                        }
                        OutlinedButton(onClick = {}, shape = RoundedCornerShape(20.dp)) {
                            Icon(Icons.Outlined.Videocam, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Video Call")
                        }
                    }
                }
            }

            // Nickname
            item { SettingsSectionHeader("Nickname") }
            item {
                SettingsGroup {
                    ListItem(
                        headlineContent = { Text("Set Nickname") },
                        supportingContent = { Text(nickname, color = VibePink) },
                        leadingContent = { Icon(Icons.Outlined.Edit, null, tint = VibePink) },
                        trailingContent = { Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.3f)) },
                        modifier = Modifier.clickable { showNicknameDialog = true },
                    )
                }
            }

            // Chat preferences
            item { SettingsSectionHeader("Chat Preferences") }
            item {
                SettingsGroup {
                    SettingsToggleRow(Icons.Outlined.NotificationsOff, "Mute Notifications", isMuted) { isMuted = it }
                    SettingsToggleRow(Icons.Outlined.Timer, "Disappearing Messages", disappearingMessages) { disappearingMessages = it }
                    SettingsRow(Icons.Outlined.Palette, "Chat Theme", trailing = selectedTheme) {}
                    SettingsRow(Icons.Outlined.EmojiEmotions, "Custom Reactions", onClick = {})
                }
            }

            // Media
            item { SettingsSectionHeader("Shared Media") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.Photo, "Photos & Videos", onClick = {})
                    SettingsRow(Icons.Outlined.Link, "Links", onClick = {})
                    SettingsRow(Icons.Outlined.AudioFile, "Voice Notes", onClick = {})
                }
            }

            // Danger zone
            item { SettingsSectionHeader("Actions") }
            item {
                SettingsGroup {
                    ListItem(
                        headlineContent = { Text("Archive Chat") },
                        leadingContent = { Icon(Icons.Outlined.Archive, null, tint = VibeOrange) },
                        modifier = Modifier.clickable(onClick = onArchive),
                    )
                    ListItem(
                        headlineContent = { Text("Delete All Messages", color = VibeError) },
                        leadingContent = { Icon(Icons.Outlined.DeleteSweep, null, tint = VibeError) },
                        modifier = Modifier.clickable { showDeleteDialog = true },
                    )
                    ListItem(
                        headlineContent = { Text("Block @${remoteUser.username}", color = VibeError) },
                        leadingContent = { Icon(Icons.Outlined.Block, null, tint = VibeError) },
                        modifier = Modifier.clickable { showBlockDialog = true },
                    )
                    ListItem(
                        headlineContent = { Text("Report", color = VibeError.copy(0.8f)) },
                        leadingContent = { Icon(Icons.Outlined.Flag, null, tint = VibeError.copy(0.8f)) },
                        modifier = Modifier.clickable(onClick = onReport),
                    )
                }
            }
        }
    }
}

@Composable
private fun NicknameDialog(
    current: String,
    userName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon    = { Icon(Icons.Outlined.Edit, null, tint = VibePink) },
        title   = { Text("Set Nickname") },
        text    = {
            Column {
                Text("Give $userName a custom nickname visible only to you.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("Nickname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                if (text.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { text = "" }) { Text("Remove Nickname", color = VibeError) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.ifBlank { userName }) }) {
                Text("Save", color = VibePink, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text  = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = if (isDestructive) VibeError else VibePink, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
