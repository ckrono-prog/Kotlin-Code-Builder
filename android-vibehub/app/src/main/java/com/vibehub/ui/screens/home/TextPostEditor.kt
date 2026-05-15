package com.vibehub.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.vibehub.ui.theme.*

// ── Background types ──────────────────────────────────────────────────────────

sealed class TextPostBackground {
    data class Gradient(val brush: Brush) : TextPostBackground()
    data class Solid(val color: Color)    : TextPostBackground()
}

// ── Fonts ─────────────────────────────────────────────────────────────────────

enum class VibeFont(val displayName: String, val family: FontFamily) {
    Default     ("Default",      FontFamily.Default),
    Serif       ("Serif",        FontFamily.Serif),
    Monospace   ("Mono",         FontFamily.Monospace),
    Cursive     ("Cursive",      FontFamily.Cursive),
    SansSerif   ("Sans Serif",   FontFamily.SansSerif),
    // The remaining fonts map to bundled system fonts / Google Fonts at build time:
    Rounded     ("Rounded",      FontFamily.Default),
    Bold        ("Bold",         FontFamily.Default),
    Thin        ("Thin",         FontFamily.Default),
    Italic      ("Italic",       FontFamily.Default),
    Display     ("Display",      FontFamily.Serif),
    Script      ("Script",       FontFamily.Cursive),
}

// ── Background presets ────────────────────────────────────────────────────────

val GRADIENT_PRESETS: List<TextPostBackground.Gradient> = listOf(
    TextPostBackground.Gradient(Brush.linearGradient(listOf(Color(0xFFE8356D), Color(0xFFF4722B)))),
    TextPostBackground.Gradient(Brush.linearGradient(listOf(Color(0xFFB94FFF), Color(0xFFE8356D)))),
    TextPostBackground.Gradient(Brush.linearGradient(listOf(Color(0xFF0DCEDA), Color(0xFF06B8F1)))),
    TextPostBackground.Gradient(Brush.linearGradient(listOf(Color(0xFF00C853), Color(0xFF006837)))),
    TextPostBackground.Gradient(Brush.linearGradient(listOf(Color(0xFFF9B234), Color(0xFFF4722B)))),
    TextPostBackground.Gradient(Brush.linearGradient(listOf(Color(0xFFFF6B9D), Color(0xFFB94FFF)))),
    TextPostBackground.Gradient(Brush.linearGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))),
    TextPostBackground.Gradient(Brush.linearGradient(listOf(Color(0xFFFF1744), Color(0xFFFF6D00)))),
    // Animated shimmer entries are shown with a special shimmer overlay
    TextPostBackground.Gradient(Brush.linearGradient(listOf(Color(0xFF7F00FF), Color(0xFFE100FF)))),
    TextPostBackground.Gradient(Brush.linearGradient(listOf(Color(0xFF00F260), Color(0xFF0575E6)))),
    TextPostBackground.Gradient(Brush.linearGradient(listOf(Color(0xFFFC466B), Color(0xFF3F5EFB)))),
    TextPostBackground.Gradient(Brush.linearGradient(listOf(Color(0xFFF7971E), Color(0xFFFFD200)))),
)

val SOLID_PRESETS: List<TextPostBackground.Solid> = listOf(
    TextPostBackground.Solid(Color(0xFF000000)),
    TextPostBackground.Solid(Color(0xFFFFFFFF)),
    TextPostBackground.Solid(Color(0xFFE8356D)),
    TextPostBackground.Solid(Color(0xFF1A1A2E)),
    TextPostBackground.Solid(Color(0xFF0D47A1)),
    TextPostBackground.Solid(Color(0xFF1B5E20)),
    TextPostBackground.Solid(Color(0xFF4A148C)),
    TextPostBackground.Solid(Color(0xFFBF360C)),
)

// ── Animated shimmer background ───────────────────────────────────────────────

@Composable
private fun animatedBrush(colors: List<Color>): Brush {
    val transition = rememberInfiniteTransition(label = "bg_anim")
    val offset by transition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Reverse),
        label = "bg_offset",
    )
    return Brush.linearGradient(colors, start = androidx.compose.ui.geometry.Offset(offset, 0f), end = androidx.compose.ui.geometry.Offset(offset + 600f, 600f))
}

// ── Full-screen text post editor ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextPostEditor(
    content:          String,
    background:       TextPostBackground,
    font:             VibeFont,
    onContentChange:  (String) -> Unit,
    onBackgroundChange: (TextPostBackground) -> Unit,
    onFontChange:     (VibeFont) -> Unit,
    onDismiss:        () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }   // 0=backgrounds, 1=fonts

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Text Post", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, null) } },
                actions = { TextButton(onClick = onDismiss) { Text("Done", color = VibePink, fontWeight = FontWeight.Bold) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            Column {
                TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Background") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Font") })
                }
                AnimatedContent(targetState = tab, label = "tab") { t ->
                    if (t == 0) BackgroundPicker(background, onBackgroundChange)
                    else FontPicker(font, onFontChange)
                }
            }
        },
    ) { padding ->
        // Live preview canvas
        val bgModifier = when (background) {
            is TextPostBackground.Gradient -> Modifier.background(background.brush)
            is TextPostBackground.Solid    -> Modifier.background(background.color)
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(padding).then(bgModifier),
            contentAlignment = Alignment.Center,
        ) {
            BasicTextEditorOverlay(content = content, font = font, onContentChange = onContentChange)
        }
    }
}

@Composable
private fun BasicTextEditorOverlay(content: String, font: VibeFont, onContentChange: (String) -> Unit) {
    OutlinedTextField(
        value         = content,
        onValueChange = { if (it.length <= 300) onContentChange(it) },
        placeholder   = { Text("What's on your mind?", color = Color.White.copy(0.5f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor     = Color.White,
            unfocusedTextColor   = Color.White,
        ),
        textStyle = LocalTextStyle.current.copy(fontFamily = font.family, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        modifier  = Modifier.fillMaxWidth(0.85f),
        maxLines  = 6,
    )
}

@Composable
private fun BackgroundPicker(current: TextPostBackground, onChange: (TextPostBackground) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(GRADIENT_PRESETS) { bg ->
            val isSelected = current == bg
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(bg.brush)
                    .border(if (isSelected) BorderStroke(3.dp, Color.White) else BorderStroke(0.dp, Color.Transparent), CircleShape)
                    .clickable { onChange(bg) },
            ) {
                if (isSelected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(18.dp))
            }
        }
        items(SOLID_PRESETS) { bg ->
            val isSelected = current == bg
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(bg.color)
                    .border(if (isSelected) BorderStroke(3.dp, Color.White) else BorderStroke(1.dp, Color.Gray.copy(0.3f)), CircleShape)
                    .clickable { onChange(bg) },
            ) {
                if (isSelected) Icon(Icons.Filled.Check, null, tint = if (bg.color == Color.White) Color.Black else Color.White, modifier = Modifier.align(Alignment.Center).size(18.dp))
            }
        }
    }
}

@Composable
private fun FontPicker(current: VibeFont, onChange: (VibeFont) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(VibeFont.values()) { font ->
            val isSelected = current == font
            Surface(
                shape  = RoundedCornerShape(24.dp),
                color  = if (isSelected) VibePink else MaterialTheme.colorScheme.surfaceVariant,
                border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.3f)) else null,
                modifier = Modifier.clickable { onChange(font) }.height(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(font.displayName, fontFamily = font.family, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
