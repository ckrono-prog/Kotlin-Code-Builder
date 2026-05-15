package com.vibehub.ui.screens.comments

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.vibehub.ui.theme.*

data class Sticker(val id: String, val emoji: String, val label: String)

/** Sticker pack categories. */
val STICKER_PACKS = listOf(
    "Reactions"  to listOf("❤️", "😂", "😍", "👏", "🔥", "💯", "😮", "😢", "😡", "🎉", "👍", "👎", "🙌", "💪", "🤣", "😎", "🥺", "😴", "🤔", "✨"),
    "Vibes"      to listOf("✌️", "🤙", "🫶", "💫", "🌟", "🎶", "🎵", "🎸", "🎤", "🎧", "📸", "🌈", "⚡", "💥", "🌊", "🍀", "🌺", "🦋", "🐝", "🌙"),
    "Food"       to listOf("🍕", "🍔", "🌮", "🍣", "🍜", "🍦", "🍩", "🧁", "🍰", "☕", "🍵", "🥤", "🍹", "🍺", "🥂", "🎂", "🍓", "🥑", "🍍", "🌽"),
    "Sports"     to listOf("⚽", "🏀", "🏈", "⚾", "🥊", "🏆", "🥇", "🎮", "🏋️", "🤸", "🧘", "🏄", "🚴", "🤿", "⛷️", "🏇", "🎯", "🎳", "🎻", "🎲"),
    "Travel"     to listOf("✈️", "🚀", "🌍", "🏝️", "🗺️", "⛵", "🚗", "🏕️", "🌄", "🌇", "🌃", "🎡", "🎠", "🏰", "🗼", "🌉", "🌁", "🏔️", "🌋", "🗽"),
)

/**
 * Emoji/sticker picker bottom sheet for comments and chat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPickerSheet(
    onStickerSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var selectedPack by remember { mutableIntStateOf(0) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text(
                "Stickers",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // Category tabs
            ScrollableTabRow(
                selectedTabIndex = selectedPack,
                edgePadding     = 12.dp,
                containerColor  = MaterialTheme.colorScheme.surface,
                contentColor    = VibePink,
                divider         = {},
            ) {
                STICKER_PACKS.forEachIndexed { idx, (name, _) ->
                    Tab(
                        selected = selectedPack == idx,
                        onClick  = { selectedPack = idx },
                        text     = { Text(name, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }

            // Sticker grid
            val (_, emojis) = STICKER_PACKS[selectedPack]
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.heightIn(max = 280.dp),
            ) {
                items(emojis, key = { it }) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onStickerSelected(emoji); onDismiss() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(emoji, fontSize = 28.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun StickerBubble(sticker: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(sticker, fontSize = 36.sp, textAlign = TextAlign.Center)
    }
}
