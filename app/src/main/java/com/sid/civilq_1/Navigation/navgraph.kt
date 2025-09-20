package com.sid.civilq_1.Navigation

import androidx.compose.runtime.Composable
import com.sid.civilq_1.screens.*
import com.sid.civilq_1.ui.screens.HomeScreen
import com.sid.civilq_1.ui.screens.ReportScreen
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController // ✅ use this

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigation() {
    // ✅ Use rememberAnimatedNavController() instead of rememberNavController()
    val navController = rememberAnimatedNavController()

    AnimatedNavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth }, // slide from right
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth }, // slide to left
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth }, // slide from left (back)
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth }, // slide to right (back)
                animationSpec = tween(300)
            )
        }
    ) {
        composable("home") { HomeScreen(navController) }
        composable("report") { ReportScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
    }
}
