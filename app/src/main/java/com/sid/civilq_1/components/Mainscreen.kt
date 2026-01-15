import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.sid.civilq_1.components.BottomNavBar
import com.sid.civilq_1.screens.ChatScreen
import com.sid.civilq_1.screens.ProfileScreen
import com.sid.civilq_1.screens.ReportScreen
import com.sid.civilq_1.ui.screens.HomeScreen
import com.sid.civilq_1.ui.screens.ReportDetailScreen
import com.sid.civilq_1.viewmodel.ReportViewModel


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(navController: NavHostController) {
    val reportViewModel: ReportViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define which routes should show the bottom bar
    val showBottomBar = currentRoute in listOf("home", "report", "profile")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                // This will now show a standard, plain bottom bar
                BottomNavBar(navController)
            }
        }
    ) { innerPadding ->
        // The innerPadding automatically handles the space for the bottom bar
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen(navController, reportViewModel) }
            composable("report") { ReportScreen(navController, reportViewModel) }
            composable("profile") { ProfileScreen(navController) }

            // Screens without bottom bar
            composable("chat") { ChatScreen(navController) }
            composable(
                route = "reportdetails/{reportId}",
                arguments = listOf(navArgument("reportId") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("reportId") ?: ""
                ReportDetailScreen(id, reportViewModel)
            }
        }
    }
}