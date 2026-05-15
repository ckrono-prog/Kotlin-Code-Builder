package com.vibehub.ui.screens.messages

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibehub.domain.model.Message
import com.vibehub.domain.model.User
import com.vibehub.ui.components.VibeAvatar
import com.vibehub.ui.components.VerifiedBadge
import com.vibehub.ui.theme.*
import com.vibehub.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

private val QUICK_REACTIONS = listOf("❤️", "😂", "😮", "😢", "😡", "👍")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EnhancedChatScreen(
    remoteUser: User,
    currentUserId: String,
    onBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var showMessageOptions by remember { mutableStateOf<Message?>(null) }
    var showReactionPicker by remember { mutableStateOf<Message?>(null) }
    var isTyping by remember { mutableStateOf(false) }

    // Simulate typing indicator from remote (wire to Supabase Realtime)
    LaunchedEffect(uiState.draftText) {
        isTyping = uiState.draftText.isNotBlank()
    }

    // Scroll to bottom on new messages
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(uiState.messages.size - 1) }
        }
    }

    // Message options modal
    if (showMessageOptions != null) {
        MessageOptionsModal(
            message   = showMessageOptions!!,
            isFromMe  = showMessageOptions!!.senderId == currentUserId,
            onDismiss = { showMessageOptions = null },
            onUnsend  = { viewModel.deleteMessage(it.id); showMessageOptions = null },
            onDelete  = { viewModel.deleteMessage(it.id); showMessageOptions = null },
            onReply   = { viewModel.setReplyTo(it); showMessageOptions = null },
            onForward = { showMessageOptions = null },
            onReport  = { showMessageOptions = null },
            onCopy    = { showMessageOptions = null },
            onMark    = { showMessageOptions = null },
        )
    }

    // Quick reaction picker
    if (showReactionPicker != null) {
        ReactionPickerOverlay(
            onReact = { emoji ->
                // TODO: send reaction via repository
                showReactionPicker = null
            },
            onDismiss = { showReactionPicker = null },
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ── Top bar ───────────────────────────────────────────────────
        TopAppBar(
            title = {
                Row(
                    modifier = Modifier.clickable { onNavigateToProfile(remoteUser.id) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        VibeAvatar(imageUrl = remoteUser.avatarUrl, size = 38.dp)
                        // Online dot
                        Box(
                            modifier = Modifier.size(12.dp).align(Alignment.BottomEnd)
                                .clip(CircleShape).background(VibeSuccess)
                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(remoteUser.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (remoteUser.isVerified) VerifiedBadge(14.dp)
                        }
                        AnimatedContent(targetState = isTyping, label = "typing") { typing ->
                            if (typing) {
                                Text("typing…", style = MaterialTheme.typography.bodySmall, color = VibePink)
                            } else {
                                Text("Online", style = MaterialTheme.typography.bodySmall, color = VibeSuccess)
                            }
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
            },
            actions = {
                IconButton(onClick = onAudioCall) { Icon(Icons.Outlined.Phone, null) }
                IconButton(onClick = onVideoCall) { Icon(Icons.Outlined.Videocam, null) }
                IconButton(onClick = {}) { Icon(Icons.Filled.MoreVert, null) }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        )

        // ── Reply preview ─────────────────────────────────────────────
        AnimatedVisibility(visible = uiState.replyTo != null) {
            uiState.replyTo?.let { reply ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.width(3.dp).height(36.dp).background(VibePink, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(reply.sender.displayName, style = MaterialTheme.typography.labelSmall, color = VibePink)
                        Text(reply.text, style = MaterialTheme.typography.bodySmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { viewModel.setReplyTo(null) }) {
                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ── Typing indicator ──────────────────────────────────────────
        // Wire to Supabase Realtime presence channel in ViewModel
        AnimatedVisibility(visible = false /* replace with remoteIsTyping from VM */) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                VibeAvatar(imageUrl = remoteUser.avatarUrl, size = 24.dp)
                Spacer(Modifier.width(6.dp))
                TypingDots()
            }
        }

        // ── Messages ──────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                val isFromMe = message.senderId == currentUserId

                SwipeableMessageRow(
                    isFromMe = isFromMe,
                    onSwipe  = { viewModel.setReplyTo(message) },
                ) {
                    EnhancedBubble(
                        message  = message,
                        isFromMe = isFromMe,
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showMessageOptions = message
                        },
                        onDoubleTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showReactionPicker = message
                        },
                        onAvatarClick = { onNavigateToProfile(message.senderId) },
                    )
                }
            }
        }

        // ── Input bar ─────────────────────────────────────────────────
        EnhancedChatInput(
            text        = uiState.draftText,
            onTextChange = { viewModel.setDraftText(it) },
            onSend       = { viewModel.sendMessage() },
            isSending    = uiState.isSending,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Swipeable message row (swipe right = reply)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeableMessageRow(
    isFromMe: Boolean,
    onSwipe: () -> Unit,
    content: @Composable () -> Unit,
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    var hasTriggered by remember { mutableStateOf(false) }

    Box(modifier = Modifier
        .fillMaxWidth()
        .draggable(
            orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
            state = rememberDraggableState { delta ->
                val direction = if (isFromMe) -1 else 1
                if (delta * direction > 0) {
                    offsetX = (offsetX + delta).coerceIn(-80f, 80f)
                    if (!hasTriggered && kotlin.math.abs(offsetX) > 60f) {
                        hasTriggered = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSwipe()
                    }
                }
            },
            onDragStopped = {
                offsetX = 0f
                hasTriggered = false
            },
        )
    ) {
        // Reply hint icon
        if (kotlin.math.abs(offsetX) > 20f) {
            Box(
                modifier = Modifier
                    .align(if (isFromMe) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 16.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Reply, null, tint = VibePink, modifier = Modifier.size(16.dp))
            }
        }
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Enhanced message bubble with double-tap + long-press
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnhancedBubble(
    message: Message,
    isFromMe: Boolean,
    onLongPress: () -> Unit,
    onDoubleTap: () -> Unit,
    onAvatarClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start,
    ) {
        if (!isFromMe) {
            VibeAvatar(imageUrl = message.sender.avatarUrl, size = 28.dp,
                modifier = Modifier.clickable(onClick = onAvatarClick))
            Spacer(Modifier.width(6.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start,
        ) {
            // Reply-to preview
            if (message.replyToMessageId != null) {
                Box(
                    modifier = Modifier.padding(bottom = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("↩ Replied to a message", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(
                        topStart    = if (isFromMe) 20.dp else 4.dp,
                        topEnd      = if (isFromMe) 4.dp else 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd   = 20.dp,
                    ))
                    .background(if (isFromMe) VibeGradient else Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)))
                    .combinedClickable(
                        onLongClick = onLongPress,
                        onDoubleClick = onDoubleTap,
                        onClick = {},
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                if (message.isDeleted) {
                    Text("This message was deleted", style = MaterialTheme.typography.bodySmall,
                        color = if (isFromMe) Color.White.copy(0.5f) else MaterialTheme.colorScheme.onSurface.copy(0.35f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                } else {
                    Column {
                        Text(message.text, style = MaterialTheme.typography.bodyMedium,
                            color = if (isFromMe) Color.White else MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text(message.createdAt.takeLast(5), style = MaterialTheme.typography.labelSmall,
                                color = if (isFromMe) Color.White.copy(0.65f) else MaterialTheme.colorScheme.onSurface.copy(0.4f))
                            if (isFromMe) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (message.isRead) Icons.Filled.DoneAll else Icons.Filled.Done,
                                    null, modifier = Modifier.size(14.dp),
                                    tint = if (message.isRead) Color(0xFF64B5F6) else Color.White.copy(0.6f),
                                )
                            }
                            if (message.isEdited) {
                                Spacer(Modifier.width(4.dp))
                                Text("edited", style = MaterialTheme.typography.labelSmall,
                                    color = if (isFromMe) Color.White.copy(0.4f) else MaterialTheme.colorScheme.onSurface.copy(0.3f))
                            }
                        }
                    }
                }
            }

            // Reactions row
            if (message.reactions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .offset(y = (-6).dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    message.reactions.entries.take(5).forEach { (emoji, count) ->
                        Text("$emoji${if (count > 1) " $count" else ""}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        if (isFromMe) {
            Spacer(Modifier.width(6.dp))
            VibeAvatar(imageUrl = "", size = 28.dp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Typing dots animation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "typing_dots")
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp)) {
        (0..2).forEach { i ->
            val alpha by transition.animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(400, delayMillis = i * 130, easing = EaseInOutSine),
                    RepeatMode.Reverse,
                ),
                label = "dot_$i",
            )
            Box(modifier = Modifier.size(6.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha)))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Message options bottom sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageOptionsModal(
    message: Message,
    isFromMe: Boolean,
    onDismiss: () -> Unit,
    onUnsend: (Message) -> Unit,
    onDelete: (Message) -> Unit,
    onReply: (Message) -> Unit,
    onForward: (Message) -> Unit,
    onReport: (Message) -> Unit,
    onCopy: (Message) -> Unit,
    onMark: (Message) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            // Quick reactions
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                QUICK_REACTIONS.forEach { emoji ->
                    Text(
                        emoji,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onDismiss() }
                            .padding(8.dp),
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            val options = buildList {
                add(Triple(Icons.Outlined.Reply, "Reply", { onReply(message) }))
                add(Triple(Icons.Outlined.ContentCopy, "Copy", { onCopy(message) }))
                add(Triple(Icons.Outlined.Forward, "Forward", { onForward(message) }))
                add(Triple(Icons.Outlined.BookmarkBorder, "Mark", { onMark(message) }))
                if (isFromMe) add(Triple(Icons.Outlined.Edit, "Unsend", { onUnsend(message) }))
                add(Triple(Icons.Outlined.Delete, "Delete", { onDelete(message) }))
                if (!isFromMe) add(Triple(Icons.Outlined.Flag, "Report", { onReport(message) }))
            }

            options.forEach { (icon, label, action) ->
                ListItem(
                    headlineContent = {
                        Text(label, color = if (label in listOf("Delete", "Unsend")) VibeError
                        else MaterialTheme.colorScheme.onSurface)
                    },
                    leadingContent = {
                        Icon(icon, null,
                            tint = if (label in listOf("Delete", "Unsend")) VibeError
                            else MaterialTheme.colorScheme.onSurface)
                    },
                    modifier = Modifier.clickable { action(); onDismiss() },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reaction picker overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReactionPickerOverlay(onReact: (String) -> Unit, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().clickable(onClick = onDismiss).background(Color.Black.copy(0.3f)),
        contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(40.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QUICK_REACTIONS.forEach { emoji ->
                Text(
                    emoji,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onReact(emoji) }
                        .padding(6.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Enhanced input bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EnhancedChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(onClick = {}) { Icon(Icons.Outlined.Add, null, tint = VibePink) }
        OutlinedTextField(
            value = text, onValueChange = onTextChange,
            placeholder = { Text("Message…") },
            modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp), maxLines = 5,
            trailingIcon = {
                Row {
                    IconButton(onClick = {}) { Icon(Icons.Outlined.EmojiEmotions, null, tint = VibeGold) }
                    IconButton(onClick = {}) { Icon(Icons.Outlined.Gif, null, tint = VibePink) }
                }
            },
        )
        if (text.isNotBlank()) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(VibeGradient)
                    .clickable(enabled = !isSending, onClick = onSend),
                contentAlignment = Alignment.Center,
            ) {
                if (isSending) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        } else {
            IconButton(onClick = {}) { Icon(Icons.Outlined.Mic, null, tint = VibePink) }
            IconButton(onClick = {}) { Icon(Icons.Outlined.CameraAlt, null, tint = VibePink) }
        }
    }
}
