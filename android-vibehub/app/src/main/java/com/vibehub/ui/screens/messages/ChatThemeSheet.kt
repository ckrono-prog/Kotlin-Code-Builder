package com.vibehub.ui.screens.messages

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.vibehub.ui.theme.*

/** Chat theme definition — static (solid/gradient) or animated. */
sealed class ChatTheme(
    open val id: String,
    open val displayName: String,
    open val emoji: String,
) {
    data class Static(
        override val id: String,
        override val displayName: String,
        override val emoji: String,
        val brush: Brush,
        val bubbleSentColor: Color,
        val bubbleReceivedColor: Color,
    ) : ChatTheme(id, displayName, emoji)

    data class Animated(
        override val id: String,
        override val displayName: String,
        override val emoji: String,
        val colorStops: List<Color>,
        val bubbleSentColor: Color,
        val bubbleReceivedColor: Color,
    ) : ChatTheme(id, displayName, emoji)
}

val CHAT_THEMES: List<ChatTheme> = listOf(
    ChatTheme.Static(
        id = "default", displayName = "Default", emoji = "💬",
        brush = Brush.linearGradient(listOf(Color(0xFF0D0D0D), Color(0xFF1A1A1A))),
        bubbleSentColor = VibePink, bubbleReceivedColor = Color(0xFF252525),
    ),
    ChatTheme.Animated(
        id = "aurora", displayName = "Aurora", emoji = "🌌",
        colorStops = listOf(Color(0xFF0D0D3B), Color(0xFF1B0533), Color(0xFF0A2A1A)),
        bubbleSentColor = Color(0xFF7F00FF), bubbleReceivedColor = Color(0xFF1A1A3B),
    ),
    ChatTheme.Animated(
        id = "sunset", displayName = "Sunset", emoji = "🌅",
        colorStops = listOf(Color(0xFFFC466B), Color(0xFFFF8C00), Color(0xFFFFD200)),
        bubbleSentColor = Color(0xFFFC466B), bubbleReceivedColor = Color(0xFF2A1A1A),
    ),
    ChatTheme.Static(
        id = "ocean", displayName = "Ocean", emoji = "🌊",
        brush = Brush.verticalGradient(listOf(Color(0xFF0D1B4B), Color(0xFF0B3D6B), Color(0xFF0D2B4B))),
        bubbleSentColor = Color(0xFF0575E6), bubbleReceivedColor = Color(0xFF1A2A3B),
    ),
    ChatTheme.Static(
        id = "forest", displayName = "Forest", emoji = "🌲",
        brush = Brush.verticalGradient(listOf(Color(0xFF0A1F0A), Color(0xFF1B3A1B))),
        bubbleSentColor = Color(0xFF2E7D32), bubbleReceivedColor = Color(0xFF1B2A1B),
    ),
    ChatTheme.Animated(
        id = "galaxy", displayName = "Galaxy", emoji = "🪐",
        colorStops = listOf(Color(0xFF0A0020), Color(0xFF1B003B), Color(0xFF000A20)),
        bubbleSentColor = Color(0xFFB94FFF), bubbleReceivedColor = Color(0xFF1A0A2A),
    ),
    ChatTheme.Static(
        id = "rose", displayName = "Rose Gold", emoji = "🌸",
        brush = Brush.linearGradient(listOf(Color(0xFF4A1420), Color(0xFF2A0A1A))),
        bubbleSentColor = Color(0xFFE8356D), bubbleReceivedColor = Color(0xFF2A1020),
    ),
    ChatTheme.Animated(
        id = "neon", displayName = "Neon City", emoji = "🌆",
        colorStops = listOf(Color(0xFF0A0A0A), Color(0xFF0D001A), Color(0xFF000A0A)),
        bubbleSentColor = Color(0xFFFF2D78), bubbleReceivedColor = Color(0xFF1A001A),
    ),
    ChatTheme.Static(
        id = "minimal", displayName = "Minimal", emoji = "⬜",
        brush = Brush.linearGradient(listOf(Color(0xFFF5F5F5), Color(0xFFEEEEEE))),
        bubbleSentColor = Color(0xFFE8356D), bubbleReceivedColor = Color(0xFFE0E0E0),
    ),
    ChatTheme.Animated(
        id = "candy", displayName = "Candy", emoji = "🍬",
        colorStops = listOf(Color(0xFF2B0030), Color(0xFF00151A), Color(0xFF1A0030)),
        bubbleSentColor = Color(0xFFFF6B9D), bubbleReceivedColor = Color(0xFF2A0030),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThemeSheet(
    currentThemeId: String,
    onSelectTheme: (ChatTheme) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Chat Theme", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onDismiss) { Text("Done", color = VibePink, fontWeight = FontWeight.Bold) }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 480.dp),
            ) {
                items(CHAT_THEMES, key = { it.id }) { theme ->
                    ThemeCard(theme = theme, isSelected = theme.id == currentThemeId, onClick = { onSelectTheme(theme) })
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(theme: ChatTheme, isSelected: Boolean, onClick: () -> Unit) {
    val bgModifier: Modifier = when (theme) {
        is ChatTheme.Static   -> Modifier.background(theme.brush)
        is ChatTheme.Animated -> Modifier.background(animatedBrush(theme.colorStops))
    }

    Box(
        modifier = Modifier
            .height(100.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(bgModifier)
            .border(BorderStroke(if (isSelected) 2.5.dp else 0.dp, VibePink), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        // Bubble previews
        Column(
            modifier = Modifier.align(Alignment.Center).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(Modifier.align(Alignment.End).height(14.dp).width(60.dp).clip(RoundedCornerShape(7.dp)).background(
                when (theme) {
                    is ChatTheme.Static   -> theme.bubbleSentColor
                    is ChatTheme.Animated -> theme.bubbleSentColor
                }
            ))
            Box(Modifier.height(14.dp).width(48.dp).clip(RoundedCornerShape(7.dp)).background(
                when (theme) {
                    is ChatTheme.Static   -> theme.bubbleReceivedColor
                    is ChatTheme.Animated -> theme.bubbleReceivedColor
                }
            ))
        }
        // Label
        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(theme.emoji, style = MaterialTheme.typography.labelSmall)
            Text(theme.displayName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
        }
        if (isSelected) {
            Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp).clip(CircleShape).background(VibePink), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
        if (theme is ChatTheme.Animated) {
            Box(Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(0.15f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                Text("✨ Live", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun animatedBrush(colors: List<Color>): Brush {
    val t = rememberInfiniteTransition(label = "chat_theme_anim")
    val offset by t.animateFloat(
        initialValue = 0f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "offset",
    )
    return Brush.linearGradient(colors, start = Offset(0f, offset), end = Offset(offset, 0f))
}
