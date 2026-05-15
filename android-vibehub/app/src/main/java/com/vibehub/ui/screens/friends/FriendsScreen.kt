package com.vibehub.ui.screens.friends

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
import com.vibehub.domain.model.User
import com.vibehub.ui.components.GradientButton
import com.vibehub.ui.components.VibeAvatar
import com.vibehub.ui.components.VerifiedBadge
import com.vibehub.ui.theme.*
import com.vibehub.ui.viewmodel.FriendsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    userId: String,
    initialTab: Int = 0,
    onBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var searchQuery  by remember { mutableStateOf("") }

    LaunchedEffect(userId) { viewModel.loadAll(userId) }

    val tabs = listOf("Suggested", "Followers", "Following")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(tabs[selectedTab], fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                    actions = {
                        IconButton(onClick = {}) { Icon(Icons.Outlined.PersonAdd, null, tint = VibePink) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor   = MaterialTheme.colorScheme.surface,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color    = VibePink,
                        )
                    },
                ) {
                    tabs.forEachIndexed { i, tab ->
                        Tab(
                            selected = selectedTab == i,
                            onClick  = { selectedTab = i },
                            text     = { Text(tab, fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal) },
                            selectedContentColor   = VibePink,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                        )
                    }
                }
                // Search bar
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("Search") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp)) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape    = RoundedCornerShape(24.dp),
                    singleLine = true,
                )
            }
        },
    ) { padding ->
        val baseList: List<User> = when (selectedTab) {
            0 -> uiState.suggested
            1 -> uiState.followers
            2 -> uiState.following
            else -> emptyList()
        }
        val displayList = if (searchQuery.isBlank()) baseList
        else baseList.filter { it.displayName.contains(searchQuery, ignoreCase = true) || it.username.contains(searchQuery, ignoreCase = true) }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VibePink)
            }
        } else if (displayList.isEmpty()) {
            EmptyFriendsState(tab = selectedTab)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (selectedTab == 0) {
                    item {
                        Text("People you might know", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                            modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
                items(displayList, key = { it.id }) { user ->
                    FriendRow(
                        user       = user,
                        tab        = selectedTab,
                        onProfile  = { onNavigateToProfile(user.id) },
                        onFollow   = { viewModel.toggleFollow(user.id) },
                        onRemove   = { viewModel.removeFollower(user.id) },
                        onDismiss  = { viewModel.dismissSuggestion(user.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendRow(
    user: User,
    tab: Int,
    onProfile: () -> Unit,
    onFollow: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var isFollowing by remember { mutableStateOf(user.isFollowedByMe) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar
            Box(modifier = Modifier.clickable(onClick = onProfile)) {
                VibeAvatar(imageUrl = user.avatarUrl, size = 50.dp)
                if (user.isVerified) {
                    Box(modifier = Modifier.align(Alignment.BottomEnd)) { VerifiedBadge(16.dp) }
                }
            }
            Spacer(Modifier.width(12.dp))

            // Name + meta
            Column(modifier = Modifier.weight(1f).clickable(onClick = onProfile)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(user.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("@${user.username}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                if (user.followedBy.isNotEmpty()) {
                    Text("Followed by ${user.followedBy.first()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("${formatCount(user.followersCount)} followers",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.35f))
            }

            Spacer(Modifier.width(8.dp))

            // Action buttons
            when (tab) {
                0 -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else VibeGradient)
                                .clickable { isFollowing = !isFollowing; onFollow() }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                        ) {
                            Text(
                                if (isFollowing) "Following" else "Follow",
                                color = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White,
                                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                            )
                        }
                        TextButton(onClick = onDismiss, contentPadding = PaddingValues(0.dp)) {
                            Text("Remove", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                        }
                    }
                }
                1 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else VibeGradient)
                                .clickable { isFollowing = !isFollowing; onFollow() }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(if (isFollowing) "Following" else "Follow Back",
                                color = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White,
                                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Outlined.PersonRemove, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                        }
                    }
                }
                2 -> {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { isFollowing = false; onFollow() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text("Following", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFriendsState(tab: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = when (tab) {
                    0 -> Icons.Outlined.PersonSearch
                    1 -> Icons.Outlined.Group
                    else -> Icons.Outlined.PersonAdd
                },
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(0.2f),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = when (tab) {
                    0 -> "No suggestions right now"
                    1 -> "No followers yet"
                    else -> "Not following anyone yet"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
            )
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000     -> "${count / 1_000}K"
    else               -> count.toString()
}
