package com.vibehub.ui.screens.video

import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.vibehub.ui.components.GradientButton
import com.vibehub.ui.theme.*
import com.vibehub.util.CameraPermissionGate
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@Composable
fun VideoCaptureScreen(onBack: () -> Unit, onVideoSaved: (Uri) -> Unit) {
    CameraPermissionGate {
        VideoCaptureContent(onBack = onBack, onVideoSaved = onVideoSaved)
    }
}

@Composable
private fun VideoCaptureContent(onBack: () -> Unit, onVideoSaved: (Uri) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State
    var isRecording by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var speedMultiplier by remember { mutableFloatStateOf(1f) }
    var autoCaptionEnabled by remember { mutableStateOf(false) }
    var selectedBeauty by remember { mutableIntStateOf(0) }
    var selectedFilter by remember { mutableIntStateOf(0) }
    var showFilterPanel by remember { mutableStateOf(false) }
    var showSpeedPanel by remember { mutableStateOf(false) }
    var showBeautyPanel by remember { mutableStateOf(false) }
    var activeMode by remember { mutableStateOf(CaptureMode.VIDEO) }
    var beautyIntensity by remember { mutableFloatStateOf(0.5f) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    // Timer while recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingSeconds++
            }
        } else {
            recordingSeconds = 0
        }
    }

    // Setup CameraX
    val previewView = remember { PreviewView(context) }
    LaunchedEffect(isFrontCamera) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            val selector = if (isFrontCamera)
                CameraSelector.DEFAULT_FRONT_CAMERA
            else
                CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, selector, preview, videoCapture,
                )
            } catch (e: Exception) { e.printStackTrace() }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Camera preview
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Soft color filter overlay (beauty / filters)
        if (selectedFilter > 0) {
            val filterColor = FilterColors.getOrElse(selectedFilter - 1) { Color.Transparent }
            Box(modifier = Modifier.fillMaxSize().background(filterColor))
        }

        // ── Top controls ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CameraIconBtn(Icons.Filled.Close, onClick = onBack)
            if (isRecording) {
                RecordingTimer(seconds = recordingSeconds)
            } else {
                Spacer(Modifier.width(40.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CameraIconBtn(
                    icon = if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    onClick = {
                        isFlashOn = !isFlashOn
                        camera?.cameraControl?.enableTorch(isFlashOn)
                    },
                )
                CameraIconBtn(Icons.Outlined.Settings, onClick = {})
            }
        }

        // ── Mode selector ────────────────────────────────────────────
        if (!isRecording) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CaptureMode.values().forEach { mode ->
                    TextButton(onClick = { activeMode = mode }) {
                        Text(
                            text = mode.label,
                            color = if (activeMode == mode) VibeGold else Color.White.copy(0.6f),
                            fontWeight = if (activeMode == mode) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }

        // ── Right side tools ──────────────────────────────────────────
        if (!isRecording) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SideTool(icon = Icons.Outlined.AutoFixHigh, label = "Beauty",
                    active = showBeautyPanel) { showBeautyPanel = !showBeautyPanel; showFilterPanel = false; showSpeedPanel = false }
                SideTool(icon = Icons.Outlined.Tune, label = "Filters",
                    active = showFilterPanel) { showFilterPanel = !showFilterPanel; showBeautyPanel = false; showSpeedPanel = false }
                SideTool(icon = Icons.Outlined.Speed, label = "Speed",
                    active = showSpeedPanel) { showSpeedPanel = !showSpeedPanel; showBeautyPanel = false; showFilterPanel = false }
                SideTool(icon = Icons.Outlined.ClosedCaption,
                    label = if (autoCaptionEnabled) "CC On" else "CC Off",
                    active = autoCaptionEnabled) { autoCaptionEnabled = !autoCaptionEnabled }
                SideTool(icon = Icons.Outlined.FlipCameraAndroid, label = "Flip",
                    active = false) { isFrontCamera = !isFrontCamera }
                SideTool(icon = Icons.Outlined.MusicNote, label = "Sound", active = false) {}
                SideTool(icon = Icons.Outlined.Timer, label = "Timer", active = false) {}
            }
        }

        // ── Bottom panel ────────────────────────────────────────────
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Expandable panels
            AnimatedVisibility(visible = showSpeedPanel) {
                SpeedPanel(selected = speedMultiplier, onSelect = { speedMultiplier = it })
            }
            AnimatedVisibility(visible = showBeautyPanel) {
                BeautyPanel(intensity = beautyIntensity, onIntensityChange = { beautyIntensity = it })
            }
            AnimatedVisibility(visible = showFilterPanel) {
                FilterPanel(selected = selectedFilter, onSelect = { selectedFilter = it })
            }

            // Auto-caption indicator
            AnimatedVisibility(visible = autoCaptionEnabled) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(VibeGold.copy(0.85f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.ClosedCaption, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Auto Captions ON", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }

            // Record button row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Gallery picker
                Box(
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(0.2f)).clickable {},
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, null, tint = Color.White)
                }

                // Record / capture button
                RecordButton(
                    isRecording = isRecording,
                    mode = activeMode,
                    onClick = {
                        if (activeMode == CaptureMode.PHOTO) {
                            // trigger photo capture
                        } else {
                            if (isRecording) {
                                activeRecording?.stop()
                                isRecording = false
                            } else {
                                val vc = videoCapture ?: return@RecordButton
                                val outputFile = File(
                                    context.getExternalFilesDir(null),
                                    "VH_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4",
                                )
                                val outputOptions = FileOutputOptions.Builder(outputFile).build()
                                activeRecording = vc.output
                                    .prepareRecording(context, outputOptions)
                                    .start(ContextCompat.getMainExecutor(context)) { event ->
                                        when (event) {
                                            is VideoRecordEvent.Finalize -> {
                                                if (!event.hasError()) {
                                                    onVideoSaved(event.outputResults.outputUri)
                                                }
                                            }
                                        }
                                    }
                                isRecording = true
                            }
                        }
                    },
                )

                // Flip camera
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape)
                        .background(Color.White.copy(0.2f)).clickable { isFrontCamera = !isFrontCamera },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.FlipCameraAndroid, null, tint = Color.White)
                }
            }
        }
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────────

@Composable
private fun RecordButton(isRecording: Boolean, mode: CaptureMode, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (isRecording) 0.85f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "rec_scale")
    Box(
        modifier = Modifier.size(80.dp).scale(scale).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(80.dp).clip(CircleShape).border(4.dp, Color.White, CircleShape))
        Box(
            modifier = Modifier
                .size(if (isRecording) 32.dp else 64.dp)
                .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                .background(
                    if (mode == CaptureMode.PHOTO) Color.White
                    else if (isRecording) VibePink else VibePink
                ),
        )
    }
}

@Composable
private fun RecordingTimer(seconds: Int) {
    val mins = seconds / 60
    val secs = seconds % 60
    val blinkAlpha by rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 1f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "blink_val",
    )
    Row(
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(0.5f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(VibePink.copy(alpha = blinkAlpha)))
        Spacer(Modifier.width(6.dp))
        Text("%02d:%02d".format(mins, secs), color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SpeedPanel(selected: Float, onSelect: (Float) -> Unit) {
    val speeds = listOf(0.3f, 0.5f, 1f, 2f, 3f)
    val labels = listOf("0.3x", "0.5x", "1x", "2x", "3x")
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.Black.copy(0.6f)).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        speeds.forEachIndexed { i, speed ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected == speed) VibeGradient else Brush.linearGradient(listOf(Color.White.copy(0.15f), Color.White.copy(0.15f))))
                    .clickable { onSelect(speed) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(labels[i], color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BeautyPanel(intensity: Float, onIntensityChange: (Float) -> Unit) {
    val features = listOf("Smooth", "Brighten", "Slim", "Teeth", "Eyes")
    Column(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(0.6f)).padding(16.dp)) {
        Text("Beauty", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            features.forEach { feat ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(0.2f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.AutoFixHigh, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Text(feat, color = Color.White.copy(0.8f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Intensity", color = Color.White.copy(0.7f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(64.dp))
            Slider(value = intensity, onValueChange = onIntensityChange, modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = VibePink, activeTrackColor = VibePink))
            Text("${(intensity * 100).toInt()}%", color = Color.White.copy(0.7f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(36.dp))
        }
    }
}

@Composable
private fun FilterPanel(selected: Int, onSelect: (Int) -> Unit) {
    val names = listOf("None", "Warm", "Cool", "B&W", "Vivid", "Fade", "Neon")
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.Black.copy(0.6f)).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        names.forEachIndexed { i, name ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(i) },
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape)
                        .background(FilterColors.getOrElse(i - 1) { Color.White.copy(0.15f) })
                        .border(if (selected == i) 2.5.dp else 0.dp, Color.White, CircleShape),
                )
                Spacer(Modifier.height(4.dp))
                Text(name, color = if (selected == i) Color.White else Color.White.copy(0.5f),
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SideTool(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(modifier = Modifier.size(42.dp).clip(CircleShape)
            .background(if (active) VibePink else Color.Black.copy(0.45f)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(label, color = Color.White.copy(0.8f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CameraIconBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(0.4f))) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

enum class CaptureMode(val label: String) {
    PHOTO("Photo"), VIDEO("Video"), REEL("Reel"), LIVE("Live")
}

private val FilterColors = listOf(
    Color(0x30FF9800), // Warm
    Color(0x300D47A1), // Cool
    Color(0x50424242), // B&W
    Color(0x40E91E63), // Vivid
    Color(0x20BDBDBD), // Fade
    Color(0x40B94FFF), // Neon
)
