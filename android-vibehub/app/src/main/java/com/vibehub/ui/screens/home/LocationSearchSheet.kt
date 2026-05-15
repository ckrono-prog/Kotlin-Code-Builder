package com.vibehub.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.vibehub.ui.theme.*
import kotlinx.coroutines.*
import org.json.JSONArray

/**
 * Bottom sheet for searching and tagging a real-world location.
 * Uses OpenStreetMap Nominatim — free, no API key required.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchSheet(
    onSelect: (LocationTag) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query     by remember { mutableStateOf("") }
    var results   by remember { mutableStateOf<List<LocationTag>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }
    val scope     = rememberCoroutineScope()
    var searchJob: Job? by remember { mutableStateOf(null) }

    LaunchedEffect(query) {
        if (query.length < 2) { results = emptyList(); return@LaunchedEffect }
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(500)
            isLoading = true; errorMsg = null
            try {
                results = nominatimSearch(query)
            } catch (e: Exception) {
                errorMsg = "Location search failed. Try again."
            }
            isLoading = false
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text(
                "Add Location",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                placeholder   = { Text("Search city, venue, address…") },
                leadingIcon   = { Icon(Icons.Filled.Search, null) },
                trailingIcon  = {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = VibePink)
                    else if (query.isNotEmpty()) IconButton(onClick = { query = ""; results = emptyList() }) { Icon(Icons.Filled.Clear, null) }
                },
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape         = RoundedCornerShape(28.dp),
                singleLine    = true,
            )

            if (errorMsg != null) {
                Text(errorMsg!!, color = VibeError, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(results, key = { it.placeId }) { loc ->
                    ListItem(
                        headlineContent   = { Text(loc.name, maxLines = 1, fontWeight = FontWeight.Medium) },
                        supportingContent = { Text(loc.address, maxLines = 2, style = MaterialTheme.typography.bodySmall, color = VibeTextSecondary) },
                        leadingContent    = { Icon(Icons.Filled.LocationOn, null, tint = VibePink) },
                        modifier          = Modifier.clickable { onSelect(loc) },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.3.dp)
                }
            }
        }
    }
}

/**
 * OpenStreetMap Nominatim search — returns up to 20 results.
 * Nominatim TOS: must include a descriptive User-Agent and not exceed 1 req/sec.
 */
private suspend fun nominatimSearch(query: String): List<LocationTag> =
    withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = java.net.URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=20&addressdetails=1")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 6_000
        conn.readTimeout    = 6_000
        conn.setRequestProperty("User-Agent", "VibeHub Android App contact@vibehub.app")
        conn.setRequestProperty("Accept", "application/json")

        val body = conn.inputStream.bufferedReader().readText()
        conn.disconnect()

        val arr = JSONArray(body)
        (0 until arr.length()).map { i ->
            val item = arr.getJSONObject(i)
            val addrObj = item.optJSONObject("address")
            val addressParts = listOfNotNull(
                addrObj?.optString("road")?.takeIf { it.isNotBlank() },
                addrObj?.optString("city") ?: addrObj?.optString("town") ?: addrObj?.optString("village"),
                addrObj?.optString("country"),
            )
            LocationTag(
                placeId = item.optString("place_id"),
                name    = item.optString("name").ifBlank { item.optString("display_name").take(40) },
                address = addressParts.joinToString(", ").ifBlank { item.optString("display_name").take(80) },
                lat     = item.optString("lat").toDoubleOrNull() ?: 0.0,
                lng     = item.optString("lon").toDoubleOrNull() ?: 0.0,
            )
        }
    }
