package com.sid.civilq_1.Navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.auth.FirebaseAuth
import com.sid.civilq_1.screens.*
import com.sid.civilq_1.ui.screens.HomeScreen
import com.sid.civilq_1.viewmodel.ReportViewModel

@Composable
fun AppNavigation() {
    // Only define one NavController for the whole app
    val navController = rememberNavController()
    val reportViewModel: ReportViewModel = viewModel()

    val auth = remember { FirebaseAuth.getInstance() }
    val startDestination = remember {
        if (auth.currentUser != null) "home" else "login"
    }

    // Optimization: Clean transitions to reduce GPU pressure
    val standardTween = tween<IntOffset>(durationMillis = 300, easing = FastOutSlowInEasing)

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = standardTween)
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -it }, animationSpec = standardTween)
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = standardTween)
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = standardTween)
            }
        ) {
            // Authentication
            composable("login") { LoginScreen(navController) }
            composable("signup") { SignUpScreen(navController) }

            // Main App
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
                ChatScreen(navController)
            }

            composable(
                route = "reportdetails/{reportId}",
                arguments = listOf(navArgument("reportId") { type = NavType.IntType })
            ) { backStackEntry ->
                val reportId = backStackEntry.arguments?.getInt("reportId") ?: 0
                ReportDetailScreen(reportId, reportViewModel)
            }
        }
    }
}

/**
 * Clean Logout Utility
 * Use this to clear the backstack when logging out to free up memory.
 */
fun NavHostController.logout() {
    navigate("login") {
        popUpTo(0) { inclusive = true }
    }
}