package com.sid.civilq_1.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.auth.FirebaseAuth
import com.sid.civilq_1.screens.ChatScreen
import com.sid.civilq_1.screens.LoginScreen
import com.sid.civilq_1.screens.ProfileScreen
import com.sid.civilq_1.screens.ReportDetailScreen
import com.sid.civilq_1.screens.SignUpScreen
import com.sid.civilq_1.ui.screens.HomeScreen
import com.sid.civilq_1.screens.ReportScreen
import com.sid.civilq_1.viewmodel.ReportViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(navController: NavHostController) {
    // ✅ Stable ViewModel
    val reportViewModel: ReportViewModel = viewModel()

    // ✅ Current route for bottom bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // ✅ Firebase current user
    val currentUser = FirebaseAuth.getInstance().currentUser

    // ✅ Dynamic start destination
    val startDestination = if (currentUser != null) "home" else "login"

    // ✅ Show bottom bar on specific routes
    val showBottomBar = currentRoute in listOf("home", "report", "profile")
    val isReportDetails = currentRoute?.startsWith("reportdetails/") == true
    val shouldShowBottomBar = showBottomBar || isReportDetails

    Box(modifier = Modifier.fillMaxSize()) {
        // ✅ Animated NavHost
        NavHost(
            navController = navController,
            startDestination = startDestination, // 👈 Dynamic login/home start
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                when (targetState.destination.route) {
                    "chat" -> slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                    else -> slideInHorizontally(
                        initialOffsetX = { it / 2 },
                        animationSpec = tween(250)
                    ) + fadeIn(animationSpec = tween(250))
                }
            },
            exitTransition = {
                when (targetState.destination.route) {
                    "chat" -> slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                    else -> slideOutHorizontally(
                        targetOffsetX = { -it / 2 },
                        animationSpec = tween(250)
                    ) + fadeOut(animationSpec = tween(250))
                }
            }
        ) {
            composable("signup") {
                SignUpScreen(navController)
            }
            composable("login") {
                LoginScreen(navController)
            }
            composable("home") {
                HomeScreen(navController, reportViewModel)
            }
            composable("report") {
                ReportScreen(navController, reportViewModel)
            }
            composable("profile") {
                ProfileScreen(navController)
            }
            composable("chat") {
                ChatScreen(navController = navController)
            }
            composable(
                route = "reportdetails/{reportId}",
                arguments = listOf(navArgument("reportId") { type = NavType.IntType })
            ) { backStackEntry ->
                val reportId = backStackEntry.arguments?.getInt("reportId") ?: 0
                ReportDetailScreen(reportId = reportId, reportViewModel = reportViewModel)
            }
        }

        // ✅ Animated Bottom Navigation Bar
        AnimatedVisibility(
            visible = shouldShowBottomBar,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(250)
            ) + fadeIn(animationSpec = tween(250)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(250)
            ) + fadeOut(animationSpec = tween(250)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                BottomNavBar(navController)
            }
        }
    }
}
