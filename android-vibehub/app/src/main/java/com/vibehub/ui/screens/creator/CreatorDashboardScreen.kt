package com.vibehub.ui.screens.creator

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.vibehub.ui.components.*
import com.vibehub.ui.theme.*

@Composable
fun CreatorDashboardScreen(onBack: () -> Unit) {
    var selectedPeriod by remember { mutableIntStateOf(1) }
    val periods = listOf("7D", "30D", "90D", "1Y")

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        // Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(VibeGradientVertical),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                        }
                        Text("Creator Studio", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        CreatorHeaderStat("128", "Posts", Icons.Outlined.GridOn)
                        CreatorHeaderStat("3.2K", "Followers", Icons.Outlined.People)
                        CreatorHeaderStat("512", "Following", Icons.Outlined.PersonAdd)
                        CreatorHeaderStat("$1.2K", "Earnings", Icons.Outlined.AttachMoney)
                    }
                }
            }
        }

        // Period selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Analytics Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    periods.forEachIndexed { index, period ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selectedPeriod == index) VibeGradient
                                else androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(Color.Transparent, Color.Transparent)))
                                .clickable { selectedPeriod = index }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(period, style = MaterialTheme.typography.labelMedium,
                                color = if (selectedPeriod == index) Color.White
                                else MaterialTheme.colorScheme.onSurface.copy(0.6f),
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Metric cards row 1
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard("Total Views", "124.5K", "+18.3%", Icons.Filled.Visibility,
                    isPositive = true, modifier = Modifier.weight(1f))
                MetricCard("Reach", "89.2K", "+12.7%", Icons.Filled.TravelExplore,
                    isPositive = true, modifier = Modifier.weight(1f))
            }
        }
        item { Spacer(Modifier.height(12.dp)) }

        // Metric cards row 2
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard("Engagement", "8.4%", "+2.1%", Icons.Filled.Favorite,
                    isPositive = true, modifier = Modifier.weight(1f))
                MetricCard("New Followers", "+248", "-3.2%", Icons.Filled.PersonAdd,
                    isPositive = false, modifier = Modifier.weight(1f))
            }
        }
        item { Spacer(Modifier.height(12.dp)) }

        // Chart placeholder
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Views Over Time", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    // Simple animated bar chart
                    SimpleBarChart()
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        // Audience insights
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Audience Insights", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))

                    listOf(
                        Triple("18-24", 0.35f, VibePink),
                        Triple("25-34", 0.28f, VibeOrange),
                        Triple("35-44", 0.20f, VibeGold),
                        Triple("45+",   0.17f, VibeNeonPurple),
                    ).forEach { (label, fraction, color) ->
                        AgeBarRow(label = label, fraction = fraction, color = color)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        // Earnings overview
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Earnings Overview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))

                    listOf(
                        Triple("Subscriptions", "$640.00", Icons.Filled.Star),
                        Triple("Tips & Gifts",  "$320.00", Icons.Filled.CardGiftcard),
                        Triple("Marketplace",   "$240.00", Icons.Filled.Store),
                        Triple("Live Events",   "$100.00", Icons.Filled.LiveTv),
                    ).forEach { (source, amount, icon) ->
                        EarningsRow(source = source, amount = amount, icon = icon)
                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        GradientText("$1,300.00", MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black))
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatorHeaderStat(value: String, label: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    change: String,
    icon: ImageVector,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(VibePink.copy(0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = VibePink, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(
                text  = change,
                style = MaterialTheme.typography.labelSmall,
                color = if (isPositive) VibeSuccess else VibeError,
            )
        }
    }
}

@Composable
private fun SimpleBarChart() {
    val bars = listOf(0.4f, 0.6f, 0.5f, 0.8f, 0.7f, 1.0f, 0.9f)
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEachIndexed { index, fraction ->
            val animatedFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(600, delayMillis = index * 80),
                label = "bar_$index",
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height((animatedFraction * 100).dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(VibeGradient),
                )
                Spacer(Modifier.height(4.dp))
                Text(days[index], style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
            }
        }
    }
}

@Composable
private fun AgeBarRow(label: String, fraction: Float, color: Color) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(800),
        label = "age_$label",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("${(fraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(0.5f), modifier = Modifier.width(32.dp))
    }
}

@Composable
private fun EarningsRow(source: String, amount: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = VibePink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(source, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(amount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
