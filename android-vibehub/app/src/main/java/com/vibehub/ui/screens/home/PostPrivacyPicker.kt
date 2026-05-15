package com.vibehub.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibehub.domain.model.CommentPermission
import com.vibehub.domain.model.PostVisibility
import com.vibehub.ui.theme.VibePink

/**
 * Bottom sheet that lets users choose who can see and comment on their post.
 * Used in the post creation flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostPrivacySheet(
    currentVisibility: PostVisibility,
    currentCommentPerm: CommentPermission,
    onVisibilityChange: (PostVisibility) -> Unit,
    onCommentPermChange: (CommentPermission) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(modifier = Modifier.navigationBarsPadding().padding(bottom = 24.dp)) {
            Text(
                "Post Audience",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Text(
                "Who can see this post?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
            )
            Spacer(Modifier.height(8.dp))

            PostVisibility.values().forEach { option ->
                PrivacyOptionRow(
                    icon     = visibilityIcon(option),
                    label    = option.label,
                    subtitle = visibilitySubtitle(option),
                    selected = currentVisibility == option,
                    onClick  = { onVisibilityChange(option) },
                )
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), thickness = 0.5.dp)

            Text(
                "Who can comment?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(4.dp))

            CommentPermission.values().forEach { option ->
                PrivacyOptionRow(
                    icon     = commentIcon(option),
                    label    = option.label,
                    subtitle = commentSubtitle(option),
                    selected = currentCommentPerm == option,
                    onClick  = { onCommentPermChange(option) },
                )
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick     = onDismiss,
                modifier    = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape       = RoundedCornerShape(28.dp),
                colors      = ButtonDefaults.buttonColors(containerColor = VibePink),
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PrivacyOptionRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) VibePink else MaterialTheme.colorScheme.onSurface)
        },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.45f))
        },
        leadingContent = {
            Icon(icon, null, tint = if (selected) VibePink else MaterialTheme.colorScheme.onSurface.copy(0.5f))
        },
        trailingContent = {
            if (selected) Icon(Icons.Filled.CheckCircle, null, tint = VibePink)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private fun visibilityIcon(v: PostVisibility): ImageVector = when (v) {
    PostVisibility.PUBLIC        -> Icons.Outlined.Public
    PostVisibility.FOLLOWERS     -> Icons.Outlined.Group
    PostVisibility.CLOSE_FRIENDS -> Icons.Outlined.Favorite
    PostVisibility.PRIVATE       -> Icons.Outlined.Lock
}

private fun visibilitySubtitle(v: PostVisibility): String = when (v) {
    PostVisibility.PUBLIC        -> "Anyone on VibeHub can see this"
    PostVisibility.FOLLOWERS     -> "Only your followers can see this"
    PostVisibility.CLOSE_FRIENDS -> "Only your close friends list"
    PostVisibility.PRIVATE       -> "Only you can see this"
}

private fun commentIcon(c: CommentPermission): ImageVector = when (c) {
    CommentPermission.EVERYONE      -> Icons.Outlined.Forum
    CommentPermission.FOLLOWERS     -> Icons.Outlined.Group
    CommentPermission.CLOSE_FRIENDS -> Icons.Outlined.Favorite
    CommentPermission.NO_ONE        -> Icons.Outlined.CommentsDisabled
}

private fun commentSubtitle(c: CommentPermission): String = when (c) {
    CommentPermission.EVERYONE      -> "Anyone can comment"
    CommentPermission.FOLLOWERS     -> "Only followers can comment"
    CommentPermission.CLOSE_FRIENDS -> "Only close friends can comment"
    CommentPermission.NO_ONE        -> "Comments disabled for this post"
}
