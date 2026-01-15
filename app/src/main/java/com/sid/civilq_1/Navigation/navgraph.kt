package com.sid.civilq_1.Navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.sid.civilq_1.screens.*
import com.sid.civilq_1.ui.screens.HomeScreen
import com.sid.civilq_1.ui.screens.ReportDetailScreen
import com.sid.civilq_1.viewmodel.ReportViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val reportViewModel: ReportViewModel = viewModel()

    // Observe current route to show/hide bottom bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val auth = remember { FirebaseAuth.getInstance() }
    val startDestination = remember {
        if (auth.currentUser != null) "home" else "login"
    }

    val standardTween = tween<IntOffset>(durationMillis = 300, easing = FastOutSlowInEasing)

    // The ROOT Scaffold manages the plain bottom bar globally
    Scaffold(
        bottomBar = {
            if (currentRoute in listOf("home", "report", "profile")) {
                NavigationBar(containerColor = Color.White) {
                    val items = listOf(
                        Triple("home", "Home", Icons.Default.Home),
                        Triple("report", "Report", Icons.Default.Warning),
                        Triple("profile", "Profile", Icons.Default.Person)
                    )
                    items.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // innerPadding automatically pushes the screen content above the bottom bar
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding), // Apply padding here!
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = standardTween) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = standardTween) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = standardTween) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = standardTween) }
        ) {
            composable("login") { LoginScreen(navController) }
            composable("signup") { SignUpScreen(navController) }
            composable("home") { HomeScreen(navController, reportViewModel) }
            composable("report") { ReportScreen(navController, reportViewModel) }
            composable("profile") { ProfileScreen(navController) }
            composable("chat") { ChatScreen(navController) }
            composable(
                route = "reportdetails/{reportId}",
                arguments = listOf(navArgument("reportId") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val reportId = backStackEntry.arguments?.getString("reportId") ?: ""
                ReportDetailScreen(reportId, reportViewModel)
            }
        }
    }
}