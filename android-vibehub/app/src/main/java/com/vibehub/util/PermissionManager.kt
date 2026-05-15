package com.vibehub.util

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.google.accompanist.permissions.*
import com.vibehub.ui.components.GradientButton
import com.vibehub.ui.theme.VibePink

// ─── Camera + microphone permission gate ─────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionGate(
    onGranted: @Composable () -> Unit,
) {
    val multiPermission = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
    )

    if (multiPermission.allPermissionsGranted) {
        onGranted()
    } else {
        PermissionRationaleScreen(
            icon = Icons.Outlined.CameraAlt,
            title = "Camera Access Needed",
            body = "VibeHub needs access to your camera and microphone to create videos, go live, and make video calls.",
            onRequest = { multiPermission.launchMultiplePermissionRequest() },
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StoragePermissionGate(onGranted: @Composable () -> Unit) {
    val perm = rememberPermissionState(Manifest.permission.READ_MEDIA_IMAGES)
    if (perm.status.isGranted) {
        onGranted()
    } else {
        PermissionRationaleScreen(
            icon  = Icons.Outlined.Photo,
            title = "Photo Library Access",
            body  = "VibeHub needs access to your photos to let you share them with your friends.",
            onRequest = { perm.launchPermissionRequest() },
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionGate(onGranted: @Composable () -> Unit) {
    val perm = rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    if (perm.status.isGranted) {
        onGranted()
    } else {
        PermissionRationaleScreen(
            icon  = Icons.Outlined.Notifications,
            title = "Stay in the Loop",
            body  = "Enable notifications to know when someone likes your posts, sends you a message, or goes live.",
            onRequest = { perm.launchPermissionRequest() },
        )
    }
}

@Composable
fun PermissionRationaleScreen(
    icon: ImageVector,
    title: String,
    body: String,
    onRequest: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, modifier = Modifier.size(80.dp), tint = VibePink)
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        GradientButton(text = "Allow Access", onClick = onRequest)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = {}) {
            Text("Not now", color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
        }
    }
}
