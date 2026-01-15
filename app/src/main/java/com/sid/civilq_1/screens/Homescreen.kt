package com.sid.civilq_1.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.sid.civilq_1.R
import com.sid.civilq_1.model.Report
import com.sid.civilq_1.viewmodel.ReportViewModel
import com.sid.civilq_1.components.getUserLocation

// Branding Colors
val ActiveColor = Color(0xFF4A90E2)
val SolvedColor = Color(0xFF50E3C2)
val TabBackground = Color(0xFFE0E0E0)
val BackgroundGray = Color(0xFFF0F0F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    reportViewModel: ReportViewModel = viewModel()
) {
    val reports by reportViewModel.reports.collectAsStateWithLifecycle()
    val userName by reportViewModel.userName.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Firebase Auth for user-specific filtering
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }

    // UI State
    var isRefreshing by remember { mutableStateOf(false) }
    var mainAddress by remember { mutableStateOf("Fetching area...") }
    var exactAddress by remember { mutableStateOf("Fetching address...") }
    var selectedTab by remember { mutableStateOf("Active") }

    // Initial Data Fetch
    LaunchedEffect(Unit) {
        reportViewModel.fetchReportsFromSupabase()
        reportViewModel.fetchUserProfile() // Refresh user name on launch
        getUserLocation(context) { main, exact ->
            mainAddress = main
            exactAddress = exact
        }
    }

    // Filtered list based on selected tab
    // The sorting logic (highest upvotes) is handled inside the ViewModel
    val filteredReports = remember(reports, selectedTab) {
        when (selectedTab) {
            "Active" -> reports.filter { it.status.equals("Active", ignoreCase = true) }
            "Reports" -> reports.filter { it.userId == currentUserId }
            else -> reports
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("chat") },
                containerColor = Color(0xFF6200EE),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.chatbot),
                    contentDescription = "Chat",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                reportViewModel.fetchReportsFromSupabase()
                // The ViewModel update will push new data to 'reports',
                // we set isRefreshing to false once the logic completes
                isRefreshing = false
            },
            modifier = Modifier.padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundGray)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
            ) {
                // 1. Welcome Header
                item {
                    Column {
                        Text("Hello, $userName! 👋", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Text("📍 $mainAddress", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(exactAddress, fontSize = 12.sp, color = Color.Gray)
                    }
                }

                // 2. Emergency Section
                item { EmergencyContactButton() }

                // 3. Status Statistics
                item {
                    val activeCount = reports.count { it.status.equals("Active", ignoreCase = true) }
                    ReportStatsRow(active = activeCount, total = reports.size)
                }

                // 4. Navigation Tabs
                item {
                    SegmentedTab(
                        tabs = listOf("Active", "Reports"),
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }

                // 5. Section Title
                item {
                    Text("$selectedTab Issues", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                // 6. Conditional List View
                if (filteredReports.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("No items found in $selectedTab", color = Color.Gray)
                        }
                    }
                } else {
                    items(
                        items = filteredReports,
                        // Using report ID as the key is essential for smooth sorting animations
                        key = { it.id ?: it.hashCode().toString() }
                    ) { report ->
                        ReportItemCard(
                            report = report,
                            onUpvote = {
                                report.id?.let { reportViewModel.upvoteReport(it) }
                            },
                            onClick = {
                                navController.navigate("reportdetails/${report.id}")
                            },
                            // Only show upvote button on the public "Active" tab
                            showUpvote = selectedTab == "Active"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SegmentedTab(tabs: List<String>, selectedTab: String, onTabSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(TabBackground)
            .padding(4.dp)
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (isSelected) ActiveColor else Color.Transparent)
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun ReportItemCard(report: Report, onUpvote: () -> Unit, onClick: () -> Unit, showUpvote: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Dot: Uses branding colors
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (report.status.equals("Active", ignoreCase = true)) ActiveColor else SolvedColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(report.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${report.category} • ${report.status.uppercase()}", fontSize = 12.sp, color = Color.Gray)
            }

            // Upvote logic: Integrated with ViewModel optimistic updates
            if (showUpvote && report.status.equals("Active", ignoreCase = true)) {
                OutlinedButton(
                    onClick = { onUpvote() },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(38.dp),
                    border = BorderStroke(1.dp, ActiveColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ActiveColor)
                ) {
                    Text("👍 ${report.upvotes}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReportStatsRow(active: Int, total: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatBox("Active Issues", active, ActiveColor, Modifier.weight(1f))
        StatBox("Total Submissions", total, Color(0xFF9C27B0), Modifier.weight(1f))
    }
}

@Composable
fun StatBox(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(count.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@SuppressLint("UseKtx")
@Composable
fun EmergencyContactButton() {
    val context = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_DIAL).apply { data = "tel:112".toUri() }
            context.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
    ) {
        Icon(
            painter = painterResource(id = R.drawable.mic_svgrepo_com),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text("Emergency Contacts (112)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}