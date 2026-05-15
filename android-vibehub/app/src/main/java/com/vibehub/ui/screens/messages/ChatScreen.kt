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
import com.vibehub.domain.model.Message
import com.vibehub.ui.components.GradientButton
import com.vibehub.ui.components.VibeAvatar
import com.vibehub.ui.theme.*
import com.vibehub.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Scroll to bottom on new messages
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VibeAvatar(imageUrl = "", size = 38.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Conversation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Online", style = MaterialTheme.typography.bodySmall, color = VibeSuccess)
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, null)
                }
            },
            actions = {
                IconButton(onClick = {}) { Icon(Icons.Outlined.Phone, null) }
                IconButton(onClick = {}) { Icon(Icons.Outlined.Videocam, null) }
                IconButton(onClick = {}) { Icon(Icons.Filled.MoreVert, null) }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        // Reply-to preview
        AnimatedVisibility(visible = uiState.replyTo != null) {
            uiState.replyTo?.let { reply ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.width(3.dp).height(36.dp).background(VibePink, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Replying to ${reply.sender.displayName}", style = MaterialTheme.typography.labelSmall, color = VibePink)
                        Text(reply.text, style = MaterialTheme.typography.bodySmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { viewModel.setReplyTo(null) }) {
                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    isFromMe = true, // Replace with actual check: message.senderId == currentUserId
                    onReply  = { viewModel.setReplyTo(message) },
                    onDelete = { viewModel.deleteMessage(message.id) },
                )
            }
        }

        // Input bar
        ChatInputBar(
            text      = uiState.draftText,
            onTextChange = { viewModel.setDraftText(it) },
            onSend    = { viewModel.sendMessage() },
            isSending = uiState.isSending,
        )
    }
}

@Composable
private fun ChatBubble(
    message: Message,
    isFromMe: Boolean,
    onReply: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start,
    ) {
        if (!isFromMe) {
            VibeAvatar(imageUrl = message.sender.avatarUrl, size = 32.dp)
            Spacer(Modifier.width(6.dp))
        }
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start,
        ) {
            if (message.replyToMessageId != null) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("Replied to a message", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isFromMe) 20.dp else 4.dp,
                            topEnd   = if (isFromMe) 4.dp else 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd   = 20.dp,
                        )
                    )
                    .background(
                        if (isFromMe) VibeGradient
                        else androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onReply,
                    ),
            ) {
                if (message.isDeleted) {
                    Text("Message deleted", style = MaterialTheme.typography.bodySmall,
                        color = if (isFromMe) Color.White.copy(0.6f) else MaterialTheme.colorScheme.onSurface.copy(0.4f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                } else {
                    Column {
                        Text(
                            text  = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isFromMe) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text  = message.createdAt.takeLast(5),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isFromMe) Color.White.copy(0.7f) else MaterialTheme.colorScheme.onSurface.copy(0.4f),
                            )
                            if (isFromMe) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (message.isRead) Icons.Filled.DoneAll else Icons.Filled.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (message.isRead) Color(0xFF4FC3F7) else Color.White.copy(0.7f),
                                )
                            }
                            if (message.isEdited) {
                                Spacer(Modifier.width(4.dp))
                                Text("edited", style = MaterialTheme.typography.labelSmall,
                                    color = if (isFromMe) Color.White.copy(0.5f) else MaterialTheme.colorScheme.onSurface.copy(0.35f))
                            }
                        }
                    }
                }
            }
        }
        if (isFromMe) {
            Spacer(Modifier.width(6.dp))
            VibeAvatar(imageUrl = "", size = 32.dp)
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = {}) { Icon(Icons.Outlined.Add, null, tint = VibePink) }

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Message...") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            maxLines = 5,
            trailingIcon = {
                Row {
                    IconButton(onClick = {}) { Icon(Icons.Outlined.EmojiEmotions, null, tint = VibeGold) }
                }
            },
        )

        if (text.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(VibeGradient)
                    .clickable(enabled = !isSending, onClick = onSend),
                contentAlignment = Alignment.Center,
            ) {
                if (isSending) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Send, "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        } else {
            IconButton(onClick = {}) { Icon(Icons.Outlined.Mic, null, tint = VibePink) }
        }
    }
}
