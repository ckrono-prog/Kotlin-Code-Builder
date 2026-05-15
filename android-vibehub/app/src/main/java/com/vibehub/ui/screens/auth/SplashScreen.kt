package com.vibehub.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibehub.ui.components.GradientText
import com.vibehub.ui.theme.*
import com.vibehub.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Logo scale animation
    val logoScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue  = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logo_pulse",
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        delay(2000)
        if (uiState.isLoggedIn) onNavigateToMain() else onNavigateToLogin()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VibeGradientVertical),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)) + scaleIn(tween(600, easing = OvershootInterpolator(2f).toEasing())),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // App icon placeholder — replace with your actual icon
                Surface(
                    modifier = Modifier.size(100.dp).scale(logoScale),
                    shape    = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                    color    = Color.White.copy(alpha = 0.25f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("VH", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(Modifier.height(24.dp))

                GradientText(
                    text  = "VibeHub",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text  = "Share your moments. Connect with vibes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )

                Spacer(Modifier.height(48.dp))

                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
        }
    }
}

private fun OvershootInterpolator.toEasing() = Easing { fraction ->
    getInterpolation(fraction)
}
private fun OvershootInterpolator(tension: Float) = android.view.animation.OvershootInterpolator(tension)
private fun android.view.animation.OvershootInterpolator.toEasing() = Easing { fraction ->
    getInterpolation(fraction)
}
