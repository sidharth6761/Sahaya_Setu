package com.sid.civilq_1.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import com.sid.civilq_1.model.Report
import com.sid.civilq_1.viewmodel.ReportViewModel
import com.sid.civilq_1.components.getUserLocation

// Constants
val ActiveColor = Color(0xFF4A90E2)
val SolvedColor = Color(0xFF50E3C2)
val TabBackground = Color(0xFFE0E0E0)

@Composable
fun HomeScreen(
    navController: NavHostController,
    reportViewModel: ReportViewModel = viewModel()
) {
    val reports by reportViewModel.reports.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // State for location
    var mainAddress by remember { mutableStateOf("Fetching your area...") }
    var exactAddress by remember { mutableStateOf("Fetching exact address...") }
    var selectedTab by remember { mutableStateOf("Active") }

    // Fetch location once
    LaunchedEffect(Unit) {
        getUserLocation(context) { main, exact ->
            mainAddress = main
            exactAddress = exact
        }
    }

    // PERFORMANCE FIX: Only filter/sort when 'reports' or 'selectedTab' actually changes
    val filteredReports by remember(reports, selectedTab) {
        derivedStateOf {
            reports.filter { it.status == selectedTab }
                .sortedByDescending { it.upvotes }
        }
    }

    // PERFORMANCE FIX: Calculate stats only when 'reports' list changes
    val activeCount by remember(reports) { derivedStateOf { reports.count { it.status == "Active" } } }
    val solvedCount by remember(reports) { derivedStateOf { reports.count { it.status == "Solved" } } }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("chat") },
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(56.dp),
                containerColor = Color(0xFF6200EE),
                contentColor = Color.White,
                shape = RoundedCornerShape(46.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    painter = painterResource(id = com.sid.civilq_1.R.drawable.chatbot),
                    contentDescription = "Chat",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Start
    ) { paddingValues ->
        // PERFORMANCE FIX: LazyColumn only renders what is visible on screen
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F0F0))
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            item {
                Column(modifier = Modifier.padding(top = 40.dp)) {
                    Text(
                        text = "Hello, Active Citizen! 👋",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "📍 $mainAddress", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(text = exactAddress, fontSize = 12.sp, color = Color.Gray)
                }
            }

            // Quick Actions
            item { EmergencyContactButton() }

            // Stats
            item { ReportStatsRow(active = activeCount, solved = solvedCount) }

            // Tab Selector
            item {
                SegmentedTab(
                    tabs = listOf("Active", "Solved"),
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            item {
                Text(
                    text = "$selectedTab Reports Nearby",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // The Dynamic List
            items(items = filteredReports, key = { it.id }) { report ->
                ReportItemCard(
                    report = report,
                    onUpvote = { reportViewModel.upvoteReport(report.id) },
                    onClick = { navController.navigate("reportdetails/${report.id}") },
                    showUpvote = (selectedTab == "Active")
                )
            }

            // Bottom Spacing for FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun SegmentedTab(tabs: List<String>, selectedTab: String, onTabSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(TabBackground)
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

@SuppressLint("UseKtx")
@Composable
fun EmergencyContactButton() {
    val context = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_DIAL).apply { data = "tel:112".toUri() }
            context.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
    ) {
        Text("Emergency Contacts", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ReportStatsRow(active: Int, solved: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatBox("Active", active, ActiveColor, Modifier.weight(1f))
        StatBox("Solved", solved, SolvedColor, Modifier.weight(1f))
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
            Text(label, fontSize = 12.sp, color = Color.White)
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
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (report.status == "Active") ActiveColor else SolvedColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(report.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("${report.category} • ${report.status}", fontSize = 12.sp, color = Color.Gray)
            }
            if (showUpvote) {
                Button(
                    onClick = onUpvote,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("👍 ${report.upvotes}")
                }
            }
        }
    }
}