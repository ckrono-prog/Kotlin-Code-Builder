package com.vibehub.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibehub.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage & Downloads", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {

            // ── Auto Download ──────────────────────────────────────────────
            item {
                SectionHeader("Auto Download")
            }
            item {
                SettingToggleRow(
                    icon    = Icons.Outlined.Download,
                    title   = "Auto-download videos",
                    subtitle = "Automatically download unwatched videos for offline viewing",
                    checked = uiState.autoDownloadEnabled,
                    onCheckedChange = viewModel::setAutoDownloadEnabled,
                )
            }
            item {
                SettingToggleRow(
                    icon    = Icons.Outlined.Wifi,
                    title   = "Download over Wi-Fi",
                    subtitle = "Download when connected to Wi-Fi",
                    checked = uiState.downloadOverWifi,
                    enabled = uiState.autoDownloadEnabled,
                    onCheckedChange = viewModel::setDownloadOverWifi,
                )
            }
            item {
                SettingToggleRow(
                    icon    = Icons.Outlined.NetworkCell,
                    title   = "Download over mobile data",
                    subtitle = "May use your cellular data allowance",
                    checked = uiState.downloadOverMobile,
                    enabled = uiState.autoDownloadEnabled,
                    onCheckedChange = viewModel::setDownloadOverMobile,
                )
            }

            // ── Cache Limit ────────────────────────────────────────────────
            item { SectionHeader("Storage Limit") }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Max video cache", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("${uiState.maxCacheMb} MB", style = MaterialTheme.typography.bodyMedium, color = VibePink, fontWeight = FontWeight.SemiBold)
                    }
                    Slider(
                        value         = uiState.maxCacheMb.toFloat(),
                        onValueChange = { viewModel.setMaxCacheMb(it.toInt()) },
                        valueRange    = 100f..2000f,
                        steps         = 18,
                        colors        = SliderDefaults.colors(thumbColor = VibePink, activeTrackColor = VibePink),
                    )
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("100 MB", style = MaterialTheme.typography.labelSmall, color = VibeTextMuted)
                        Text("2 GB", style = MaterialTheme.typography.labelSmall, color = VibeTextMuted)
                    }
                }
            }

            // ── Storage Usage ──────────────────────────────────────────────
            item { SectionHeader("Current Storage") }
            item {
                StorageUsageCard(
                    downloadedMb = uiState.downloadedSizeMb,
                    cachedMb     = uiState.cachedSizeMb,
                )
            }

            // ── Clear actions ──────────────────────────────────────────────
            item { SectionHeader("Manage Storage") }
            item {
                ListItem(
                    headlineContent   = { Text("Clear downloaded videos", color = VibeError) },
                    supportingContent = { Text("${uiState.downloadedSizeMb} MB of downloaded videos will be deleted") },
                    leadingContent    = { Icon(Icons.Outlined.DeleteOutline, null, tint = VibeError) },
                    modifier          = Modifier.clickable(onClick = { viewModel.clearDownloads() }),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.3.dp)
            }
            item {
                ListItem(
                    headlineContent   = { Text("Clear video cache", color = VibeError) },
                    supportingContent = { Text("${uiState.cachedSizeMb} MB of cached stream data will be cleared") },
                    leadingContent    = { Icon(Icons.Outlined.CleaningServices, null, tint = VibeError) },
                    modifier          = Modifier.clickable(onClick = { viewModel.clearCache() }),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.3.dp)
            }
            item {
                ListItem(
                    headlineContent   = { Text("Clear all local data") },
                    supportingContent = { Text("Removes all downloads and cached data") },
                    leadingContent    = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.5f)) },
                    modifier          = Modifier.clickable(onClick = { viewModel.clearAll() }),
                )
            }
        }
    }
}

@Composable
private fun StorageUsageCard(downloadedMb: Int, cachedMb: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StorageBar(label = "Downloaded videos", valueMb = downloadedMb, maxMb = 2000, color = VibePink)
            StorageBar(label = "Stream cache", valueMb = cachedMb, maxMb = 500, color = VibeGold)
        }
    }
}

@Composable
private fun StorageBar(label: String, valueMb: Int, maxMb: Int, color: androidx.compose.ui.graphics.Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("$valueMb MB", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = color)
        }
        LinearProgressIndicator(
            progress  = { (valueMb.toFloat() / maxMb).coerceIn(0f, 1f) },
            modifier  = Modifier.fillMaxWidth().height(6.dp),
            color     = color,
            trackColor = MaterialTheme.colorScheme.outline.copy(0.2f),
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style    = MaterialTheme.typography.labelMedium,
        color    = VibePink,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun SettingToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent   = { Text(title, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(0.4f)) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(if (enabled) 0.5f else 0.3f)) },
        leadingContent    = { Icon(icon, null, tint = if (enabled) VibePink else MaterialTheme.colorScheme.onSurface.copy(0.3f)) },
        trailingContent   = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors  = SwitchDefaults.colors(checkedTrackColor = VibePink),
            )
        },
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.3.dp)
}

private fun androidx.compose.foundation.layout.BoxScope.clickable(onClick: () -> Unit): Modifier = Modifier
