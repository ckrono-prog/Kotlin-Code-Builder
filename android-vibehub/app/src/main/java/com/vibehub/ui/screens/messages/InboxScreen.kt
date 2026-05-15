package com.vibehub.ui.screens.messages

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibehub.domain.model.*
import com.vibehub.ui.components.*
import com.vibehub.ui.theme.*
import com.vibehub.ui.viewmodel.InboxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onOpenChat: (String) -> Unit,
    viewModel: InboxViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Inbox", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row {
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.Edit, contentDescription = "New message")
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search")
                }
            }
        }

        // Search bar
        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = { Text("Search messages") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(28.dp),
            singleLine = true,
        )

        Spacer(Modifier.height(8.dp))

        // Active users row
        if (uiState.conversations.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            VibeAvatar(imageUrl = "", size = 56.dp)
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(VibeGradient),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Your note", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }

                items(uiState.conversations.take(8)) { convo ->
                    val peer = convo.participants.firstOrNull()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onOpenChat(convo.id) },
                    ) {
                        VibeAvatar(imageUrl = peer?.avatarUrl ?: "", size = 56.dp, isLive = false)
                        Spacer(Modifier.height(4.dp))
                        Text(peer?.displayName?.take(8) ?: "", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = MaterialTheme.colorScheme.background,
        ) {
            listOf("Messages", "Requests", "Groups").forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick  = { selectedTab = index },
                    text     = { Text(label, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                    selectedContentColor   = VibePink,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                )
            }
        }

        // Conversation list
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(uiState.conversations, key = { it.id }) { convo ->
                ConversationItem(conversation = convo, onClick = { onOpenChat(convo.id) })
                Divider(
                    modifier = Modifier.padding(start = 80.dp, end = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )
            }

            if (uiState.isLoading) {
                items(6) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        ShimmerBox(modifier = Modifier.size(54.dp).clip(CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f).height(16.dp).clip(RoundedCornerShape(8.dp)))
                            Spacer(Modifier.height(6.dp))
                            ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp).clip(RoundedCornerShape(6.dp)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(conversation: Conversation, onClick: () -> Unit) {
    val peer = conversation.participants.firstOrNull()
    val lastMsg = conversation.lastMessage

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VibeAvatar(
            imageUrl = peer?.avatarUrl ?: conversation.groupAvatarUrl ?: "",
            size = 54.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (conversation.type == ConversationType.GROUP)
                    conversation.groupName ?: "Group"
                else peer?.displayName ?: "",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = lastMsg?.text ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (conversation.unreadCount > 0) 0.9f else 0.5f
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = lastMsg?.createdAt?.take(5) ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = if (conversation.unreadCount > 0) VibePink
                else MaterialTheme.colorScheme.onSurface.copy(0.4f),
            )
            if (conversation.unreadCount > 0) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(VibePink),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = conversation.unreadCount.coerceAtMost(99).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            } else if (conversation.isMuted) {
                Icon(Icons.Outlined.NotificationsOff, null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(0.3f))
            }
        }
    }
}
