package com.vibehub.ui.screens.messages

import android.Manifest
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.vibehub.ui.theme.*
import kotlinx.coroutines.delay

/** Draggable mic button with slide-to-cancel, waveform animation, and playback preview. */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceNoteButton(
    onVoiceNoteSend: (durationMs: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    var isRecording  by remember { mutableStateOf(false) }
    var elapsedMs    by remember { mutableLongStateOf(0L) }
    var isCancelled  by remember { mutableStateOf(false) }
    var slideOffset  by remember { mutableFloatStateOf(0f) }

    // Timer tick while recording
    LaunchedEffect(isRecording) {
        elapsedMs = 0L
        if (isRecording) {
            while (isRecording) {
                delay(100L)
                elapsedMs += 100L
            }
        }
    }

    val cancelThreshold = -120f
    val shouldCancel = slideOffset < cancelThreshold

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Slide-to-cancel hint
        AnimatedVisibility(visible = isRecording) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.ChevronLeft, null, tint = if (shouldCancel) VibeError else VibeTextSecondary, modifier = Modifier.size(16.dp))
                Text(if (shouldCancel) "Release to cancel" else "Slide to cancel", style = MaterialTheme.typography.labelSmall, color = if (shouldCancel) VibeError else VibeTextSecondary)
                Spacer(Modifier.width(8.dp))
                RecordingWaveform(elapsedMs)
                Spacer(Modifier.width(8.dp))
                Text(formatVoiceDuration(elapsedMs), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = VibeError)
            }
        }

        // Mic button (hold to record)
        val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 1f, targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulse_scale",
        )

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isRecording) VibeError else VibeCardDark)
                .scale(if (isRecording) pulse else 1f)
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { _ ->
                            if (micPermission.status.isGranted) {
                                isRecording = true; isCancelled = false; slideOffset = 0f
                            } else {
                                micPermission.launchPermissionRequest()
                            }
                        },
                        onDrag = { _, dragAmount ->
                            slideOffset += dragAmount.x
                        },
                        onDragEnd = {
                            if (isRecording) {
                                isRecording = false
                                if (!shouldCancel && elapsedMs > 500L) {
                                    onVoiceNoteSend(elapsedMs)
                                }
                            }
                            slideOffset = 0f
                        },
                        onDragCancel = {
                            isRecording = false; slideOffset = 0f
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Mic, null, tint = if (isRecording) Color.White else VibeTextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun VoiceNotePlayback(
    durationMs: Long,
    waveformData: List<Float> = List(40) { (0.2f + Math.random().toFloat() * 0.8f) },
    isFromMe: Boolean,
) {
    var isPlaying by remember { mutableStateOf(false) }
    var progressFraction by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val steps = 100
            repeat(steps) {
                delay(durationMs / steps)
                progressFraction = (it + 1) / steps.toFloat()
            }
            isPlaying = false
            progressFraction = 0f
        }
    }

    val tint = if (isFromMe) Color.White else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.width(220.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(
            onClick  = { isPlaying = !isPlaying },
            modifier = Modifier.size(36.dp).clip(CircleShape).background(tint.copy(0.15f)),
        ) {
            Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = tint)
        }

        Box(modifier = Modifier.weight(1f).height(36.dp), contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                waveformData.forEachIndexed { idx, amp ->
                    val isFilled = idx / waveformData.size.toFloat() <= progressFraction
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(amp.coerceIn(0.1f, 1f))
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isFilled) VibePink else tint.copy(0.3f)),
                    )
                }
            }
        }

        Text(formatVoiceDuration(durationMs), style = MaterialTheme.typography.labelSmall, color = tint.copy(0.7f))
    }
}

// Animated waveform bars while recording
@Composable
private fun RecordingWaveform(elapsedMs: Long) {
    val bars = 12
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(bars) { idx ->
            val transition = rememberInfiniteTransition(label = "wave$idx")
            val height by transition.animateFloat(
                initialValue = 4.dp.value,
                targetValue  = (8 + (idx % 4) * 5).dp.value,
                animationSpec = infiniteRepeatable(
                    tween(200 + idx * 40, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse,
                ),
                label = "bar$idx",
            )
            Box(
                modifier = Modifier.width(2.dp).height(height.dp).clip(RoundedCornerShape(2.dp)).background(VibeError),
            )
        }
    }
}

private fun formatVoiceDuration(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
