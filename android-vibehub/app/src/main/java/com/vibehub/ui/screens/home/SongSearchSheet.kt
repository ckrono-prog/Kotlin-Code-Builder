package com.vibehub.ui.screens.home

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vibehub.domain.model.MusicTrack
import com.vibehub.ui.theme.*
import com.vibehub.ui.viewmodel.SongSearchViewModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Remote model returned by songs Edge Function (Jamendo) ───────────────────

@Serializable
data class RemoteSong(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    @SerialName("cover_url")    val coverUrl: String = "",
    @SerialName("preview_url")  val previewUrl: String = "",
    @SerialName("duration_sec") val durationSec: Int = 0,
    val license: String = "",
) {
    fun toTrack() = MusicTrack(
        id              = id,
        title           = title,
        artist          = artist,
        coverUrl        = coverUrl,
        previewUrl      = previewUrl,
        durationSeconds = durationSec,
    )
}

// ─── Sheet ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongSearchSheet(
    onSongSelected: (MusicTrack) -> Unit,
    onDismiss: () -> Unit,
    viewModel: SongSearchViewModel = hiltViewModel(),
) {
    val uiState    by viewModel.uiState.collectAsState()
    val sheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        dragHandle       = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.85f).navigationBarsPadding()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.MusicNote, null, tint = VibePink)
                Spacer(Modifier.width(8.dp))
                Text("Add Music", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text("Jamendo CC Music", style = MaterialTheme.typography.labelSmall, color = VibeTextSecondary)
            }

            // Search bar
            OutlinedTextField(
                value         = uiState.query,
                onValueChange = { viewModel.onQueryChange(it) },
                placeholder   = { Text("Search free licensed music…") },
                leadingIcon   = { Icon(Icons.Outlined.Search, null) },
                trailingIcon  = {
                    if (uiState.query.isNotBlank()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) { Icon(Icons.Filled.Close, null) }
                    }
                },
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape     = RoundedCornerShape(24.dp),
                singleLine = true,
                colors    = OutlinedTextFieldDefaults.colors(focusedBorderColor = VibePink),
            )

            // Content
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(color = VibePink)
                            Text("Searching Jamendo…", style = MaterialTheme.typography.bodySmall, color = VibeTextSecondary)
                        }
                    }
                }
                uiState.error != null -> {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(uiState.error ?: "Error", color = VibeError, style = MaterialTheme.typography.bodySmall)
                    }
                }
                uiState.tracks.isEmpty() && uiState.query.length >= 2 -> {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No tracks found for "${uiState.query}"", color = VibeTextSecondary)
                    }
                }
                uiState.tracks.isEmpty() -> {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.LibraryMusic, null, tint = VibePink, modifier = Modifier.size(40.dp))
                            Text("Search for any song or artist", color = VibeTextSecondary, style = MaterialTheme.typography.bodyMedium)
                            Text("Free CC-licensed music from Jamendo", style = MaterialTheme.typography.labelSmall, color = VibeTextSecondary.copy(0.6f))
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding     = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(uiState.tracks, key = { it.id }) { song ->
                            SongRow(
                                song      = song,
                                isPlaying = uiState.playingId == song.id,
                                onPlay    = { viewModel.togglePreview(song) },
                                onSelect  = { onSongSelected(song.toTrack()); onDismiss() },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongRow(
    song: RemoteSong,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPlaying) VibePink.copy(0.08f) else Color.Transparent)
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Album art
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))) {
            if (song.coverUrl.isNotBlank()) {
                AsyncImage(model = song.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize().background(VibePink.copy(0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MusicNote, null, tint = VibePink, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Meta
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = VibeTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${song.durationSec / 60}:${"%02d".format(song.durationSec % 60)}",
                    style = MaterialTheme.typography.labelSmall, color = VibeTextSecondary,
                )
                Text("•", style = MaterialTheme.typography.labelSmall, color = VibeTextSecondary)
                Icon(Icons.Outlined.Copyright, null, tint = VibeSuccess, modifier = Modifier.size(10.dp))
                Text("Free", style = MaterialTheme.typography.labelSmall, color = VibeSuccess)
            }
        }

        // Preview play
        IconButton(onClick = onPlay) {
            Icon(
                if (isPlaying) Icons.Filled.PauseCircle else Icons.Outlined.PlayCircle,
                null,
                tint     = if (isPlaying) VibePink else MaterialTheme.colorScheme.onSurface.copy(0.45f),
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
