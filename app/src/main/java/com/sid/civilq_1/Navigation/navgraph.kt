package com.sid.civilq_1.Navigation

import androidx.compose.animation.*

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*

import androidx.compose.runtime.*

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.auth.FirebaseAuth
import com.sid.civilq_1.screens.ChatScreen
import com.sid.civilq_1.screens.ProfileScreen
import com.sid.civilq_1.screens.ReportDetailScreen
import com.sid.civilq_1.screens.LoginScreen
import com.sid.civilq_1.screens.SignUpScreen
import com.sid.civilq_1.ui.screens.HomeScreen
import com.sid.civilq_1.screens.ReportScreen
import com.sid.civilq_1.viewmodel.ReportViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val reportViewModel: ReportViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // ✅ ULTIMATE PERFORMANCE: Preload ChatScreen composition in background
    var shouldPreloadChat by remember { mutableStateOf(false) }

    // ✅ Trigger preloading when user is likely to navigate to chat
    LaunchedEffect(currentRoute) {
        if (currentRoute == "home") {
            // Preload after a short delay when on home screen
            kotlinx.coroutines.delay(1000)
            shouldPreloadChat = true
        }
    }

    // ✅ Invisible preloaded ChatScreen for instant navigation
    if (shouldPreloadChat && currentRoute != "chat") {
        Box(modifier = Modifier.size(0.dp)) {
            ChatScreen(navController = navController)
        }
    }

    // Define routes with bottom navigation bar
    val routesWithBottomBar = setOf("home", "report", "profile")
    val isReportDetails = currentRoute?.startsWith("reportdetails/") == true
    val shouldShowBottomBar = currentRoute in routesWithBottomBar || isReportDetails

    // Define authentication routes (no bottom bar needed)
    val authRoutes = setOf("login", "signup")
    val isAuthRoute = currentRoute in authRoutes

    Box(modifier = Modifier.fillMaxSize()) {
        val navController = rememberNavController()
        val auth = FirebaseAuth.getInstance()
        val startDestination = if (auth.currentUser != null) {
            "home"
        } else {
            "login"
        }
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),

            // ✅ Enter animations
            enterTransition = {
                when (targetState.destination.route) {
                    "login", "signup" -> {
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        ) + slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        )
                    }

                    else -> {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        )
                    }
                }
            },

            // ✅ Exit animations
            exitTransition = {
                when (initialState.destination.route) {
                    "login", "signup" -> {
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        ) + slideOutVertically(
                            targetOffsetY = { -it / 4 },
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        )
                    }

                    else -> {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        )
                    }
                }
            },

            // ✅ Pop enter animations
            popEnterTransition = {
                when (targetState.destination.route) {
                    "login", "signup" -> {
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        ) + slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        )
                    }

                    else -> {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        )
                    }
                }
            },

            // ✅ Pop exit animations
            popExitTransition = {
                when (initialState.destination.route) {
                    "login", "signup" -> {
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        ) + slideOutVertically(
                            targetOffsetY = { it / 4 },
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        )
                    }

                    else -> {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        )
                    }
                }
            }
        ) {
            // ✅ Authentication Screens
            composable("login") { LoginScreen(navController) }
            composable("signup") { SignUpScreen(navController) }

            // ✅ Main App Screens
            composable("home") { HomeScreen(navController, reportViewModel) }
            composable("report") { ReportScreen(navController, reportViewModel) }
            composable("profile") { ProfileScreen(navController) }
            composable("chat") { ChatScreen(navController) }

            composable(
                route = "reportdetails/{reportId}",
                arguments = listOf(navArgument("reportId") { type = NavType.IntType })
            ) { backStackEntry ->
                val reportId = backStackEntry.arguments?.getInt("reportId") ?: 0
                ReportDetailScreen(reportId, reportViewModel)
            }

        }
    }

    // ✅ Extension function for easier logout navigation
    fun NavHostController.navigateToAuth() {
        navigate("login") {
            popUpTo(0) { inclusive = true }
        }
    }
}