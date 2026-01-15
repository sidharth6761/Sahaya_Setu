package com.sid.civilq_1.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.android.gms.location.*
import com.sid.civilq_1.ai.GeminiHelper
import com.sid.civilq_1.components.VoiceNoteBar
import com.sid.civilq_1.model.Report
import com.sid.civilq_1.viewmodel.ReportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

private val DEPARTMENTS = listOf("Fire", "Road", "Potholes", "Sanitation", "Traffic", "Administration", "Urban Planning", "Water Supply", "Electricity", "Others")
private val BACKGROUND_COLOR = Color(0xFFF8F9FA)
private val CARD_SHAPE = RoundedCornerShape(20.dp)
private val FIELD_SHAPE = RoundedCornerShape(12.dp)

@Stable
data class ReportFormState(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val location: String = "Detecting location...",
    val imageUri: Uri? = null,
    val isRecording: Boolean = false,
    val recordedAudioUri: Uri? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun ReportScreen(
    navController: NavHostController,
    reportViewModel: ReportViewModel
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isUiReady by reportViewModel.isUiReady.collectAsStateWithLifecycle()

    var formState by remember { mutableStateOf(ReportFormState()) }
    var isGeneratingDescription by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // --- Permissions and Launchers ---
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            getCurrentLocationOptimized(fusedClient) { loc ->
                scope.launch {
                    formState = formState.copy(
                        location = getAddressFromLocationOptimized(context, loc),
                        latitude = loc.latitude,
                        longitude = loc.longitude
                    )
                }
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        formState = formState.copy(imageUri = uri)
    }

    LaunchedEffect(isUiReady) {
        if (isUiReady) {
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    Scaffold(containerColor = BACKGROUND_COLOR) { innerPadding ->
        Crossfade(targetState = isUiReady, label = "ReportScreenFade") { ready ->
            if (!ready) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF4A7C59))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item { HeaderSection() }

                    // Attachment Section (Image & Voice)
                    item {
                        MediaCard(
                            formState = formState,
                            onImageClick = { imagePickerLauncher.launch("image/*") },
                            onMicClick = { /* Handle audio recording if needed */ }
                        )
                    }

                    // Form Input Section
                    item {
                        FormCard(
                            formState = formState,
                            isGeneratingDescription = isGeneratingDescription,
                            onTitleChange = { formState = formState.copy(title = it) },
                            onCategoryChange = { formState = formState.copy(category = it) },
                            onDescriptionChange = { formState = formState.copy(description = it) },
                            onGenerateAI = {
                                if (formState.imageUri != null) {
                                    scope.launch {
                                        isGeneratingDescription = true
                                        try {
                                            val result = withContext(Dispatchers.IO) {
                                                GeminiHelper().generateReportDescription(
                                                    context, formState.imageUri!!, formState.category, formState.title, Location("")
                                                )
                                            }
                                            result.onSuccess { formState = formState.copy(description = it) }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "AI Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isGeneratingDescription = false
                                        }
                                    }
                                }
                            }
                        )
                    }

                    // Submission Button
                    item {
                        Button(
                            onClick = {
                                if (formState.title.isBlank() || formState.category.isBlank() || formState.imageUri == null) {
                                    Toast.makeText(context, "Title, Category, and Image are required", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isSubmitting = true
                                scope.launch {
                                    try {
                                        // 1. Prepare Report Object
                                        val newReport = Report(
                                            title = formState.title,
                                            category = formState.category,
                                            description = formState.description,
                                            location = formState.location,
                                            latLong = "${formState.latitude},${formState.longitude}",
                                            status = "Active"
                                        )

                                        // 2. Delegate to ViewModel (Handles Upload + DB Insert + Firebase UID)
                                        reportViewModel.addReport(
                                            context = context,
                                            report = newReport,
                                            imageUri = formState.imageUri
                                        )

                                        Toast.makeText(context, "Submission Successful!", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    } catch (e: Exception) {
                                        Log.e("ReportSubmit", "Failed: ${e.message}")
                                        Toast.makeText(context, "Submission failed", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A7C59))
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Submit Report", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

// --- Supporting UI Components ---

@Composable
private fun HeaderSection() {
    Column {
        Text("Create Report", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Provide details to help authorities act faster", color = Color.Gray)
    }
}

@Composable
private fun FormCard(
    formState: ReportFormState,
    isGeneratingDescription: Boolean,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onGenerateAI: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = CARD_SHAPE, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = formState.title, onValueChange = onTitleChange, label = { Text("Issue Title") }, modifier = Modifier.fillMaxWidth())
            DepartmentDropdown(formState.category, onCategoryChange)
            OutlinedTextField(value = formState.location, onValueChange = {}, readOnly = true, label = { Text("Location") }, trailingIcon = { Icon(Icons.Default.LocationOn, null, tint = Color.Red) }, modifier = Modifier.fillMaxWidth())

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = formState.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )
                if (isGeneratingDescription) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(32.dp), color = Color(0xFF4A7C59))
                }
            }

            if (formState.imageUri != null) {
                Button(
                    onClick = onGenerateAI,
                    enabled = !isGeneratingDescription,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A7C59).copy(alpha = 0.1f), contentColor = Color(0xFF4A7C59))
                ) {
                    Text("✨ AI Describe Image", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MediaCard(formState: ReportFormState, onImageClick: () -> Unit, onMicClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = CARD_SHAPE, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Attachments", fontWeight = FontWeight.Bold)
            Surface(
                modifier = Modifier.fillMaxWidth().height(160.dp).clickable { onImageClick() },
                shape = FIELD_SHAPE,
                color = Color(0xFFF1F3F4)
            ) {
                if (formState.imageUri != null) {
                    AsyncImage(model = formState.imageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Call, null, modifier = Modifier.size(32.dp), tint = Color.Gray)
                            Text("Add Photo of the Issue", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepartmentDropdown(selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = if (selected.isEmpty()) "Select Category" else selected,
            onValueChange = {},
            readOnly = true,
            label = {Text("Category")},
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DEPARTMENTS.forEach { dept ->
                DropdownMenuItem(text = { Text(dept) }, onClick = { onSelected(dept); expanded = false })
            }
        }
    }
}

// --- Helper Functions ---

@SuppressLint("MissingPermission")
private fun getCurrentLocationOptimized(fusedClient: FusedLocationProviderClient, onLocation: (Location) -> Unit) {
    fusedClient.lastLocation.addOnSuccessListener { loc ->
        if (loc != null) onLocation(loc)
        else {
            val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).setMaxUpdates(1).build()
            fusedClient.requestLocationUpdates(req, object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) { p0.locations.firstOrNull()?.let(onLocation) }
            }, Looper.getMainLooper())
        }
    }
}

private suspend fun getAddressFromLocationOptimized(context: Context, location: Location): String {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addrs = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            addrs?.firstOrNull()?.getAddressLine(0) ?: "Unknown Location"
        } catch (e: Exception) { "Location Error" }
    }
}