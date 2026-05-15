package com.vibehub.ui.screens.reels

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vibehub.domain.model.Post
import com.vibehub.ui.components.*
import com.vibehub.ui.theme.*
import com.vibehub.ui.viewmodel.FeedViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelsScreen(
    onNavigateToProfile: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val reels = uiState.posts.filter { it.mediaType.name == "REEL" }

    if (reels.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VibePink)
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { reels.size })

    VerticalPager(
        state     = pagerState,
        modifier  = Modifier.fillMaxSize(),
        beyondBoundsPageCount = 1,
    ) { page ->
        ReelItem(
            post = reels[page],
            onLike    = { viewModel.toggleLike(reels[page]) },
            onSave    = { viewModel.toggleSave(reels[page]) },
            onComment = {},
            onShare   = {},
            onAuthorClick = { onNavigateToProfile(reels[page].authorId) },
        )
    }
}

@Composable
private fun ReelItem(
    post: Post,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onAuthorClick: () -> Unit,
) {
    var isMuted by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Video / thumbnail
        AsyncImage(
            model = post.reelsData?.thumbnailUrl ?: post.mediaUrls.firstOrNull(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Bottom gradient scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.85f)))
                ),
        )

        // Right action column
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Author avatar
            Box(modifier = Modifier.clickable(onClick = onAuthorClick)) {
                VibeAvatar(imageUrl = post.author.avatarUrl, size = 44.dp)
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 8.dp)
                        .clip(CircleShape)
                        .background(VibeGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }

            Spacer(Modifier.height(4.dp))

            // Like
            ReelActionButton(
                icon  = if (post.isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                label = formatCount(post.likesCount),
                tint  = if (post.isLikedByMe) VibePink else Color.White,
                onClick = onLike,
            )

            // Comment
            ReelActionButton(
                icon  = Icons.Outlined.ChatBubbleOutline,
                label = formatCount(post.commentsCount),
                tint  = Color.White,
                onClick = onComment,
            )

            // Share
            ReelActionButton(
                icon  = Icons.Outlined.Share,
                label = "Share",
                tint  = Color.White,
                onClick = onShare,
            )

            // Save
            ReelActionButton(
                icon  = if (post.isSavedByMe) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                label = "Save",
                tint  = if (post.isSavedByMe) VibeGold else Color.White,
                onClick = onSave,
            )

            // Mute
            ReelActionButton(
                icon    = if (isMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                label   = "",
                tint    = Color.White,
                onClick = { isMuted = !isMuted },
            )
        }

        // Bottom info: author + caption + music
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.82f)
                .padding(start = 16.dp, bottom = 80.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "@${post.author.username}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.clickable(onClick = onAuthorClick),
                )
                if (post.author.isVerified) VerifiedBadge(14.dp)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text  = post.caption,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (post.hashtags.isNotEmpty()) {
                Text(
                    text  = post.hashtags.take(3).joinToString(" ") { "#$it" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(0.75f),
                )
            }
            Spacer(Modifier.height(8.dp))
            // Music ticker
            post.musicTrack?.let { track ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text("${track.title} - ${track.artist}", style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Top: VibeHub logo + Reels label
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Reels", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.CameraAlt, null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun ReelActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
        if (label.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}
