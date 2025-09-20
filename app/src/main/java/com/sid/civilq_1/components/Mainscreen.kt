package com.sid.civilq_1.components

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.sid.civilq_1.screens.ProfileScreen
import com.sid.civilq_1.screens.ReportDetailScreen
import com.sid.civilq_1.ui.screens.HomeScreen
import com.sid.civilq_1.ui.screens.ReportScreen
import com.sid.civilq_1.viewmodel.ReportViewModel

@Composable
fun MainScreen(navController: NavHostController) {
    val reportViewModel: ReportViewModel = viewModel()
    val context: Context = LocalContext.current

    Scaffold(
        topBar = {
            MyTopBar(
                title="Saha Setu",
                context = context,
                actions = {

                }
            )
        },
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen(navController, reportViewModel) }
            composable("report") { ReportScreen(navController, reportViewModel) }
            composable("profile") { ProfileScreen(navController) }
            composable(
                route = "reportdetails/{reportId}",
                arguments = listOf(navArgument("reportId") { type = NavType.IntType })
            ) { backStackEntry ->
                val reportId = backStackEntry.arguments?.getInt("reportId") ?: 0
                ReportDetailScreen(reportId = reportId, reportViewModel = reportViewModel)
            }
        }
    }
}
