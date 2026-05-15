package com.vibehub.ui.screens.messages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.vibehub.ui.screens.home.VideoTrimmerSheet
import com.vibehub.ui.theme.*

data class MediaAttachment(
    val uri: Uri,
    val isVideo: Boolean,
    val caption: String = "",
    val isViewOnce: Boolean = false,
)

/**
 * Full-screen media preview sheet.
 * Lets users review their photo/video before sending, add a caption,
 * choose view-once, and trim videos to 2-min limit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPreviewSheet(
    attachments: List<MediaAttachment>,
    onSend: (List<MediaAttachment>) -> Unit,
    onDismiss: () -> Unit,
) {
    var items by remember { mutableStateOf(attachments) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var showTrimmer by remember { mutableStateOf(false) }
    val selected = items.getOrNull(selectedIndex)

    if (showTrimmer && selected != null) {
        VideoTrimmerSheet(
            uri      = selected.uri,
            onDismiss = { showTrimmer = false },
            onTrimmed = { _, _ -> showTrimmer = false },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = {
                    if (selected?.isVideo == true) {
                        IconButton(onClick = { showTrimmer = true }) {
                            Icon(Icons.Filled.ContentCut, null, tint = VibeGold)
                        }
                    }
                    Button(
                        onClick = { onSend(items) },
                        shape  = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VibePink),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Icon(Icons.Filled.Send, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Send", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
            )
        },
        containerColor = Color.Black,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Main preview
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                selected?.let { media ->
                    if (media.isVideo) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AsyncImage(model = media.uri, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                            Icon(Icons.Filled.PlayCircle, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(64.dp))
                        }
                    } else {
                        AsyncImage(model = media.uri, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                    }
                }
            }

            // Caption field + view-once toggle
            Column(
                modifier = Modifier.background(Color(0xFF111111)).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (selected != null) {
                    OutlinedTextField(
                        value = selected.caption,
                        onValueChange = { cap ->
                            items = items.mapIndexed { i, m -> if (i == selectedIndex) m.copy(caption = cap.take(500)) else m }
                        },
                        placeholder = { Text("Add a caption…", color = Color.White.copy(0.4f)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VibePink,
                            unfocusedBorderColor = Color.White.copy(0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )

                    // View-once toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                            items = items.mapIndexed { i, m -> if (i == selectedIndex) m.copy(isViewOnce = !m.isViewOnce) else m }
                        }.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Timer, null, tint = if (selected.isViewOnce) VibePink else Color.White.copy(0.6f), modifier = Modifier.size(18.dp))
                            Column {
                                Text("View Once", fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                Text("Recipient can only view it once", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.4f))
                            }
                        }
                        Switch(
                            checked = selected.isViewOnce,
                            onCheckedChange = {
                                items = items.mapIndexed { i, m -> if (i == selectedIndex) m.copy(isViewOnce = it) else m }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VibePink),
                        )
                    }
                }
            }

            // Thumbnail strip (multiple items)
            if (items.size > 1) {
                LazyRow(
                    modifier = Modifier.background(Color(0xFF0A0A0A)).padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(items) { idx, media ->
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(BorderStroke(if (idx == selectedIndex) 2.dp else 0.dp, VibePink), RoundedCornerShape(8.dp))
                                .clickable { selectedIndex = idx },
                        ) {
                            AsyncImage(model = media.uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            if (media.isVideo) {
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.3f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            // Remove button
                            IconButton(
                                onClick = {
                                    items = items.filterIndexed { i, _ -> i != idx }
                                    if (selectedIndex >= items.size) selectedIndex = maxOf(0, items.size - 1)
                                },
                                modifier = Modifier.size(16.dp).align(Alignment.TopEnd).background(Color.Black.copy(0.7f), CircleShape),
                            ) {
                                Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(9.dp))
                            }
                        }
                    }
                    // Add more media button
                    item {
                        Box(
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1A)).border(BorderStroke(1.dp, Color.White.copy(0.15f)), RoundedCornerShape(8.dp)).clickable {},
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Add, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
