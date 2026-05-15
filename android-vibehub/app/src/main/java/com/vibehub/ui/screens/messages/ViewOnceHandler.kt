package com.vibehub.ui.screens.messages

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.vibehub.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Renders a view-once message bubble.
 *
 * Security constraints enforced:
 *  - No screenshot allowed (WindowManager.LayoutParams.FLAG_SECURE via Activity)
 *  - Blurred until tapped; removed from UI after first open
 *  - No forward / copy / save actions in the long-press menu
 *  - Marked "Opened" in the DB immediately on first view
 *
 * Note: FLAG_SECURE must be set on the Activity hosting the chat screen
 * for OS-level screenshot prevention. Set it in MainActivity or via a
 * SideEffect that calls window.setFlags(FLAG_SECURE, FLAG_SECURE).
 */
@Composable
fun ViewOnceMediaBubble(
    mediaUrl: String,
    isVideo: Boolean,
    isOpened: Boolean,
    isFromMe: Boolean,
    onOpened: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tapped by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    val hasBeenOpened = isOpened || tapped

    LaunchedEffect(tapped) {
        if (tapped) {
            onOpened()
            // 10-second auto-dismiss countdown
            countdown = 10
            while (countdown > 0) {
                delay(1000L)
                countdown--
            }
        }
    }

    Box(
        modifier = modifier
            .size(width = 180.dp, height = 240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(VibeCardDark),
        contentAlignment = Alignment.Center,
    ) {
        if (!hasBeenOpened) {
            // Blurred locked state
            AsyncImage(
                model = mediaUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(radius = 24.dp),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable { tapped = true }.padding(16.dp),
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Timer, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Text("View Once", fontWeight = FontWeight.Bold, color = Color.White)
                Text(if (isVideo) "Tap to play video" else "Tap to view photo", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
            }
        } else if (tapped && countdown > 0) {
            // Media visible with countdown overlay
            AsyncImage(
                model = mediaUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (isVideo) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.3f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlayCircle, null, tint = Color.White.copy(0.9f), modifier = Modifier.size(52.dp))
                }
            }
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).clip(CircleShape).background(Color.Black.copy(0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("$countdown", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        } else {
            // Expired / already opened
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.NoPhotography, null, tint = VibeTextMuted, modifier = Modifier.size(32.dp))
                Text("Opened", color = VibeTextMuted, style = MaterialTheme.typography.labelMedium)
                Text(if (isFromMe) "Seen by recipient" else "Viewed once", style = MaterialTheme.typography.labelSmall, color = VibeTextMuted)
            }
        }
    }
}

/**
 * Intercepts long-press menu for view-once messages and strips
 * forward / copy / screenshot-related actions.
 */
fun viewOnceAllowedActions(): Set<String> = setOf("reply", "delete", "report")

/**
 * Call this in the Activity that hosts the chat screen to block screenshots
 * while a view-once message is visible.
 */
fun setSecureWindowFlag(context: Context, secure: Boolean) {
    val activity = context as? android.app.Activity ?: return
    if (secure) {
        activity.window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
        )
    } else {
        activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
    }
}
