package com.sid.civilq_1.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Direct Card without Box wrapper
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(95.dp)
            .padding(horizontal = 50.dp, vertical = 20.dp), // Moved padding here ,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White) //transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Glassmorphism background
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Blue.copy(alpha = 0.6f),
                            Color.Magenta.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)

                )
        ) {
            // Inner glow effect
            /*Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
            )*/

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    NavBarItem(
                        item = item,
                        isSelected = currentDestination?.route == item.route,
                        onClick = {
                            if (currentDestination?.route != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavBarItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        // Background for selected item
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isSelected) Color(0xFF6200EE) else Color.Transparent,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (isSelected) Color.White else Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Text for selected item
        if (isSelected) {
            Text(
                text = item.title,
                color = Color(0xFF6200EE),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

data class NavItem(val route: String, val title: String, val icon: ImageVector)