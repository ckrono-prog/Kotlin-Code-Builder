package com.vibehub.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.vibehub.domain.model.User
import com.vibehub.ui.theme.*

// ─────────────────────────────────────────────
// Gradient button (VibeHub's primary CTA style)
// ─────────────────────────────────────────────

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(if (enabled) VibeGradient else Brush.linearGradient(listOf(Color.Gray, Color.Gray)))
            .clickable(enabled = enabled && !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ─────────────────────────────────────────────
// Glassmorphism card
// ─────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VibeGlassLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    ) {
        Column(content = content)
    }
}

// ─────────────────────────────────────────────
// Avatar
// ─────────────────────────────────────────────

@Composable
fun VibeAvatar(
    imageUrl: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    hasStory: Boolean = false,
    isViewed: Boolean = false,
    isLive: Boolean = false,
) {
    Box(modifier = modifier.size(size + if (hasStory || isLive) 4.dp else 0.dp)) {
        Box(
            modifier = Modifier
                .size(size + if (hasStory || isLive) 4.dp else 0.dp)
                .align(Alignment.Center)
                .then(
                    if (hasStory && !isViewed)
                        Modifier.background(VibeGradient, CircleShape)
                    else if (isLive)
                        Modifier.background(Brush.linearGradient(listOf(VibePink, VibeNeonPurple)), CircleShape)
                    else Modifier
                )
        ) {
            AsyncImage(
                model = imageUrl.ifBlank { null },
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .align(Alignment.Center)
                    .padding(if (hasStory || isLive) 2.dp else 0.dp)
                    .clip(CircleShape)
                    .background(VibeCardDark),
            )
        }
        if (isLive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(VibePink, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text("LIVE", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
            }
        }
    }
}

// ─────────────────────────────────────────────
// Verified badge
// ─────────────────────────────────────────────

@Composable
fun VerifiedBadge(size: Dp = 16.dp) {
    Icon(
        imageVector = Icons.Filled.Verified,
        contentDescription = "Verified",
        tint = Color(0xFF1DA1F2),
        modifier = Modifier.size(size),
    )
}

// ─────────────────────────────────────────────
// Gradient text
// ─────────────────────────────────────────────

@Composable
fun GradientText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = style,
        modifier = modifier.graphicsLayer(alpha = 0.99f).drawWithCache {
            val brush = VibeGradient
            onDrawWithContent {
                drawContent()
                drawRect(brush = brush, blendMode = BlendMode.SrcAtop)
            }
        },
    )
}

// ─────────────────────────────────────────────
// Like button with animation
// ─────────────────────────────────────────────

@Composable
fun AnimatedLikeButton(
    isLiked: Boolean,
    likesCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isLiked) 1.25f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "like_scale",
    )
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = "Like",
            tint = if (isLiked) VibePink else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp).scale(scale),
        )
        Text(
            text = formatCount(likesCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

// ─────────────────────────────────────────────
// Stat chip (for profile stats)
// ─────────────────────────────────────────────

@Composable
fun StatChip(count: Int, label: String, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatCount(count),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

// ─────────────────────────────────────────────
// Outline button
// ─────────────────────────────────────────────

@Composable
fun OutlineVibeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, VibeGradient),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = VibePink),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

// ─────────────────────────────────────────────
// Loading shimmer placeholder
// ─────────────────────────────────────────────

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.Gray.copy(alpha = 0.2f),
            Color.Gray.copy(alpha = 0.4f),
            Color.Gray.copy(alpha = 0.2f),
        ),
        start = Offset(shimmerTranslate - 200f, 0f),
        end   = Offset(shimmerTranslate, 0f),
    )
    Box(modifier = modifier.background(shimmerBrush))
}

// ─────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────

fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000f)
    count >= 1_000     -> "%.1fK".format(count / 1_000f)
    else               -> count.toString()
}

fun formatLong(count: Long): String = when {
    count >= 1_000_000L -> "%.1fM".format(count / 1_000_000f)
    count >= 1_000L     -> "%.1fK".format(count / 1_000f)
    else                -> count.toString()
}
