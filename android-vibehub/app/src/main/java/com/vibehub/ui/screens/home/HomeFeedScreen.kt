package com.vibehub.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vibehub.domain.model.*
import com.vibehub.ui.components.*
import com.vibehub.ui.theme.*
import com.vibehub.ui.viewmodel.FeedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToReels: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Load more when near bottom
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !uiState.isLoading) viewModel.loadMore()
    }

    val pullState = rememberPullToRefreshState()
    if (pullState.isRefreshing) {
        LaunchedEffect(Unit) {
            viewModel.refresh()
            pullState.endRefresh()
        }
    }

    Box(modifier = Modifier.fillMaxSize().nestedScroll(pullState.nestedScrollConnection)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            // ── Top app bar ────────────────────────────────────────────────
            item {
                FeedTopBar(onNavigateToMessages = {})
            }

            // ── Stories row ───────────────────────────────────────────────
            item {
                StoriesRow(
                    stories = uiState.stories,
                    onStoryClick = { viewModel.markStoryViewed(it.id) },
                    onAddStory = {},
                )
            }

            // ── Create post bar ───────────────────────────────────────────
            item {
                CreatePostBar(avatarUrl = "")
            }

            // ── Posts ─────────────────────────────────────────────────────
            items(uiState.posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    onLike    = { viewModel.toggleLike(post) },
                    onSave    = { viewModel.toggleSave(post) },
                    onComment = {},
                    onShare   = {},
                    onAuthorClick = { onNavigateToProfile(post.authorId) },
                )
            }

            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VibePink, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        PullToRefreshContainer(state = pullState, modifier = Modifier.align(Alignment.TopCenter))
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun FeedTopBar(onNavigateToMessages: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            VibeAvatar(imageUrl = "", size = 40.dp, hasStory = false)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Hello, Armenam 👋", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("What's on your mind?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
        IconButton(onClick = {}) {
            Icon(Icons.Outlined.Search, contentDescription = "Search")
        }
        IconButton(onClick = onNavigateToMessages) {
            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Messages")
        }
    }
}

// ─── Stories Row ─────────────────────────────────────────────────────────────

@Composable
private fun StoriesRow(
    stories: List<Story>,
    onStoryClick: (Story) -> Unit,
    onAddStory: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            StoryCircle(
                imageUrl = "",
                label    = "Your note",
                hasStory = false,
                isAdd    = true,
                onClick  = onAddStory,
            )
        }
        items(stories.take(10), key = { it.id }) { story ->
            StoryCircle(
                imageUrl = story.author.avatarUrl,
                label    = story.author.displayName.take(8),
                hasStory = true,
                isViewed = story.isViewedByMe,
                isLive   = false,
                onClick  = { onStoryClick(story) },
            )
        }
    }
}

@Composable
private fun StoryCircle(
    imageUrl: String,
    label: String,
    hasStory: Boolean,
    isViewed: Boolean = false,
    isLive: Boolean   = false,
    isAdd: Boolean    = false,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box {
            VibeAvatar(imageUrl = imageUrl, size = 64.dp, hasStory = hasStory, isViewed = isViewed, isLive = isLive)
            if (isAdd) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(VibeGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ─── Create Post Bar ──────────────────────────────────────────────────────────

@Composable
private fun CreatePostBar(avatarUrl: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VibeAvatar(imageUrl = avatarUrl, size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {},
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text("Share your thoughts...", modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MediaActionChip(Icons.Outlined.Image, "Photo", Color(0xFF1877F2))
                MediaActionChip(Icons.Outlined.Videocam, "Video", VibePink)
                MediaActionChip(Icons.Outlined.EmojiEmotions, "Feeling", VibeGold)
            }
        }
    }
}

@Composable
private fun MediaActionChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color) {
    Row(
        modifier = Modifier.clickable {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

// ─── Post Card ────────────────────────────────────────────────────────────────

@Composable
fun PostCard(
    post: Post,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onAuthorClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column {
            // Author header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VibeAvatar(imageUrl = post.author.avatarUrl, size = 42.dp,
                    modifier = Modifier.clickable(onClick = onAuthorClick))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f).clickable(onClick = onAuthorClick)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(post.author.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (post.author.isVerified) VerifiedBadge(14.dp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(post.createdAt.take(10), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Icon(Icons.Filled.Public, null, modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.MoreVert, null, modifier = Modifier.size(20.dp))
                }
            }

            // Caption
            if (post.caption.isNotBlank()) {
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 14.dp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                if (post.hashtags.isNotEmpty()) {
                    Text(
                        text = post.hashtags.take(5).joinToString(" ") { "#$it" },
                        style = MaterialTheme.typography.bodySmall,
                        color = VibePink,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            // Media
            if (post.mediaUrls.isNotEmpty()) {
                AsyncImage(
                    model = post.mediaUrls.first(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(post.aspectRatio.coerceIn(0.5f, 1.91f)),
                )
            }

            // Reaction summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier.size(18.dp).clip(CircleShape).background(VibePink),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    }
                    Text(formatCount(post.likesCount), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Text("${formatCount(post.commentsCount)} Comments", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }

            Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PostActionButton(
                    icon  = if (post.isLikedByMe) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    label = "Like",
                    tint  = if (post.isLikedByMe) VibePink else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    onClick = onLike,
                )
                PostActionButton(Icons.Outlined.ChatBubbleOutline, "Comment", onClick = onComment)
                PostActionButton(Icons.Outlined.BookmarkBorder, "Save",
                    tint = if (post.isSavedByMe) VibePink else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    onClick = onSave)
            }

            // First comment preview
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun PostActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = LocalContentColor.current.copy(alpha = 0.6f),
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}
