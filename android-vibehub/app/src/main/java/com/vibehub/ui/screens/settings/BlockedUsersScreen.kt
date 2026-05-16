package com.vibehub.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibehub.domain.model.User
import com.vibehub.ui.components.VibeAvatar
import com.vibehub.ui.theme.*
import com.vibehub.ui.viewmodel.BlockedUsersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    onBack: () -> Unit,
    viewModel: BlockedUsersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadBlockedUsers() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked Accounts", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VibePink)
            }
        } else if (uiState.users.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Block, null, tint = VibeTextSecondary, modifier = Modifier.size(48.dp))
                    Text("No blocked accounts", style = MaterialTheme.typography.titleMedium)
                    Text("People you block won't see your content", style = MaterialTheme.typography.bodySmall, color = VibeTextSecondary)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(uiState.users, key = { it.id }) { user ->
                    BlockedUserRow(user = user, onUnblock = { viewModel.unblockUser(user.id) })
                }
            }
        }
    }
}

@Composable
private fun BlockedUserRow(user: User, onUnblock: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title   = { Text("Unblock ${user.displayName}?") },
            text    = { Text("They will be able to see your posts and follow you again.") },
            confirmButton = {
                TextButton(onClick = { onUnblock(); showConfirm = false }) {
                    Text("Unblock", color = VibePink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } },
        )
    }

    ListItem(
        headlineContent  = { Text(user.displayName, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text("@${user.username}", color = VibeTextSecondary) },
        leadingContent   = { VibeAvatar(imageUrl = user.avatarUrl, size = 44.dp) },
        trailingContent  = {
            OutlinedButton(
                onClick = { showConfirm = true },
                shape   = RoundedCornerShape(20.dp),
            ) {
                Text("Unblock", style = MaterialTheme.typography.labelMedium)
            }
        },
    )
    HorizontalDivider(thickness = 0.3.dp)
}
