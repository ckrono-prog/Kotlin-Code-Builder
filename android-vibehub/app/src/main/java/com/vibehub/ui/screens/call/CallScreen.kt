package com.vibehub.ui.screens.call

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.vibehub.domain.model.User
import com.vibehub.ui.components.VibeAvatar
import com.vibehub.ui.theme.*
import kotlinx.coroutines.delay

enum class CallType { AUDIO, VIDEO }
enum class CallState { RINGING, CONNECTING, ACTIVE, ENDED }

@Composable
fun CallScreen(
    remoteUser: User,
    callType: CallType,
    callState: CallState = CallState.RINGING,
    onEndCall: () -> Unit,
    onAccept: () -> Unit = {},
    isIncoming: Boolean = false,
) {
    var currentState by remember { mutableStateOf(callState) }
    var durationSeconds by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(callType == CallType.VIDEO) }
    var isCameraOff by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(true) }

    LaunchedEffect(currentState) {
        if (currentState == CallState.ACTIVE) {
            while (currentState == CallState.ACTIVE) {
                delay(1000)
                durationSeconds++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (callType == CallType.VIDEO && !isCameraOff) Color.Black
                else Brush.verticalGradient(listOf(Color(0xFF1A0A2E), Color(0xFF16213E), Color(0xFF0F3460)))
            ),
    ) {
        // Remote video (or avatar for audio call)
        if (callType == CallType.VIDEO && currentState == CallState.ACTIVE && !isCameraOff) {
            // Placeholder — wire up actual WebRTC/Supabase Realtime here
            AsyncImage(
                model = remoteUser.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Gradient overlay
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Black.copy(0.6f), Color.Transparent, Color.Transparent, Color.Black.copy(0.8f)))
        ))

        // Self-view thumbnail (video call)
        if (callType == CallType.VIDEO && currentState == CallState.ACTIVE) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(width = 90.dp, height = 130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VibeCardDark)
                    .border(2.dp, Color.White.copy(0.3f), RoundedCornerShape(12.dp))
                    .clickable { isFrontCamera = !isFrontCamera },
                contentAlignment = Alignment.Center,
            ) {
                if (isCameraOff) {
                    Icon(Icons.Filled.VideocamOff, null, tint = Color.White.copy(0.5f))
                } else {
                    // Placeholder for self-view camera feed
                    Text("You", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                }
            }
        }

        // Center content
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Pulsing avatar ring (ringing state)
            val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
                initialValue = 1f, targetValue = 1.15f,
                animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "pulse_val",
            )
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .then(if (currentState == CallState.RINGING) Modifier.scale(pulseScale) else Modifier)
                    .clip(CircleShape)
                    .background(VibeGradient),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = remoteUser.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(122.dp).clip(CircleShape),
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(remoteUser.displayName, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = Color.White)
                if (remoteUser.isVerified) {
                    Icon(Icons.Filled.Verified, null, tint = Color(0xFF1DA1F2), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = when (currentState) {
                    CallState.RINGING     -> if (isIncoming) "Incoming ${callType.name.lowercase()} call..." else "Calling..."
                    CallState.CONNECTING  -> "Connecting..."
                    CallState.ACTIVE      -> formatDuration(durationSeconds)
                    CallState.ENDED       -> "Call ended"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(0.75f),
            )
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Controls row
            if (currentState == CallState.ACTIVE || (!isIncoming && currentState == CallState.RINGING)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CallControlButton(
                        icon  = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        label = if (isMuted) "Unmute" else "Mute",
                        color = if (isMuted) VibePink else Color.White.copy(0.2f),
                        onClick = { isMuted = !isMuted },
                    )
                    if (callType == CallType.VIDEO) {
                        CallControlButton(
                            icon  = if (isCameraOff) Icons.Filled.VideocamOff else Icons.Filled.Videocam,
                            label = if (isCameraOff) "Show" else "Hide",
                            color = if (isCameraOff) VibePink else Color.White.copy(0.2f),
                            onClick = { isCameraOff = !isCameraOff },
                        )
                    }
                    CallControlButton(
                        icon  = if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        label = "Speaker",
                        color = if (isSpeakerOn) VibeGold else Color.White.copy(0.2f),
                        onClick = { isSpeakerOn = !isSpeakerOn },
                    )
                    if (callType == CallType.VIDEO) {
                        CallControlButton(
                            icon  = Icons.Outlined.FlipCameraAndroid,
                            label = "Flip",
                            color = Color.White.copy(0.2f),
                            onClick = { isFrontCamera = !isFrontCamera },
                        )
                    }
                }
            }

            // Main action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isIncoming && currentState == CallState.RINGING) {
                    // Accept button
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape).background(VibeSuccess).clickable { onAccept() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (callType == CallType.VIDEO) Icons.Filled.Videocam else Icons.Filled.Call,
                            null, tint = Color.White, modifier = Modifier.size(32.dp)
                        )
                    }
                }
                // End call
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(VibeError).clickable(onClick = onEndCall),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.CallEnd, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
    }
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
