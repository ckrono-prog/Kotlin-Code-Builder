package com.vibehub.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.navigation.*
import androidx.navigation.compose.*
import com.vibehub.ui.screens.auth.LoginScreen
import com.vibehub.ui.screens.auth.RegisterScreen
import com.vibehub.ui.screens.auth.SplashScreen
import com.vibehub.ui.screens.home.MainShell

sealed class Route(val path: String) {
    object Splash   : Route("splash")
    object Login    : Route("login")
    object Register : Route("register")
    object Main     : Route("main")
    object Chat     : Route("chat/{conversationId}") {
        fun withId(id: String) = "chat/$id"
    }
    object Profile  : Route("profile/{userId}") {
        fun withId(id: String) = "profile/$id"
    }
    object PostDetail : Route("post/{postId}") {
        fun withId(id: String) = "post/$id"
    }
}

@Composable
fun VibeHubNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Splash.path,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(350)) +
                    fadeIn(animationSpec = tween(350))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(350)) +
                    fadeOut(animationSpec = tween(200))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(350)) +
                    fadeIn(animationSpec = tween(350))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(350)) +
                    fadeOut(animationSpec = tween(200))
        },
    ) {
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

        composable(Route.Main.path) {
            MainShell(
                onNavigateToProfile = { userId ->
                    navController.navigate(Route.Profile.withId(userId))
                },
                onNavigateToChat = { conversationId ->
                    navController.navigate(Route.Chat.withId(conversationId))
                },
                onLogout = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(Route.Main.path) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Route.Chat.path,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
        ) {
            com.vibehub.ui.screens.messages.ChatScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Route.Profile.path,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
        ) {
            com.vibehub.ui.screens.profile.ProfileScreen(
                onBack = { navController.popBackStack() },
                onNavigateToChat = { conversationId ->
                    navController.navigate(Route.Chat.withId(conversationId))
                },
            )
        }
    }
}
