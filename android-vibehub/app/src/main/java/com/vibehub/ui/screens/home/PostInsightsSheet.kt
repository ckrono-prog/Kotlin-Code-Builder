package com.vibehub.ui.screens.home

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.vibehub.domain.model.User
import com.vibehub.ui.components.VibeAvatar
import com.vibehub.ui.components.formatLong
import com.vibehub.ui.theme.*

data class PostReactionBreakdown(val emoji: String, val count: Int)
data class PostViewer(val user: User, val viewedAt: String)

/**
 * Bottom sheet shown to post owners — views breakdown, top reactions, viewer list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostInsightsSheet(
    viewCount: Long,
    likeCount: Int,
    commentCount: Int,
    shareCount: Int,
    saveCount: Int,
    reactions: List<PostReactionBreakdown>,
    recentViewers: List<PostViewer>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text(
                "Post Insights",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                InsightStat(Icons.Outlined.Visibility, formatLong(viewCount), "Views")
                InsightStat(Icons.Outlined.FavoriteBorder, formatLong(likeCount.toLong()), "Likes")
                InsightStat(Icons.Outlined.ChatBubbleOutline, formatLong(commentCount.toLong()), "Comments")
                InsightStat(Icons.Outlined.Share, formatLong(shareCount.toLong()), "Shares")
                InsightStat(Icons.Outlined.BookmarkBorder, formatLong(saveCount.toLong()), "Saves")
            }

            HorizontalDivider(thickness = 0.5.dp)

            // Tabs
            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Reactions") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Viewers") })
            }

            if (tab == 0) {
                // Reactions breakdown
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    if (reactions.isEmpty()) {
                        item { EmptyInsightsHint("No reactions yet") }
                    } else {
                        items(reactions) { r ->
                            ListItem(
                                headlineContent   = { Text(r.emoji, style = MaterialTheme.typography.headlineSmall) },
                                trailingContent   = { Text(formatLong(r.count.toLong()), fontWeight = FontWeight.Bold, color = VibePink) },
                                supportingContent = {
                                    LinearProgressIndicator(
                                        progress  = { r.count / (reactions.maxOf { it.count }.toFloat()) },
                                        modifier  = Modifier.fillMaxWidth(0.8f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                        color     = VibePink,
                                        trackColor = MaterialTheme.colorScheme.outline.copy(0.15f),
                                    )
                                },
                            )
                        }
                    }
                }
            } else {
                // Viewer list
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    if (recentViewers.isEmpty()) {
                        item { EmptyInsightsHint("No viewer data yet") }
                    } else {
                        items(recentViewers) { v ->
                            ListItem(
                                headlineContent   = { Text(v.user.displayName, fontWeight = FontWeight.Medium) },
                                supportingContent = { Text(v.viewedAt, style = MaterialTheme.typography.labelSmall, color = VibeTextMuted) },
                                leadingContent    = { VibeAvatar(imageUrl = v.user.avatarUrl, size = 40.dp) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightStat(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, null, tint = VibePink, modifier = Modifier.size(20.dp))
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.labelSmall, color = VibeTextMuted)
    }
}

@Composable
private fun EmptyInsightsHint(msg: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(msg, color = VibeTextMuted, style = MaterialTheme.typography.bodyMedium)
    }
}
