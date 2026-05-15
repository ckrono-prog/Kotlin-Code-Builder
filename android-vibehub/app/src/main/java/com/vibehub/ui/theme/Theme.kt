package com.vibehub.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ──────────────────────────────────────────────────────────
// VibeHub brand palette
// ──────────────────────────────────────────────────────────
val VibePink       = Color(0xFFE8356D)
val VibeOrange     = Color(0xFFF4722B)
val VibeGold       = Color(0xFFF9B234)
val VibeDarkBg     = Color(0xFF0D0D0D)
val VibeCardDark   = Color(0xFF1A1A1A)
val VibeCardLight  = Color(0xFFF5F5F5)
val VibeGlassLight = Color(0x26FFFFFF)
val VibeGlassDark  = Color(0x1AFFFFFF)
val VibeNeonPink   = Color(0xFFFF2D78)
val VibeNeonPurple = Color(0xFFB94FFF)
val VibeTextPrimary   = Color(0xFFFFFFFF)
val VibeTextSecondary = Color(0xFFB0B0B0)
val VibeTextMuted     = Color(0xFF666666)
val VibeSuccess = Color(0xFF00C853)
val VibeError   = Color(0xFFFF1744)

/** Reusable gradient brush used across the app */
val VibeGradient = Brush.linearGradient(listOf(VibePink, VibeOrange, VibeGold))
val VibeGradientVertical = Brush.verticalGradient(listOf(VibePink, VibeOrange, VibeGold))
val VibeGradientSoft = Brush.linearGradient(listOf(Color(0xFFFF6B9D), Color(0xFFFF8E53)))

private val DarkColorScheme = darkColorScheme(
    primary        = VibePink,
    onPrimary      = Color.White,
    secondary      = VibeOrange,
    onSecondary    = Color.White,
    tertiary       = VibeGold,
    background     = VibeDarkBg,
    onBackground   = VibeTextPrimary,
    surface        = VibeCardDark,
    onSurface      = VibeTextPrimary,
    surfaceVariant = Color(0xFF252525),
    error          = VibeError,
)

private val LightColorScheme = lightColorScheme(
    primary        = VibePink,
    onPrimary      = Color.White,
    secondary      = VibeOrange,
    onSecondary    = Color.White,
    tertiary       = VibeGold,
    background     = Color(0xFFFAFAFA),
    onBackground   = Color(0xFF1A1A1A),
    surface        = Color.White,
    onSurface      = Color(0xFF1A1A1A),
    surfaceVariant = VibeCardLight,
    error          = VibeError,
)

@Composable
fun VibeHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = VibeTypography,
        content     = content
    )
}
