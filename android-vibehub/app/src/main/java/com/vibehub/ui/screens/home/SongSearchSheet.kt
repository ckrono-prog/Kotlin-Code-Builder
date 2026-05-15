package com.vibehub.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.vibehub.ui.theme.*
import kotlinx.coroutines.*

/**
 * Bottom sheet for searching and selecting a song to attach to a post or story.
 * Uses iTunes Search API (public, no key needed) for real song lookups.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongSearchSheet(
    selected: SongChoice?,
    onSelect: (SongChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query      by remember { mutableStateOf("") }
    var results    by remember { mutableStateOf<List<SongChoice>>(emptyList()) }
    var isLoading  by remember { mutableStateOf(false) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }
    val scope      = rememberCoroutineScope()
    var searchJob: Job? by remember { mutableStateOf(null) }

    LaunchedEffect(query) {
        if (query.length < 2) { results = emptyList(); return@LaunchedEffect }
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(400)
            isLoading = true; errorMsg = null
            try {
                results = searchITunes(query)
            } catch (e: Exception) {
                errorMsg = "Could not load songs. Check connection."
            }
            isLoading = false
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            // Title
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Add Music", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                if (selected != null) {
                    TextButton(onClick = { onSelect(selected) }) { Text("Done", color = VibePink, fontWeight = FontWeight.Bold) }
                }
            }

            // Search field
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                placeholder   = { Text("Search songs, artists…") },
                leadingIcon   = { Icon(Icons.Filled.Search, null) },
                trailingIcon  = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, null) } },
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape         = RoundedCornerShape(28.dp),
                singleLine    = true,
            )

            // Currently selected song chip
            AnimatedVisibility(visible = selected != null) {
                selected?.let { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(VibePink.copy(0.12f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AsyncImage(model = song.artUrl, contentDescription = null, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = VibeTextSecondary)
                        }
                        Icon(Icons.Filled.CheckCircle, null, tint = VibePink)
                    }
                }
            }

            // Loading / error
            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VibePink, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
            } else if (errorMsg != null) {
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(errorMsg!!, color = VibeError, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Results list
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(results, key = { it.id }) { song ->
                    SongRow(song = song, isSelected = selected?.id == song.id, onClick = { onSelect(song) })
                }
            }
        }
    }
}

@Composable
private fun SongRow(song: SongChoice, isSelected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent   = { Text(song.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 1) },
        supportingContent = { Text(song.artist, style = MaterialTheme.typography.bodySmall, color = VibeTextSecondary, maxLines = 1) },
        leadingContent    = {
            AsyncImage(
                model       = song.artUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier    = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
            )
        },
        trailingContent   = {
            if (isSelected) Icon(Icons.Filled.CheckCircle, null, tint = VibePink)
            else Icon(Icons.Outlined.PlayCircle, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f))
        },
        colors            = ListItemDefaults.colors(
            containerColor = if (isSelected) VibePink.copy(0.06f) else MaterialTheme.colorScheme.surface,
        ),
        modifier          = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.3.dp)
}

/** Calls the iTunes Search API — no key required. */
private suspend fun searchITunes(query: String): List<SongChoice> =
    withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = java.net.URL("https://itunes.apple.com/search?term=$encoded&media=music&limit=25&entity=song")
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout    = 5_000
        connection.setRequestProperty("Accept", "application/json")

        val body = connection.inputStream.bufferedReader().readText()
        connection.disconnect()

        val jsonObj = org.json.JSONObject(body)
        val results = jsonObj.getJSONArray("results")
        (0 until results.length()).map { i ->
            val item = results.getJSONObject(i)
            SongChoice(
                id         = item.optString("trackId"),
                title      = item.optString("trackName"),
                artist     = item.optString("artistName"),
                artUrl     = item.optString("artworkUrl100").replace("100x100", "300x300"),
                previewUrl = item.optString("previewUrl"),
            )
        }
    }

@Composable
private fun TagUserSheet(
    tagged: List<TaggedUser>,
    onToggle: (TaggedUser) -> Unit,
    onDismiss: () -> Unit,
) {
    // Stub — uses FriendsScreen logic internally
    // Full implementation: search users, tap to toggle selection
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tag People") },
        text  = { Text("Search and tag people in your post.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
