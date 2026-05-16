package com.vibehub.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibehub.ui.screens.explore.ExploreScreen
import com.vibehub.ui.screens.messages.InboxScreen
import com.vibehub.ui.screens.notifications.NotificationsScreen
import com.vibehub.ui.screens.profile.ProfileScreen
import com.vibehub.ui.screens.reels.ReelsScreen
import com.vibehub.ui.theme.*
import com.vibehub.ui.viewmodel.NotificationViewModel

sealed class BottomTab(
    val index: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    object Home    : BottomTab(0, "Home",    Icons.Filled.Home,          Icons.Outlined.Home)
    object Friends : BottomTab(1, "Friends", Icons.Filled.People,        Icons.Outlined.People)
    object Create  : BottomTab(2, "Create",  Icons.Filled.AddCircle,     Icons.Outlined.AddCircle)
    object Notif   : BottomTab(3, "Alerts",  Icons.Filled.Notifications, Icons.Outlined.Notifications)
    object Menu    : BottomTab(4, "Menu",    Icons.Filled.Menu,          Icons.Outlined.Menu)
}

private val TABS = listOf(BottomTab.Home, BottomTab.Friends, BottomTab.Create, BottomTab.Notif, BottomTab.Menu)

@Composable
fun MainShell(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToComments: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToVideoCapture: () -> Unit,
    onLogout: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val notifViewModel: NotificationViewModel = hiltViewModel()
    val notifState by notifViewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            VibeBottomNav(
                selectedIndex    = selectedTab,
                onTabSelected    = { idx ->
                    if (idx == 2) onNavigateToCreate() else selectedTab = idx
                },
                unreadNotifCount = notifState.unreadCount,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> HomeFeedScreen(
                    onNavigateToProfile  = onNavigateToProfile,
                    onNavigateToReels    = { selectedTab = 1 },
                    onNavigateToComments = onNavigateToComments,
                    onNavigateToCreate   = onNavigateToCreate,
                    onNavigateToChat     = onNavigateToChat,
                )
                1 -> ExploreScreen(onNavigateToProfile = onNavigateToProfile)
                3 -> NotificationsScreen(onNavigateToProfile = onNavigateToProfile)
                4 -> MenuScreen(
                    onNavigateToProfile  = onNavigateToProfile,
                    onNavigateToSettings = onNavigateToSettings,
                    onLogout             = onLogout,
                )
            }
        }
    }
}

// ─── Bottom Navigation ────────────────────────────────────────────────────────

@Composable
private fun VibeBottomNav(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    unreadNotifCount: Int,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        TABS.forEach { tab ->
            val selected = selectedIndex == tab.index
            NavigationBarItem(
                selected = selected,
                onClick  = { onTabSelected(tab.index) },
                icon = {
                    if (tab == BottomTab.Create) {
                        Box(
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(50)).background(VibeGradient),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Add, "Create", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    } else {
                        BadgedBox(badge = {
                            if (tab == BottomTab.Notif && unreadNotifCount > 0) {
                                Badge { Text(unreadNotifCount.coerceAtMost(99).toString()) }
                            }
                        }) {
                            Icon(if (selected) tab.selectedIcon else tab.unselectedIcon, tab.label)
                        }
                    }
                },
                label = { if (tab != BottomTab.Create) Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = VibePink,
                    selectedTextColor   = VibePink,
                    indicatorColor      = VibePink.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                ),
            )
        }
    }
}

// ─── Menu / Profile tab ───────────────────────────────────────────────────────

@Composable
private fun MenuScreen(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    ProfileScreen(
        onBack              = {},
        onNavigateToChat    = {},
        onNavigateToFollowers = {},
        onNavigateToFollowing = {},
    )
}
