package com.sid.civilq_1.screens

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sid.civilq_1.model.Report
import com.sid.civilq_1.viewmodel.ReportViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun ReportDetailScreen(
    reportId: Int,
    reportViewModel: ReportViewModel = viewModel()
) {
    val reports by reportViewModel.reports.collectAsState()
    val report = reports.find { it.id == reportId }

    if (report == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Report not found!", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    // shared scroll state so we can control/inspect if needed
    val scrollState = rememberScrollState()
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Title
        Text(
            text = report.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Description
        Text("Description", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(report.description ?: "No description provided")
        Spacer(modifier = Modifier.height(12.dp))

        // Image
        if (!report.imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = report.imageUrl,
                contentDescription = "Report Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text("No image provided", color = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Audio
        if (!report.audioUrl.isNullOrEmpty()) {
            Text("Audio Evidence", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = { /* TODO: Play audio */ }) {
                Text("Play Audio")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

/*
        // Updates
        report.updates?.let { updates ->
            if (updates.isNotEmpty()) {
                Text("Updates", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                updates.forEach { update ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEFEF))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                update.time ?: "Time not available",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(update.description ?: "No update details")
                        }
                    }
                }
            }
        }
*/

        Spacer(modifier = Modifier.height(16.dp))

        // Map section - intercept touch to prevent parent scroll stealing gestures
        Text("Worker Location", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(16.dp))
                // pointerInteropFilter used to call requestDisallowInterceptTouchEvent on the parent Android view
                .pointerInteropFilter { motionEvent ->
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // tell Android parent views not to intercept while interacting with the map
                            view.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            view.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    // return false so MapView (AndroidView) still receives the event
                    false
                }
        ) {
            ReportLocationMap(
                lat = report.latitude ?: 20.2961,
                lon = report.longitude ?: 85.8245,
                locationName = report.location,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Department & Worker Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Department & Worker Info", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Department Head: ${report.departmentHeadName ?: "N/A"}")
                Text("Worker Name: ${report.workerName ?: "N/A"}")
                Text("Worker Phone: ${report.workerPhone ?: "N/A"}")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun ReportLocationMap(
    lat: Double,
    lon: Double,
    locationName: String = "Report Location",
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                controller.setZoom(15.0)
                val geoPoint = GeoPoint(lat, lon)
                controller.setCenter(geoPoint)
                setMultiTouchControls(true)

                // marker
                val marker = Marker(this).apply {
                    position = geoPoint
                    title = locationName
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                marker.setOnMarkerClickListener { m, _ ->
                    m.showInfoWindow()
                    true
                }
                overlays.add(marker)

                // ALSO set OnTouchListener on the MapView to request parent not intercept
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> v.parent?.requestDisallowInterceptTouchEvent(true)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    // return false so MapView still handles the event as usual
                    false
                }
            }
        },
        modifier = modifier
    )
}
