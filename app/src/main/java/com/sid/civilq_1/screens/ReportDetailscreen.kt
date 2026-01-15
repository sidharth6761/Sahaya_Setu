package com.sid.civilq_1.ui.screens

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sid.civilq_1.viewmodel.ReportViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    reportId: String, // String type to match UUID from Supabase
    reportViewModel: ReportViewModel = viewModel()
) {
    val reports by reportViewModel.reports.collectAsStateWithLifecycle()
    val report = remember(reports) { reports.find { it.id == reportId } }

    val scrollState = rememberScrollState()
    val view = LocalView.current

    if (report == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF4A90E2))
            Text(
                text = "Loading report details...",
                modifier = Modifier.padding(top = 80.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header Section
            Text(
                modifier = Modifier.padding(top = 48.dp),
                text = report.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(containerColor = if(report.status == "Pending") Color(0xFF4A90E2) else Color(0xFF50E3C2)) {
                    Text(report.status, color = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = report.category, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description Section
            Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(report.description, style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(16.dp))

            // Evidence Image
            if (!report.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = report.imageUrl,
                    contentDescription = "Report Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Map Section
            Text("Incident Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(report.location, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .pointerInteropFilter { motionEvent ->
                        when (motionEvent.action) {
                            MotionEvent.ACTION_DOWN -> view.parent?.requestDisallowInterceptTouchEvent(true)
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> view.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        false
                    }
            ) {
                // Parsing Supabase lat_long text "lat,long"
                val coords = report.latLong?.split(",")
                val lat = coords?.getOrNull(0)?.toDoubleOrNull() ?: 20.2961
                val lon = coords?.getOrNull(1)?.toDoubleOrNull() ?: 85.8245

                ReportLocationMap(
                    lat = lat,
                    lon = lon,
                    locationName = report.location,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Details Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Report Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    InfoRow(label = "Current Status", value = report.status)
                    InfoRow(label = "Department", value = report.category)
                    InfoRow(label = "Report ID", value = report.id?.take(8) ?: "N/A")
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("$label: ", fontWeight = FontWeight.Medium, fontSize = 14.sp)
        Text(value, fontSize = 14.sp, color = Color.DarkGray)
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun ReportLocationMap(
    lat: Double,
    lon: Double,
    locationName: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                controller.setZoom(16.0)
                val geoPoint = GeoPoint(lat, lon)
                controller.setCenter(geoPoint)
                setMultiTouchControls(true)

                val marker = Marker(this).apply {
                    position = geoPoint
                    title = locationName
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                overlays.add(marker)
            }
        },
        modifier = modifier
    )
}