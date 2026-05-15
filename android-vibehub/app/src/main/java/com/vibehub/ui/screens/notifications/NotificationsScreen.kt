package com.vibehub.ui.screens.notifications

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibehub.domain.model.*
import com.vibehub.ui.components.*
import com.vibehub.ui.theme.*
import com.vibehub.ui.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateToProfile: (String) -> Unit,
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Group notifications by date
    val grouped = uiState.notifications.groupBy {
        when {
            it.createdAt.contains("T") -> it.createdAt.substringBefore("T")
            else -> "Earlier"
        }
    }

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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Notifications", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (uiState.unreadCount > 0) {
                    Badge {
                        Text(uiState.unreadCount.coerceAtMost(99).toString())
                    }
                }
            }
            TextButton(onClick = { viewModel.markAllRead() }) {
                Text("Mark all read", color = VibePink, style = MaterialTheme.typography.labelMedium)
            }
        }

        // Filter chips
        var selectedFilter by remember { mutableIntStateOf(0) }
        val filters = listOf("All", "Likes", "Comments", "Follows", "Mentions")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(filters) { index, filter ->
                FilterChip(
                    selected = selectedFilter == index,
                    onClick  = { selectedFilter = index },
                    label    = { Text(filter) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VibePink,
                        selectedLabelColor     = Color.White,
                    ),
                    shape = RoundedCornerShape(50),
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            if (uiState.isLoading) {
                items(8) {
                    NotificationShimmer()
                }
            } else {
                grouped.forEach { (date, notifications) ->
                    stickyHeader {
                        Text(
                            text = formatDateLabel(date),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(notifications, key = { it.id }) { notif ->
                        NotificationItem(
                            notification = notif,
                            onClick = {
                                viewModel.markRead(notif.id)
                                onNavigateToProfile(notif.actorId)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(notification: Notification, onClick: () -> Unit) {
    val isUnread = !notification.isRead

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isUnread) VibePink.copy(alpha = 0.05f)
                else MaterialTheme.colorScheme.background
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar with notification type icon badge
        Box {
            VibeAvatar(imageUrl = notification.actor.avatarUrl, size = 46.dp)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(notifIconBg(notification.type)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(notifIcon(notification.type), null, tint = Color.White, modifier = Modifier.size(11.dp))
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedNotifText(notification),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = notification.createdAt.take(10),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
            )
        }

        // Unread dot
        if (isUnread) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(VibePink),
            )
        }
    }
}

private fun buildAnnotatedNotifText(notification: Notification): String {
    val actor = notification.actor.displayName.ifBlank { "@${notification.actor.username}" }
    return when (notification.type) {
        NotificationType.LIKE     -> "$actor liked your post"
        NotificationType.COMMENT  -> "$actor commented on your post"
        NotificationType.FOLLOW   -> "$actor started following you"
        NotificationType.MENTION  -> "$actor mentioned you in a comment"
        NotificationType.SHARE    -> "$actor shared your post"
        NotificationType.LIVE     -> "$actor started a live stream"
        NotificationType.STORY_REACT -> "$actor reacted to your story"
        NotificationType.GIFT     -> "$actor sent you a gift"
        NotificationType.PURCHASE -> "$actor purchased your product"
        NotificationType.SYSTEM   -> notification.text
        NotificationType.CHALLENGE -> "$actor challenged you"
    }
}

private fun notifIcon(type: NotificationType): ImageVector = when (type) {
    NotificationType.LIKE      -> Icons.Filled.Favorite
    NotificationType.COMMENT   -> Icons.Filled.Comment
    NotificationType.FOLLOW    -> Icons.Filled.PersonAdd
    NotificationType.MENTION   -> Icons.Filled.AlternateEmail
    NotificationType.SHARE     -> Icons.Filled.Share
    NotificationType.LIVE      -> Icons.Filled.LiveTv
    NotificationType.STORY_REACT -> Icons.Filled.AutoAwesome
    NotificationType.GIFT      -> Icons.Filled.CardGiftcard
    NotificationType.PURCHASE  -> Icons.Filled.ShoppingBag
    NotificationType.SYSTEM    -> Icons.Filled.Info
    NotificationType.CHALLENGE -> Icons.Filled.EmojiEvents
}

private fun notifIconBg(type: NotificationType): Color = when (type) {
    NotificationType.LIKE      -> VibePink
    NotificationType.COMMENT   -> Color(0xFF1877F2)
    NotificationType.FOLLOW    -> VibeSuccess
    NotificationType.MENTION   -> VibeOrange
    NotificationType.SHARE     -> Color(0xFF00BCD4)
    NotificationType.LIVE      -> Color(0xFFE91E63)
    NotificationType.STORY_REACT -> VibeNeonPurple
    NotificationType.GIFT      -> VibeGold
    NotificationType.PURCHASE  -> Color(0xFF4CAF50)
    NotificationType.SYSTEM    -> Color(0xFF9E9E9E)
    NotificationType.CHALLENGE -> Color(0xFFFF9800)
}

@Composable
private fun NotificationShimmer() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShimmerBox(modifier = Modifier.size(46.dp).clip(CircleShape))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(7.dp)))
            Spacer(Modifier.height(6.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.3f).height(10.dp).clip(RoundedCornerShape(5.dp)))
        }
    }
}

private fun formatDateLabel(date: String): String = when {
    date == "Earlier" -> "Earlier"
    else              -> date
}
