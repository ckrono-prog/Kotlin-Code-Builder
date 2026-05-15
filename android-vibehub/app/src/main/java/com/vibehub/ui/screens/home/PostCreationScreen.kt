package com.vibehub.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.vibehub.domain.model.PostVisibility
import com.vibehub.domain.model.CommentPermission
import com.vibehub.ui.components.GradientButton
import com.vibehub.ui.theme.*
import com.vibehub.util.InputSanitizer

enum class PostType { MEDIA, TEXT, STORY }

data class SongChoice(val id: String, val title: String, val artist: String, val artUrl: String, val previewUrl: String)
data class TaggedUser(val id: String, val username: String, val displayName: String, val avatarUrl: String)
data class LocationTag(val placeId: String, val name: String, val address: String, val lat: Double, val lng: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCreationScreen(
    postType: PostType = PostType.MEDIA,
    onBack: () -> Unit,
    onPostSuccess: () -> Unit,
) {
    var caption        by remember { mutableStateOf("") }
    var selectedUris   by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedSong   by remember { mutableStateOf<SongChoice?>(null) }
    var taggedUsers    by remember { mutableStateOf<List<TaggedUser>>(emptyList()) }
    var locationTag    by remember { mutableStateOf<LocationTag?>(null) }
    var visibility     by remember { mutableStateOf(PostVisibility.PUBLIC) }
    var commentPerm    by remember { mutableStateOf(CommentPermission.EVERYONE) }
    var isPosting      by remember { mutableStateOf(false) }

    var showSongSheet      by remember { mutableStateOf(false) }
    var showLocationSheet  by remember { mutableStateOf(false) }
    var showTagSheet       by remember { mutableStateOf(false) }
    var showPrivacySheet   by remember { mutableStateOf(false) }
    var showTrimmer        by remember { mutableStateOf(false) }
    var showTextEditor     by remember { mutableStateOf(false) }

    // Text post state (only for PostType.TEXT)
    var textPostContent    by remember { mutableStateOf("") }
    var textBgColor        by remember { mutableStateOf<TextPostBackground>(TextPostBackground.Gradient(VibeGradient)) }
    var textFont           by remember { mutableStateOf(VibeFont.Default) }

    val mediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> selectedUris = uris }

    if (showSongSheet)   SongSearchSheet(selectedSong, onSelect = { selectedSong = it; showSongSheet = false }, onDismiss = { showSongSheet = false })
    if (showLocationSheet) LocationSearchSheet(onSelect = { locationTag = it; showLocationSheet = false }, onDismiss = { showLocationSheet = false })
    if (showTagSheet)    TagUserSheet(tagged = taggedUsers, onToggle = { u -> taggedUsers = if (taggedUsers.any { it.id == u.id }) taggedUsers - u else taggedUsers + u }, onDismiss = { showTagSheet = false })
    if (showPrivacySheet) PostPrivacySheet(visibility, commentPerm, onVisibilityChange = { visibility = it }, onCommentPermChange = { commentPerm = it }, onDismiss = { showPrivacySheet = false })
    if (showTextEditor)  TextPostEditor(content = textPostContent, background = textBgColor, font = textFont, onContentChange = { textPostContent = it }, onBackgroundChange = { textBgColor = it }, onFontChange = { textFont = it }, onDismiss = { showTextEditor = false })
    if (showTrimmer && selectedUris.isNotEmpty()) VideoTrimmerSheet(uri = selectedUris.first(), onDismiss = { showTrimmer = false }, onTrimmed = { showTrimmer = false })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (postType == PostType.STORY) "New Story" else "New Post", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.Close, null) } },
                actions = {
                    Button(
                        onClick  = { isPosting = true; onPostSuccess() },
                        enabled  = !isPosting && (selectedUris.isNotEmpty() || (postType == PostType.TEXT && textPostContent.isNotBlank())),
                        shape    = RoundedCornerShape(20.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = VibePink),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        if (isPosting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("Share", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            // ── Media picker row ───────────────────────────────────────────────
            if (postType != PostType.TEXT) {
                item {
                    MediaPickerRow(
                        selectedUris = selectedUris,
                        onPickMedia  = { mediaPicker.launch("*/*") },
                        onRemove     = { uri -> selectedUris = selectedUris - uri },
                        onTrimVideo  = { showTrimmer = true },
                    )
                }
            }

            // ── Text post editor trigger ───────────────────────────────────────
            if (postType == PostType.TEXT) {
                item {
                    TextPostPreview(
                        content    = textPostContent,
                        background = textBgColor,
                        font       = textFont,
                        onClick    = { showTextEditor = true },
                    )
                }
            }

            // ── Caption ────────────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value         = caption,
                    onValueChange = { caption = InputSanitizer.sanitizeCaption(it) },
                    placeholder   = { Text("Write a caption…", color = MaterialTheme.colorScheme.onSurface.copy(0.4f)) },
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    maxLines      = 6,
                    supportingText = { Text("${caption.length}/2200", style = MaterialTheme.typography.labelSmall) },
                    shape         = RoundedCornerShape(14.dp),
                )
            }

            // ── Song ───────────────────────────────────────────────────────────
            item {
                PostOptionRow(
                    icon    = Icons.Outlined.MusicNote,
                    label   = if (selectedSong != null) "${selectedSong!!.title} · ${selectedSong!!.artist}" else "Add Music",
                    onClick = { showSongSheet = true },
                    hasValue = selectedSong != null,
                    onClear  = { selectedSong = null },
                )
            }

            // ── Tag people ─────────────────────────────────────────────────────
            item {
                PostOptionRow(
                    icon    = Icons.Outlined.PersonAdd,
                    label   = if (taggedUsers.isEmpty()) "Tag People" else taggedUsers.joinToString(", ") { "@${it.username}" },
                    onClick = { showTagSheet = true },
                    hasValue = taggedUsers.isNotEmpty(),
                    onClear  = { taggedUsers = emptyList() },
                )
            }

            // ── Location ───────────────────────────────────────────────────────
            item {
                PostOptionRow(
                    icon    = Icons.Outlined.LocationOn,
                    label   = locationTag?.name ?: "Add Location",
                    onClick = { showLocationSheet = true },
                    hasValue = locationTag != null,
                    onClear  = { locationTag = null },
                )
            }

            // ── Privacy ────────────────────────────────────────────────────────
            item {
                PostOptionRow(
                    icon    = Icons.Outlined.Lock,
                    label   = "${visibility.label} · ${commentPerm.label}",
                    onClick = { showPrivacySheet = true },
                )
            }

            // ── Trimmer hint for long videos ───────────────────────────────────
            if (selectedUris.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = CardDefaults.cardColors(containerColor = VibeGold.copy(0.12f)),
                        border   = BorderStroke(1.dp, VibeGold.copy(0.3f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Outlined.ContentCut, null, tint = VibeGold, modifier = Modifier.size(18.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Video length limit: 2 minutes", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = VibeGold)
                                Text("Long videos will be auto-trimmed or you can trim manually.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            }
                            TextButton(onClick = { showTrimmer = true }) { Text("Trim", color = VibeGold, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────────

@Composable
fun MediaPickerRow(
    selectedUris: List<Uri>,
    onPickMedia: () -> Unit,
    onRemove: (Uri) -> Unit,
    onTrimVideo: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VibeCardDark)
                    .clickable(onClick = onPickMedia),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.AddPhotoAlternate, null, tint = VibeTextSecondary, modifier = Modifier.size(28.dp))
                    Text("Add Media", style = MaterialTheme.typography.labelSmall, color = VibeTextSecondary)
                }
            }
        }
        items(selectedUris) { uri ->
            Box(modifier = Modifier.size(90.dp)) {
                AsyncImage(
                    model           = uri,
                    contentDescription = null,
                    contentScale    = ContentScale.Crop,
                    modifier        = Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)),
                )
                IconButton(
                    onClick   = { onRemove(uri) },
                    modifier  = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(0.6f), CircleShape),
                ) {
                    Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
                // Scissors button for videos
                IconButton(
                    onClick   = onTrimVideo,
                    modifier  = Modifier.align(Alignment.BottomStart).size(24.dp).padding(2.dp).background(Color.Black.copy(0.6f), CircleShape),
                ) {
                    Icon(Icons.Filled.ContentCut, null, tint = VibeGold, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
fun PostOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    hasValue: Boolean = false,
    onClear: (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(label, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, color = if (hasValue) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(0.6f)) },
        leadingContent  = { Icon(icon, null, tint = if (hasValue) VibePink else MaterialTheme.colorScheme.onSurface.copy(0.5f)) },
        trailingContent = {
            Row {
                if (hasValue && onClear != null) {
                    IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Cancel, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.size(16.dp))
                    }
                }
                Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.3f))
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.3.dp)
}

@Composable
fun TextPostPreview(
    content: String,
    background: TextPostBackground,
    font: VibeFont,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(
                when (background) {
                    is TextPostBackground.Gradient -> Modifier.background(background.brush)
                    is TextPostBackground.Solid    -> Modifier.background(background.color)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (content.isBlank()) {
            Text("Tap to write something…", color = Color.White.copy(0.5f), style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(content, color = Color.White, fontFamily = font.family, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(24.dp))
        }
    }
}
