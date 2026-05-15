package com.vibehub.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import com.vibehub.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.user

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Cover + Avatar header
        item {
            ProfileHeader(
                user         = user,
                isOwnProfile = uiState.isOwnProfile,
                onBack       = onBack,
                onFollow     = { viewModel.followUser() },
                onUnfollow   = { viewModel.unfollowUser() },
                onMessage    = { onNavigateToChat("placeholder-convoid") },
            )
        }

        // Bio + meta
        if (user != null) {
            item {
                ProfileBioSection(user)
            }
        }

        // Highlights
        if (uiState.highlights.isNotEmpty()) {
            item {
                HighlightsRow(highlights = uiState.highlights)
            }
        }

        // Tab bar
        item {
            ProfileTabBar(
                selectedTab  = uiState.selectedTab,
                onTabSelected = { viewModel.setTab(it) },
            )
        }

        // Grid of posts/reels/saved
        when (uiState.selectedTab) {
            ProfileTab.POSTS -> items(uiState.posts.chunked(3)) { row ->
                GridRow(row)
            }
            ProfileTab.REELS -> items(uiState.reels.chunked(3)) { row ->
                GridRow(row)
            }
            ProfileTab.SAVED -> items(uiState.savedPosts.chunked(3)) { row ->
                GridRow(row)
            }
        }

        // Empty state
        if (uiState.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VibePink, modifier = Modifier.size(32.dp))
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─── Profile Header ──────────────────────────────────────────────────────────

@Composable
private fun ProfileHeader(
    user: User?,
    isOwnProfile: Boolean,
    onBack: () -> Unit,
    onFollow: () -> Unit,
    onUnfollow: () -> Unit,
    onMessage: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
        // Cover image
        AsyncImage(
            model = user?.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Gradient scrim
        Box(modifier = Modifier.fillMaxSize().background(
            androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(Color.Black.copy(0.3f), Color.Black.copy(0.6f))
            )
        ))

        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp),
        ) {
            Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
        }

        // Three-dot menu
        IconButton(
            onClick = {},
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp),
        ) {
            Icon(Icons.Filled.MoreVert, null, tint = Color.White)
        }

        // Avatar + stats overlay
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box {
                AsyncImage(
                    model = user?.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .border(3.dp, VibeGradient, CircleShape)
                        .background(VibeCardDark),
                )
                // Online dot
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(VibeSuccess)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(user?.displayName ?: "Loading...", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = Color.White)
                    if (user?.isVerified == true) VerifiedBadge(16.dp)
                }
                Text("@${user?.username ?: ""}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
                if (user?.isFollowedByMe == true) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color.White.copy(0.15f)) {
                        Text("Follows you", style = MaterialTheme.typography.labelSmall,
                            color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }
    }

    // Stat row
    if (user != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 16.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatChip(user.postsCount,    "Posts")
            VerticalDivider(modifier = Modifier.height(36.dp))
            StatChip(user.followersCount,"Followers")
            VerticalDivider(modifier = Modifier.height(36.dp))
            StatChip(user.followingCount,"Following")
        }

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isOwnProfile) {
                OutlineVibeButton("Edit Profile", {}, modifier = Modifier.weight(1f))
            } else {
                if (user.isFollowedByMe) {
                    OutlineVibeButton("Following", onUnfollow, modifier = Modifier.weight(1f))
                } else {
                    GradientButton("Follow", onFollow, modifier = Modifier.weight(1f))
                }
                OutlineVibeButton("Message", onMessage, modifier = Modifier.weight(1f))
                OutlinedIconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.PersonAdd, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ─── Bio Section ─────────────────────────────────────────────────────────────

@Composable
private fun ProfileBioSection(user: User) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        if (user.bio.isNotBlank()) {
            Text(user.bio, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (user.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.5f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(user.location, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
            }
            if (user.joinedAt.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarToday, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.5f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Joined ${user.joinedAt.take(7)}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

// ─── Highlights Row ───────────────────────────────────────────────────────────

@Composable
private fun HighlightsRow(highlights: List<Highlight>) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(highlights, key = { it.id }) { h ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(2.dp, VibeGradient, CircleShape),
                ) {
                    AsyncImage(
                        model = h.coverUrl,
                        contentDescription = h.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().padding(2.dp).clip(CircleShape),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(h.title, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 64.dp))
            }
        }
    }
}

// ─── Tab Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun ProfileTabBar(
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
) {
    TabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor   = MaterialTheme.colorScheme.surface,
        indicator = { tabPositions ->
            Box(
                Modifier
                    .tabIndicatorOffset(tabPositions[selectedTab.ordinal])
                    .height(3.dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(VibeGradient)
            )
        },
    ) {
        ProfileTab.entries.forEach { tab ->
            val icon = when (tab) {
                ProfileTab.POSTS -> Icons.Outlined.GridOn
                ProfileTab.REELS -> Icons.Outlined.PlayCircleOutline
                ProfileTab.SAVED -> Icons.Outlined.BookmarkBorder
            }
            Tab(
                selected = selectedTab == tab,
                onClick  = { onTabSelected(tab) },
                icon     = {
                    Icon(icon, tab.name, tint = if (selectedTab == tab) VibePink
                    else MaterialTheme.colorScheme.onSurface.copy(0.5f))
                },
                selectedContentColor   = VibePink,
                unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(0.5f),
            )
        }
    }
}

// ─── Grid Row ────────────────────────────────────────────────────────────────

@Composable
private fun GridRow(posts: List<Post>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        posts.forEach { post ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clickable {},
            ) {
                AsyncImage(
                    model = post.mediaUrls.firstOrNull(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (post.mediaType == MediaType.REEL || post.mediaType == MediaType.VIDEO) {
                    Icon(Icons.Filled.PlayCircle, null, tint = Color.White.copy(0.8f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp))
                }
                if (post.isPinned) {
                    Icon(Icons.Filled.PushPin, null, tint = Color.White.copy(0.9f),
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(18.dp))
                }
            }
        }
        // Fill empty slots in last row
        repeat(3 - posts.size) {
            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
        }
    }
}
