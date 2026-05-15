package com.vibehub.ui.screens.comments

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibehub.domain.model.Comment
import com.vibehub.ui.components.VibeAvatar
import com.vibehub.ui.components.VerifiedBadge
import com.vibehub.ui.theme.*
import com.vibehub.ui.viewmodel.CommentsViewModel
import com.vibehub.util.InputSanitizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    postId: String,
    onBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: CommentsViewModel = hiltViewModel(),
) {
    val uiState      by viewModel.uiState.collectAsState()
    var commentText  by remember { mutableStateOf("") }
    var replyingTo   by remember { mutableStateOf<Comment?>(null) }
    var expandedReplies by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loadedReplies   by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }  // parentId → loaded count
    var showStickers by remember { mutableStateOf(false) }

    LaunchedEffect(postId) { viewModel.loadComments(postId) }

    if (showStickers) {
        StickerPickerSheet(
            onStickerSelected = { sticker ->
                commentText += sticker
                showStickers = false
            },
            onDismiss = { showStickers = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Comments", fontWeight = FontWeight.Bold)
                        if (uiState.totalCount > 0) {
                            Text("${uiState.totalCount} comments", style = MaterialTheme.typography.labelSmall, color = VibeTextSecondary)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = {}) { Icon(Icons.Outlined.Sort, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            CommentInputBar(
                replyingTo    = replyingTo,
                text          = commentText,
                onTextChange  = { commentText = InputSanitizer.sanitizeComment(it) },
                onSend        = {
                    viewModel.postComment(postId, commentText, replyingTo?.id)
                    commentText = ""
                    replyingTo  = null
                },
                onCancelReply  = { replyingTo = null },
                onStickerClick = { showStickers = true },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VibePink)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val topLevel = uiState.comments.filter { it.parentId == null }
                items(topLevel, key = { it.id }) { comment ->
                    Column {
                        CommentItem(
                            comment           = comment,
                            isTopLevel        = true,
                            isFollowingAuthor = comment.author.isFollowedByMe,
                            onLike            = { viewModel.likeComment(it) },
                            onReply           = { replyingTo = it },
                            onProfile         = { onNavigateToProfile(comment.authorId) },
                            onDelete          = { viewModel.deleteComment(it) },
                            onReport          = {},
                            onFollow          = { viewModel.followUser(comment.authorId) },
                        )

                        val allReplies = uiState.comments.filter { it.parentId == comment.id }
                        if (allReplies.isNotEmpty()) {
                            val isExpanded    = comment.id in expandedReplies
                            val loadedCount   = loadedReplies[comment.id] ?: 3
                            val visibleReplies = allReplies.take(loadedCount)

                            if (!isExpanded) {
                                TextButton(
                                    onClick  = { expandedReplies = expandedReplies + comment.id },
                                    modifier = Modifier.padding(start = 52.dp),
                                ) {
                                    Icon(Icons.Outlined.SubdirectoryArrowRight, null, modifier = Modifier.size(14.dp), tint = VibePink)
                                    Spacer(Modifier.width(4.dp))
                                    Text("View ${allReplies.size} ${if (allReplies.size == 1) "reply" else "replies"}", color = VibePink, style = MaterialTheme.typography.labelMedium)
                                }
                            } else {
                                TextButton(
                                    onClick  = { expandedReplies = expandedReplies - comment.id },
                                    modifier = Modifier.padding(start = 52.dp),
                                ) {
                                    Text("Hide replies", color = MaterialTheme.colorScheme.onSurface.copy(0.4f), style = MaterialTheme.typography.labelMedium)
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(modifier = Modifier.padding(start = 52.dp)) {
                                        visibleReplies.forEach { reply ->
                                            CommentItem(
                                                comment           = reply,
                                                isTopLevel        = false,
                                                isFollowingAuthor = reply.author.isFollowedByMe,
                                                onLike            = { viewModel.likeComment(it) },
                                                onReply           = { replyingTo = comment },
                                                onProfile         = { onNavigateToProfile(reply.authorId) },
                                                onDelete          = { viewModel.deleteComment(it) },
                                                onReport          = {},
                                                onFollow          = { viewModel.followUser(reply.authorId) },
                                            )
                                        }

                                        // Lazy-load more replies button
                                        if (loadedCount < allReplies.size) {
                                            TextButton(
                                                onClick = {
                                                    loadedReplies = loadedReplies + (comment.id to loadedCount + 10)
                                                },
                                            ) {
                                                Text("Load ${minOf(10, allReplies.size - loadedCount)} more replies", color = VibePink, style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Load more top-level comments
                if (uiState.hasMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                            TextButton(onClick = { viewModel.loadMoreComments(postId) }) {
                                Text("Load more comments", color = VibePink)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommentItem(
    comment: Comment,
    isTopLevel: Boolean,
    isFollowingAuthor: Boolean,
    onLike: (Comment) -> Unit,
    onReply: (Comment) -> Unit,
    onProfile: () -> Unit,
    onDelete: (Comment) -> Unit,
    onReport: (Comment) -> Unit,
    onFollow: () -> Unit,
) {
    var showOptions by remember { mutableStateOf(false) }
    var liked       by remember { mutableStateOf(comment.isLikedByMe) }
    var likeCount   by remember { mutableIntStateOf(comment.likesCount) }

    if (showOptions) {
        CommentOptionsSheet(
            comment   = comment,
            onDismiss = { showOptions = false },
            onDelete  = { onDelete(comment); showOptions = false },
            onReport  = { onReport(comment); showOptions = false },
            onCopy    = { showOptions = false },
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onLongClick = { showOptions = true }, onClick = {},
        ).padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        VibeAvatar(imageUrl = comment.author.avatarUrl, size = if (isTopLevel) 34.dp else 28.dp, modifier = Modifier.clickable(onClick = onProfile))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Name + text
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(comment.author.displayName) }
                append("  ")
                append(comment.text)
            }, style = MaterialTheme.typography.bodyMedium)

            // Sticker rendering (if text is a single emoji/sticker)
            if (comment.text.length <= 4 && comment.text.any { it.category == CharCategory.OTHER_SYMBOL || it.category == CharCategory.OTHER_LETTER }) {
                // Already rendered above; in full app, distinguish sticker from text
            }

            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(comment.createdAt.take(10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                if (likeCount > 0) {
                    Text("$likeCount ${if (likeCount == 1) "like" else "likes"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.45f))
                }
                TextButton(onClick = { onReply(comment) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                    Text("Reply", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
                // Follow button in comment row (only if not following)
                if (isTopLevel && !isFollowingAuthor) {
                    TextButton(onClick = onFollow, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                        Text("Follow", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = VibePink)
                    }
                }
            }
        }

        // Like button
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick  = { liked = !liked; likeCount += if (liked) 1 else -1; onLike(comment) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null,
                    tint = if (liked) VibePink else MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.size(18.dp))
            }
            if (likeCount > 0) {
                Text(likeCount.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentOptionsSheet(comment: Comment, onDismiss: () -> Unit, onDelete: () -> Unit, onReport: () -> Unit, onCopy: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.navigationBarsPadding().padding(bottom = 24.dp)) {
            ListItem(headlineContent = { Text("Copy") }, leadingContent = { Icon(Icons.Outlined.ContentCopy, null) }, modifier = Modifier.clickable { onCopy() })
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.3.dp)
            ListItem(headlineContent = { Text("Report", color = VibeError) }, leadingContent = { Icon(Icons.Outlined.Flag, null, tint = VibeError) }, modifier = Modifier.clickable { onReport() })
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.3.dp)
            ListItem(headlineContent = { Text("Delete", color = VibeError) }, leadingContent = { Icon(Icons.Outlined.Delete, null, tint = VibeError) }, modifier = Modifier.clickable { onDelete() })
        }
    }
}

@Composable
private fun CommentInputBar(
    replyingTo: Comment?,
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancelReply: () -> Unit,
    onStickerClick: () -> Unit,
) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        AnimatedVisibility(visible = replyingTo != null) {
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Replying to ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                Text("@${replyingTo?.author?.username}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = VibePink)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onCancelReply, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VibeAvatar(imageUrl = "", size = 34.dp)
            OutlinedTextField(
                value         = text,
                onValueChange = onTextChange,
                placeholder   = { Text(if (replyingTo != null) "Add a reply…" else "Add a comment…") },
                modifier      = Modifier.weight(1f),
                shape         = RoundedCornerShape(24.dp),
                maxLines      = 4,
                trailingIcon  = {
                    Row {
                        IconButton(onClick = onStickerClick) { Icon(Icons.Outlined.EmojiEmotions, null, tint = VibeGold) }
                    }
                },
            )
            if (text.isNotBlank()) {
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(VibeGradient).clickable(onClick = onSend),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
