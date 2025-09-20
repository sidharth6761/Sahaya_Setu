package com.sid.civilq_1.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState


@Composable
fun BottomNavBar(navController: NavHostController) {
    val items = listOf(
        NavItem("home", "Home", Icons.Default.Home),
        NavItem("report", "Report", Icons.Default.Warning),
        NavItem("profile", "Profile", Icons.Default.Person)
    )

    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentDestination?.route == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                label = { Text(item.title) },
                icon = { Icon(item.icon, contentDescription = item.title) }
            )
        }
    }
}

data class NavItem(val route: String, val title: String, val icon: ImageVector)
