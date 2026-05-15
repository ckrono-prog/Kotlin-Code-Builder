package com.vibehub.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.navigation.*
import androidx.navigation.compose.*
import com.vibehub.domain.model.User
import com.vibehub.ui.screens.auth.LoginScreen
import com.vibehub.ui.screens.auth.RegisterScreen
import com.vibehub.ui.screens.auth.SplashScreen
import com.vibehub.ui.screens.call.CallScreen
import com.vibehub.ui.screens.call.CallState
import com.vibehub.ui.screens.call.CallType
import com.vibehub.ui.screens.comments.CommentsScreen
import com.vibehub.ui.screens.friends.FriendsScreen
import com.vibehub.ui.screens.home.MainShell
import com.vibehub.ui.screens.messages.EnhancedChatScreen
import com.vibehub.ui.screens.settings.ConversationSettingsScreen
import com.vibehub.ui.screens.settings.PrivacySettingsScreen
import com.vibehub.ui.screens.settings.ProfileSettingsScreen
import com.vibehub.ui.screens.settings.SettingsScreen
import com.vibehub.ui.screens.video.VideoCaptureScreen

sealed class Route(val path: String) {
    object Splash   : Route("splash")
    object Login    : Route("login")
    object Register : Route("register")
    object Main     : Route("main")

    object Chat : Route("chat/{conversationId}") {
        fun withId(id: String) = "chat/$id"
    }
    object Profile : Route("profile/{userId}") {
        fun withId(id: String) = "profile/$id"
    }
    object PostDetail : Route("post/{postId}") {
        fun withId(id: String) = "post/$id"
    }
    object Comments : Route("comments/{postId}") {
        fun withId(id: String) = "comments/$id"
    }
    object Friends : Route("friends/{userId}/{tab}") {
        fun withId(id: String, tab: Int = 0) = "friends/$id/$tab"
    }
    object VideoCapture : Route("video_capture")
    object AudioCall : Route("audio_call/{userId}") {
        fun withId(id: String) = "audio_call/$id"
    }
    object VideoCall : Route("video_call/{userId}") {
        fun withId(id: String) = "video_call/$id"
    }
    object Settings            : Route("settings")
    object ProfileSettings     : Route("profile_settings")
    object PrivacySettings     : Route("privacy_settings")
    object ConversationSettings : Route("conversation_settings/{conversationId}") {
        fun withId(id: String) = "conversation_settings/$id"
    }
}

@Composable
fun VibeHubNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = Route.Splash.path,
        enterTransition  = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) +
                    fadeIn(animationSpec = tween(300))
        },
        exitTransition   = {
            slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) +
                    fadeOut(animationSpec = tween(200))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) +
                    fadeIn(animationSpec = tween(300))
        },
        popExitTransition  = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                    fadeOut(animationSpec = tween(200))
        },
    ) {
        // ── Auth ─────────────────────────────────────────────────────────────
        composable(Route.Splash.path) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Route.Main.path) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
                },
            )
        }

        composable(Route.Login.path) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Route.Register.path) },
                onLoginSuccess = {
                    navController.navigate(Route.Main.path) {
                        popUpTo(Route.Login.path) { inclusive = true }
                    }
                },
            )
        }

        composable(Route.Register.path) {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Route.Main.path) {
                        popUpTo(Route.Register.path) { inclusive = true }
                    }
                },
            )
        }

        // ── Main shell (home, explore, reels, notifications, inbox tabs) ─────
        composable(Route.Main.path) {
            MainShell(
                onNavigateToProfile = { navController.navigate(Route.Profile.withId(it)) },
                onNavigateToChat    = { navController.navigate(Route.Chat.withId(it)) },
                onNavigateToSettings = { navController.navigate(Route.Settings.path) },
                onNavigateToVideoCapture = { navController.navigate(Route.VideoCapture.path) },
                onLogout = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(Route.Main.path) { inclusive = true }
                    }
                },
            )
        }

        // ── Chat ─────────────────────────────────────────────────────────────
        composable(
            route     = Route.Chat.path,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            EnhancedChatScreen(
                remoteUser          = User(),  // replace with ViewModel lookup
                currentUserId       = "",       // replace with AuthViewModel.currentUserId
                onBack              = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(Route.Profile.withId(it)) },
                onAudioCall         = { navController.navigate(Route.AudioCall.withId(conversationId)) },
                onVideoCall         = { navController.navigate(Route.VideoCall.withId(conversationId)) },
            )
        }

        // ── Profile ──────────────────────────────────────────────────────────
        composable(
            route     = Route.Profile.path,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
        ) {
            com.vibehub.ui.screens.profile.ProfileScreen(
                onBack = { navController.popBackStack() },
                onNavigateToChat = { navController.navigate(Route.Chat.withId(it)) },
                onNavigateToFollowers = { userId ->
                    navController.navigate(Route.Friends.withId(userId, 1))
                },
                onNavigateToFollowing = { userId ->
                    navController.navigate(Route.Friends.withId(userId, 2))
                },
            )
        }

        // ── Comments ─────────────────────────────────────────────────────────
        composable(
            route     = Route.Comments.path,
            arguments = listOf(navArgument("postId") { type = NavType.StringType }),
            enterTransition = {
                slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(tween(300))
            },
            exitTransition = {
                slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(tween(200))
            },
        ) { backStackEntry ->
            CommentsScreen(
                postId               = backStackEntry.arguments?.getString("postId") ?: "",
                onBack               = { navController.popBackStack() },
                onNavigateToProfile  = { navController.navigate(Route.Profile.withId(it)) },
            )
        }

        // ── Friends ──────────────────────────────────────────────────────────
        composable(
            route     = Route.Friends.path,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("tab")    { type = NavType.IntType; defaultValue = 0 },
            ),
        ) { backStackEntry ->
            FriendsScreen(
                userId               = backStackEntry.arguments?.getString("userId") ?: "",
                initialTab           = backStackEntry.arguments?.getInt("tab") ?: 0,
                onBack               = { navController.popBackStack() },
                onNavigateToProfile  = { navController.navigate(Route.Profile.withId(it)) },
            )
        }

        // ── Video Capture ─────────────────────────────────────────────────────
        composable(
            route            = Route.VideoCapture.path,
            enterTransition  = { fadeIn(tween(200)) },
            exitTransition   = { fadeOut(tween(200)) },
        ) {
            VideoCaptureScreen(
                onBack       = { navController.popBackStack() },
                onVideoSaved = { navController.popBackStack() },
            )
        }

        // ── Audio Call ───────────────────────────────────────────────────────
        composable(
            route     = Route.AudioCall.path,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            enterTransition = { fadeIn(tween(300)) },
            exitTransition  = { fadeOut(tween(300)) },
        ) {
            CallScreen(
                remoteUser = User(),
                callType   = CallType.AUDIO,
                callState  = CallState.RINGING,
                onEndCall  = { navController.popBackStack() },
            )
        }

        // ── Video Call ───────────────────────────────────────────────────────
        composable(
            route     = Route.VideoCall.path,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            enterTransition = { fadeIn(tween(300)) },
            exitTransition  = { fadeOut(tween(300)) },
        ) {
            CallScreen(
                remoteUser = User(),
                callType   = CallType.VIDEO,
                callState  = CallState.RINGING,
                onEndCall  = { navController.popBackStack() },
            )
        }

        // ── Settings ─────────────────────────────────────────────────────────
        composable(Route.Settings.path) {
            SettingsScreen(
                onBack                   = { navController.popBackStack() },
                onNavigateToPrivacy      = { navController.navigate(Route.PrivacySettings.path) },
                onNavigateToBlocked      = { },
                onNavigateToNotifications = { },
                onNavigateToSecurity     = { },
                onNavigateToAppearance   = { },
                onNavigateToHelp         = { },
                onLogout = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(Route.Main.path) { inclusive = true }
                    }
                },
            )
        }

        // ── Profile Settings ─────────────────────────────────────────────────
        composable(Route.ProfileSettings.path) {
            ProfileSettingsScreen(
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() },
            )
        }

        // ── Privacy Settings ─────────────────────────────────────────────────
        composable(Route.PrivacySettings.path) {
            PrivacySettingsScreen(onBack = { navController.popBackStack() })
        }

        // ── Conversation Settings ─────────────────────────────────────────────
        composable(
            route     = Route.ConversationSettings.path,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
        ) {
            ConversationSettingsScreen(
                remoteUser          = User(),
                onBack              = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(Route.Profile.withId(it)) },
                onBlock             = { navController.popBackStack() },
                onReport            = { },
                onDeleteConversation = { navController.popBackStack() },
                onArchive           = { navController.popBackStack() },
            )
        }
    }
}
