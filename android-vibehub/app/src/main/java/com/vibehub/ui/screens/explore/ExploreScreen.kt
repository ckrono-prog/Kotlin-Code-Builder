package com.vibehub.ui.screens.explore

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.vibehub.ui.components.*
import com.vibehub.ui.theme.*

private val CATEGORIES = listOf("For You", "Trending", "Music", "Travel", "Food", "Fashion", "Sports", "Art", "Gaming", "Memes")
private val TRENDING_HASHTAGS = listOf("#GoodVibes", "#HappyDay", "#Grateful", "#Sunset", "#Travel", "#Coffee", "#VibeHub", "#Lifestyle")

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(onNavigateToProfile: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableIntStateOf(0) }
    var isSearchFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search people, posts, hashtags...") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
            )
        }

        // Category chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(CATEGORIES) { index, category ->
                FilterChip(
                    selected = selectedCategory == index,
                    onClick  = { selectedCategory = index },
                    label    = { Text(category) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VibePink,
                        selectedLabelColor     = Color.White,
                    ),
                    shape = RoundedCornerShape(50),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Content grid
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalItemSpacing = 6.dp,
        ) {
            // Trending hashtags section
            item(span = StaggeredGridItemSpan.FullLine) {
                Text("Trending", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            item(span = StaggeredGridItemSpan.FullLine) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(TRENDING_HASHTAGS) { tag ->
                        TrendingChip(tag = tag)
                    }
                }
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                Text("Discover", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }

            // Staggered grid of explore posts (placeholder tiles)
            items(20) { index ->
                ExplorePostTile(
                    imageUrl = "",
                    aspectRatio = if (index % 3 == 0) 1.5f else if (index % 3 == 1) 0.8f else 1.0f,
                    hasVideo = index % 4 == 0,
                    isReel   = index % 5 == 0,
                )
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun TrendingChip(tag: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(VibeGradient)
            .clickable {}
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(tag, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ExplorePostTile(
    imageUrl: String,
    aspectRatio: Float,
    hasVideo: Boolean,
    isReel: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {},
    ) {
        AsyncImage(
            model = imageUrl.ifBlank { null },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isReel) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(0.5f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(12.dp))
                Text("Reel", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        } else if (hasVideo) {
            Icon(Icons.Filled.PlayCircle, null, tint = Color.White.copy(0.8f),
                modifier = Modifier.align(Alignment.Center).size(32.dp))
        }
    }
}
