package com.vibehub.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.navigation.*
import androidx.navigation.compose.*
import com.vibehub.domain.model.User
import com.vibehub.ui.screens.auth.LoginScreen
import com.vibehub.ui.screens.auth.OtpVerifyScreen
import com.vibehub.ui.screens.auth.ProfileSetupScreen
import com.vibehub.ui.screens.auth.SignUpScreen
import com.vibehub.ui.screens.auth.SplashScreen
import com.vibehub.ui.screens.call.CallScreen
import com.vibehub.ui.screens.call.CallState
import com.vibehub.ui.screens.call.CallType
import com.vibehub.ui.screens.comments.CommentsScreen
import com.vibehub.ui.screens.friends.FriendsScreen
import com.vibehub.ui.screens.home.MainShell
import com.vibehub.ui.screens.home.PostCreationScreen
import com.vibehub.ui.screens.messages.EnhancedChatScreen
import com.vibehub.ui.screens.settings.BlockedUsersScreen
import com.vibehub.ui.screens.settings.ConversationSettingsScreen
import com.vibehub.ui.screens.settings.DownloadSettingsScreen
import com.vibehub.ui.screens.settings.PrivacySettingsScreen
import com.vibehub.ui.screens.settings.ProfileSettingsScreen
import com.vibehub.ui.screens.settings.SettingsScreen
import com.vibehub.ui.screens.video.VideoCaptureScreen

sealed class Route(val path: String) {
    object Splash          : Route("splash")
    object Login           : Route("login")
    object Register        : Route("register")
    object OtpVerify       : Route("otp_verify/{email}") { fun withEmail(email: String) = "otp_verify/${email.encodeUrl()}" }
    object ProfileSetup    : Route("profile_setup")
    object Main            : Route("main")

    object Chat            : Route("chat/{conversationId}") { fun withId(id: String) = "chat/$id" }
    object Profile         : Route("profile/{userId}")      { fun withId(id: String) = "profile/$id" }
    object PostDetail      : Route("post/{postId}")         { fun withId(id: String) = "post/$id" }
    object Comments        : Route("comments/{postId}")     { fun withId(id: String) = "comments/$id" }
    object Friends         : Route("friends/{userId}/{tab}") { fun withId(id: String, tab: Int = 0) = "friends/$id/$tab" }
    object PostCreation    : Route("post_creation")
    object VideoCapture    : Route("video_capture")

    object AudioCall       : Route("audio_call/{userId}") { fun withId(id: String) = "audio_call/$id" }
    object VideoCall       : Route("video_call/{userId}") { fun withId(id: String) = "video_call/$id" }

    object Settings            : Route("settings")
    object ProfileSettings     : Route("profile_settings")
    object PrivacySettings     : Route("privacy_settings")
    object DownloadSettings    : Route("download_settings")
    object BlockedUsers        : Route("blocked_users")
    object ConversationSettings : Route("conversation_settings/{conversationId}") {
        fun withId(id: String) = "conversation_settings/$id"
    }
}

private fun String.encodeUrl() = java.net.URLEncoder.encode(this, "UTF-8")
private fun String.decodeUrl() = java.net.URLDecoder.decode(this, "UTF-8")

@Composable
fun VibeHubNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = Route.Splash.path,
        enterTransition  = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(tween(300)) },
        exitTransition   = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut(tween(200)) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn(tween(300)) },
        popExitTransition  = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(tween(200)) },
    ) {
        // ── Splash ────────────────────────────────────────────────────────────
        composable(Route.Splash.path) {
            SplashScreen(
                onNavigateToLogin = { navController.navigate(Route.Login.path) { popUpTo(Route.Splash.path) { inclusive = true } } },
                onNavigateToMain  = { navController.navigate(Route.Main.path)  { popUpTo(Route.Splash.path) { inclusive = true } } },
            )
        }

        // ── Login ─────────────────────────────────────────────────────────────
        composable(Route.Login.path) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Route.Register.path) },
                onLoginSuccess = { navController.navigate(Route.Main.path) { popUpTo(Route.Login.path) { inclusive = true } } },
            )
        }

        // ── Register (Sign Up) ────────────────────────────────────────────────
        composable(Route.Register.path) {
            SignUpScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onSignUpSuccess   = { email ->
                    navController.navigate(Route.OtpVerify.withEmail(email)) { popUpTo(Route.Register.path) { inclusive = true } }
                },
            )
        }

        // ── OTP Verify ────────────────────────────────────────────────────────
        composable(
            route     = Route.OtpVerify.path,
            arguments = listOf(navArgument("email") { type = NavType.StringType }),
        ) { back ->
            val email = back.arguments?.getString("email")?.decodeUrl() ?: ""
            OtpVerifyScreen(
                email      = email,
                onVerified = { navController.navigate(Route.ProfileSetup.path) { popUpTo(Route.OtpVerify.path) { inclusive = true } } },
                onBack     = { navController.popBackStack() },
            )
        }

        // ── Profile Setup ─────────────────────────────────────────────────────
        composable(Route.ProfileSetup.path) {
            ProfileSetupScreen(
                onComplete = { navController.navigate(Route.Main.path) { popUpTo(Route.ProfileSetup.path) { inclusive = true } } },
            )
        }

        // ── Main shell ────────────────────────────────────────────────────────
        composable(Route.Main.path) {
            MainShell(
                onNavigateToProfile     = { navController.navigate(Route.Profile.withId(it)) },
                onNavigateToChat        = { navController.navigate(Route.Chat.withId(it)) },
                onNavigateToComments    = { navController.navigate(Route.Comments.withId(it)) },
                onNavigateToCreate      = { navController.navigate(Route.PostCreation.path) },
                onNavigateToSettings    = { navController.navigate(Route.Settings.path) },
                onNavigateToVideoCapture = { navController.navigate(Route.VideoCapture.path) },
                onLogout = { navController.navigate(Route.Login.path) { popUpTo(Route.Main.path) { inclusive = true } } },
            )
        }

        // ── Post Creation ─────────────────────────────────────────────────────
        composable(
            route           = Route.PostCreation.path,
            enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(350)) + fadeIn(tween(350)) },
            exitTransition  = { slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(tween(250)) },
        ) {
            PostCreationScreen(
                onBack    = { navController.popBackStack() },
                onPosted  = { navController.popBackStack() },
            )
        }

        // ── Chat ──────────────────────────────────────────────────────────────
        composable(
            route     = Route.Chat.path,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
        ) { back ->
            val conversationId = back.arguments?.getString("conversationId") ?: ""
            EnhancedChatScreen(
                remoteUser          = User(),
                currentUserId       = "",
                onBack              = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(Route.Profile.withId(it)) },
                onAudioCall         = { navController.navigate(Route.AudioCall.withId(conversationId)) },
                onVideoCall         = { navController.navigate(Route.VideoCall.withId(conversationId)) },
            )
        }

        // ── Profile ───────────────────────────────────────────────────────────
        composable(
            route     = Route.Profile.path,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
        ) {
            com.vibehub.ui.screens.profile.ProfileScreen(
                onBack              = { navController.popBackStack() },
                onNavigateToChat    = { navController.navigate(Route.Chat.withId(it)) },
                onNavigateToFollowers = { userId -> navController.navigate(Route.Friends.withId(userId, 1)) },
                onNavigateToFollowing = { userId -> navController.navigate(Route.Friends.withId(userId, 2)) },
            )
        }

        // ── Comments ──────────────────────────────────────────────────────────
        composable(
            route           = Route.Comments.path,
            arguments       = listOf(navArgument("postId") { type = NavType.StringType }),
            enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(tween(300)) },
            exitTransition  = { slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(tween(200)) },
        ) { back ->
            CommentsScreen(
                postId              = back.arguments?.getString("postId") ?: "",
                onBack              = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(Route.Profile.withId(it)) },
            )
        }

        // ── Friends ───────────────────────────────────────────────────────────
        composable(
            route     = Route.Friends.path,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("tab")    { type = NavType.IntType; defaultValue = 0 },
            ),
        ) { back ->
            FriendsScreen(
                userId              = back.arguments?.getString("userId") ?: "",
                initialTab          = back.arguments?.getInt("tab") ?: 0,
                onBack              = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(Route.Profile.withId(it)) },
            )
        }

        // ── Video Capture ─────────────────────────────────────────────────────
        composable(
            route           = Route.VideoCapture.path,
            enterTransition = { fadeIn(tween(200)) },
            exitTransition  = { fadeOut(tween(200)) },
        ) {
            VideoCaptureScreen(
                onBack       = { navController.popBackStack() },
                onVideoSaved = { navController.popBackStack() },
            )
        }

        // ── Calls ─────────────────────────────────────────────────────────────
        composable(
            route     = Route.AudioCall.path,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            enterTransition = { fadeIn(tween(300)) }, exitTransition = { fadeOut(tween(300)) },
        ) {
            CallScreen(remoteUser = User(), callType = CallType.AUDIO, callState = CallState.RINGING, onEndCall = { navController.popBackStack() })
        }
        composable(
            route     = Route.VideoCall.path,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            enterTransition = { fadeIn(tween(300)) }, exitTransition = { fadeOut(tween(300)) },
        ) {
            CallScreen(remoteUser = User(), callType = CallType.VIDEO, callState = CallState.RINGING, onEndCall = { navController.popBackStack() })
        }

        // ── Settings ──────────────────────────────────────────────────────────
        composable(Route.Settings.path) {
            SettingsScreen(
                onBack                    = { navController.popBackStack() },
                onNavigateToPrivacy       = { navController.navigate(Route.PrivacySettings.path) },
                onNavigateToBlocked       = { navController.navigate(Route.BlockedUsers.path) },
                onNavigateToDownloads     = { navController.navigate(Route.DownloadSettings.path) },
                onNavigateToNotifications = {},
                onNavigateToSecurity      = {},
                onNavigateToAppearance    = {},
                onNavigateToHelp          = {},
                onLogout = { navController.navigate(Route.Login.path) { popUpTo(Route.Main.path) { inclusive = true } } },
            )
        }

        composable(Route.ProfileSettings.path) {
            ProfileSettingsScreen(onBack = { navController.popBackStack() }, onSave = { navController.popBackStack() })
        }

        composable(Route.PrivacySettings.path) {
            PrivacySettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.DownloadSettings.path) {
            DownloadSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.BlockedUsers.path) {
            BlockedUsersScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route     = Route.ConversationSettings.path,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
        ) {
            ConversationSettingsScreen(
                remoteUser          = User(),
                onBack              = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(Route.Profile.withId(it)) },
                onBlock             = { navController.popBackStack() },
                onReport            = {},
                onDeleteConversation = { navController.popBackStack() },
                onArchive           = { navController.popBackStack() },
            )
        }
    }
}
