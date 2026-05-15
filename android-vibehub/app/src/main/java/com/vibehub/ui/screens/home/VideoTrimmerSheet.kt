package com.vibehub.ui.screens.home

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.vibehub.ui.theme.*
import kotlin.math.roundToInt

/** Maximum allowed video duration in seconds (2 min = 120 s). */
const val MAX_VIDEO_DURATION_SEC = 120

/**
 * Bottom sheet video trimmer.
 * Displays a draggable timeline with start/end handles.
 * Videos longer than MAX_VIDEO_DURATION_SEC trigger an auto-trim offer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTrimmerSheet(
    uri: Uri,
    onDismiss: () -> Unit,
    onTrimmed: (startMs: Long, endMs: Long) -> Unit,
    estimatedDurationSec: Int = 180,  // would come from MediaMetadataRetriever in real app
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val totalSec   = estimatedDurationSec
    val maxSec     = MAX_VIDEO_DURATION_SEC

    var startSec by remember { mutableFloatStateOf(0f) }
    var endSec   by remember { mutableFloatStateOf(minOf(totalSec.toFloat(), maxSec.toFloat())) }

    val isOverLimit = totalSec > maxSec
    val trimDuration = (endSec - startSec).roundToInt()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Trim Video", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

            if (isOverLimit) {
                Card(
                    shape  = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VibeError.copy(0.12f)),
                    border = BorderStroke(1.dp, VibeError.copy(0.3f)),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = VibeError, modifier = Modifier.size(18.dp))
                        Column {
                            Text("Video is ${totalSec}s — limit is ${maxSec}s", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, color = VibeError)
                            Text("Auto-trimmed to first ${maxSec}s. Adjust handles below.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        }
                    }
                }
            }

            // Thumbnail strip (placeholder)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(VibeCardDark),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)))
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.35f)))
                Icon(Icons.Filled.VideoFile, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(32.dp))
            }

            // Range slider
            RangeSliderRow(
                totalSec  = totalSec.toFloat(),
                maxSec    = maxSec.toFloat(),
                startSec  = startSec,
                endSec    = endSec,
                onStartChange = { startSec = it },
                onEndChange   = { endSec   = it },
            )

            // Time labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatSec(startSec.roundToInt()), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = VibePink)
                Text("Duration: ${trimDuration}s", style = MaterialTheme.typography.labelMedium)
                Text(formatSec(endSec.roundToInt()), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = VibePink)
            }

            // Duration warning
            if (trimDuration > maxSec) {
                Text("Selection is ${trimDuration}s — still over 2 min limit. Shorten it.", color = VibeError, style = MaterialTheme.typography.labelSmall)
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(28.dp)) {
                    Text("Cancel")
                }
                Button(
                    onClick  = { onTrimmed(startSec.toLong() * 1000L, endSec.toLong() * 1000L) },
                    enabled  = trimDuration in 1..maxSec,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(28.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = VibePink),
                ) { Text("Apply Trim", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun RangeSliderRow(
    totalSec: Float,
    maxSec: Float,
    startSec: Float,
    endSec: Float,
    onStartChange: (Float) -> Unit,
    onEndChange: (Float) -> Unit,
) {
    Column {
        Text("Start", style = MaterialTheme.typography.labelSmall, color = VibeTextSecondary)
        Slider(
            value         = startSec,
            onValueChange = { v ->
                if (v < endSec - 5f && v >= 0f) onStartChange(v)
            },
            valueRange    = 0f..totalSec,
            colors        = SliderDefaults.colors(thumbColor = VibePink, activeTrackColor = VibePink),
        )
        Text("End", style = MaterialTheme.typography.labelSmall, color = VibeTextSecondary)
        Slider(
            value         = endSec,
            onValueChange = { v ->
                if (v > startSec + 5f && v <= minOf(totalSec, startSec + maxSec)) onEndChange(v)
            },
            valueRange    = 0f..totalSec,
            colors        = SliderDefaults.colors(thumbColor = VibeOrange, activeTrackColor = VibeOrange),
        )
    }
}

private fun formatSec(secs: Int): String {
    val m = secs / 60
    val s = secs % 60
    return "%d:%02d".format(m, s)
}
