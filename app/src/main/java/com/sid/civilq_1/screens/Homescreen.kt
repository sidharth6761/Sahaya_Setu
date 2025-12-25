package com.sid.civilq_1.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.draw.clip
import android.content.Intent


import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.shadow

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

import com.sid.civilq_1.model.Report
import com.sid.civilq_1.viewmodel.ReportViewModel
import com.sid.civilq_1.components.getUserLocation
import androidx.core.net.toUri

val ActiveColor = Color(0xFF4A90E2) // Blue for Active
val SolvedColor = Color(0xFF50E3C2)  // Green for Solved
val TabBackground = Color(0xFFE0E0E0) // Light gray for unselected background

@Composable
fun HomeScreen(
    navController: NavHostController,
    reportViewModel: ReportViewModel = viewModel()
) {
    val reports by reportViewModel.reports.collectAsStateWithLifecycle()
    val nearbyReports = reports

    val context = LocalContext.current

    // state to hold location values
    var mainAddress by remember { mutableStateOf("Fetching your area...") }
    var exactAddress by remember { mutableStateOf("Fetching exact address...") }

    // fetch location once when screen loads
    LaunchedEffect(Unit) {
        getUserLocation(context) { main, exact ->
            mainAddress = main
            exactAddress = exact
        }
    }

    // State to track selected tab: "Active" or "Solved"
    var selectedTab by remember { mutableStateOf("Active") }

    // Filtered reports based on selected tab
    val filteredReports = nearbyReports.filter { it.status == selectedTab }

    // Main container with Box to allow floating button
    Box {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F0F0))
                .padding(top=0.dp, start = 16.dp, end = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Location text (left-aligned below top bar)
            Text(
                text = "Hello, Active Citizen! 👋",
                fontSize = 34.sp,
                fontWeight = FontWeight.W800,
                color = Color.DarkGray,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 55.dp, bottom = 2.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "📍 $mainAddress",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 1.dp, bottom = 2.dp)
            )
            Text(
                text = exactAddress,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 16.dp)
            )

            // Emergency button below location
            EmergencyContactButton()

            Spacer(modifier = Modifier.height(24.dp))

            // Stats row
            ReportStatsRow(
                active = nearbyReports.count { it.status == "Active" },
                solved = nearbyReports.count { it.status == "Solved" }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pill-shaped segmented control
            SegmentedTab(
                tabs = listOf("Active", "Solved"),
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "$selectedTab Reports in Nearby Areas",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Reports List
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filteredReports.sortedByDescending { it.upvotes }.forEach { report ->
                    ReportItemCard(
                        report = report,
                        onUpvote = { reportViewModel.upvoteReport(report.id) },
                        onClick = { navController.navigate("reportdetails/${report.id}") },
                        showUpvote = (selectedTab == "Active")
                    )
                }
            }

            // Add bottom padding to prevent content from being hidden behind FAB
            Spacer(modifier = Modifier.height(80.dp))
        }

        // Floating Chat Button at bottom left
        FloatingActionButton(
            onClick = {
                navController.navigate("chat")
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start=10.dp,bottom = 100.dp)
                .size(56.dp),
            containerColor = Color(0xFF6200EE),
            contentColor = Color.White,
            shape = RoundedCornerShape(46.dp),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                painter = painterResource(id = com.sid.civilq_1.R.drawable.chatbot), // Replace with your chat icon
                contentDescription = "Open Chat Assistant",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Segmented Tab Composable
@Composable
fun SegmentedTab(
    tabs: List<String>,
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(TabBackground),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { tab ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (tab == selectedTab) ActiveColor else Color.Transparent
                    )
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab,
                    fontWeight = FontWeight.Bold,
                    color = if (tab == selectedTab) Color.White else Color.DarkGray
                )
            }
        }
    }
}

@SuppressLint("UseKtx")
@Composable
fun EmergencyContactButton() {
    val context = LocalContext.current
    val emergencyNumber = "112"

    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = "tel:$emergencyNumber".toUri()
            }
            context.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .shadow(6.dp, RoundedCornerShape(52.dp))
    ) {
        Text(
            text = "Emergency Contacts",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun ReportStatsRow(active: Int, solved: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        StatBox("Active Reports", active, ActiveColor)
        StatBox("Solved Reports", solved, SolvedColor)
    }
}

@Composable
fun StatBox(label: String, count: Int, color: Color) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    count.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ReportItemCard(
    report: Report,
    onUpvote: () -> Unit,
    onClick: () -> Unit,
    showUpvote: Boolean = true
) {
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
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when (report.status) {
                            "Active" -> ActiveColor
                            "Solved" -> SolvedColor
                            else -> Color.Gray
                        }
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(report.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Status: ${report.status} | Category: ${report.category}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (showUpvote) {
                Button(
                    onClick = onUpvote,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("👍 ${report.upvotes}")
                }
            }
        }
    }
}