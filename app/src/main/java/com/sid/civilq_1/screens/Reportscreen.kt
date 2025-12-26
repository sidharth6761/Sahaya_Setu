package com.sid.civilq_1.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.sid.civilq_1.R
import com.sid.civilq_1.components.VoiceNoteBar
import com.sid.civilq_1.model.Report
import com.sid.civilq_1.viewmodel.ReportViewModel
import com.google.android.gms.location.*
import com.sid.civilq_1.ai.GeminiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
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
    val recordedAudioUri: Uri? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun ReportScreen(
    navController: NavHostController,
    reportViewModel: ReportViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val geminiHelper = remember { GeminiHelper() }

    // UI States
    var formState by remember { mutableStateOf(ReportFormState()) }
    var isGeneratingDescription by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Hardware Controllers
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }

    // PERFORMANCE: Cleanup hardware resources on exit
    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.apply {
                try { stop() } catch (e: Exception) {}
                release()
            }
        }
    }

    // Launchers
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
            uri -> formState = formState.copy(imageUri = uri)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            getCurrentLocationOptimized(fusedClient) { loc ->
                scope.launch {
                    val address = getAddressFromLocationOptimized(context, loc)
                    formState = formState.copy(location = address)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    Scaffold(
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                Button(
                    onClick = {
                        if (!isSubmitting) {
                            isSubmitting = true
                            scope.launch {
                                submitReportLogic(context, formState, reportViewModel)
                                isSubmitting = false
                                navController.popBackStack()
                            }
                        }
                    },
                    enabled = formState.title.isNotBlank() && formState.category.isNotBlank() && !isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Submit Report", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BACKGROUND_COLOR)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            HeaderSection()

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
                            geminiHelper.generateReportDescription(context, formState.imageUri!!, formState.category, formState.title, Location("").apply { latitude = 0.0; longitude = 0.0 })
                                .onSuccess { formState = formState.copy(description = it) }
                            isGeneratingDescription = false
                        }
                    }
                }
            )

            MediaCard(
                formState = formState,
                onImageClick = { imagePickerLauncher.launch("image/*") },
                onMicClick = {
                    if (!formState.isRecording) {
                        audioFile = File(context.cacheDir, "rec_${System.currentTimeMillis()}.mp3")
                        mediaRecorder = MediaRecorder().apply {
                            setAudioSource(MediaRecorder.AudioSource.MIC)
                            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                            setOutputFile(audioFile!!.absolutePath)
                            prepare()
                            start()
                        }
                        formState = formState.copy(isRecording = true)
                    } else {
                        mediaRecorder?.apply { stop(); release() }
                        mediaRecorder = null
                        formState = formState.copy(isRecording = false, recordedAudioUri = audioFile?.toUri())
                    }
                }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// Logic separation to reduce UI overloading
private suspend fun submitReportLogic(context: Context, state: ReportFormState, viewModel: ReportViewModel) {
    withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        val coords = try { geocoder.getFromLocationName(state.location, 1) } catch (e: Exception) { null }

        val report = Report(
            id = (0..999999).random(),
            title = state.title,
            category = state.category,
            status = "Active",
            description = state.description,
            location = state.location,
            latitude = coords?.firstOrNull()?.latitude,
            longitude = coords?.firstOrNull()?.longitude,
            imageUrl = state.imageUri?.toString() ?: "",
            audioUrl = state.recordedAudioUri?.toString() ?: "",
            timestamp = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date()),
            upvotes = 0,
            departmentHeadName = "not assigned",
            workerName = "not assigned",
            workerPhone = "not assigned"
        )
        viewModel.addReport(report)
    }
}

@Composable
private fun HeaderSection() {
    Column {
        Text("Create Report", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1C1E))
        Text("Provide details to help authorities act faster", fontSize = 14.sp, color = Color.Gray)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CARD_SHAPE,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = formState.title,
                onValueChange = onTitleChange,
                label = { Text("Issue Title") },
                modifier = Modifier.fillMaxWidth(),
                shape = FIELD_SHAPE
            )

            DepartmentDropdown(formState.category, onCategoryChange)

            OutlinedTextField(
                value = formState.location,
                onValueChange = {},
                readOnly = true,
                label = { Text("Location") },
                trailingIcon = { Icon(Icons.Default.LocationOn, null, tint = Color.Red) },
                modifier = Modifier.fillMaxWidth(),
                shape = FIELD_SHAPE
            )

            Box {
                OutlinedTextField(
                    value = formState.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    shape = FIELD_SHAPE,
                    placeholder = { Text("AI can help you write this...") }
                )
                if (formState.imageUri != null) {
                    TextButton(
                        onClick = onGenerateAI,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                        enabled = !isGeneratingDescription
                    ) {
                        if (isGeneratingDescription) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        else Text("✨ Generate with AI", fontWeight = FontWeight.Bold)
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
            value = selected.ifBlank { "Select Category" },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = FIELD_SHAPE
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DEPARTMENTS.forEach { dept ->
                DropdownMenuItem(text = { Text(dept) }, onClick = { onSelected(dept); expanded = false })
            }
        }
    }
}

@Composable
private fun MediaCard(formState: ReportFormState, onImageClick: () -> Unit, onMicClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CARD_SHAPE,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Attachments", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.weight(1f).height(100.dp).clickable { onImageClick() },
                    shape = FIELD_SHAPE,
                    color = Color(0xFFF1F3F4),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    if (formState.imageUri != null) {
                        AsyncImage(model = formState.imageUri, contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Outlined.Add, null)
                            Text("Add Photo", fontSize = 12.sp)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f).height(100.dp),
                    shape = FIELD_SHAPE,
                    color = if (formState.isRecording) Color(0xFFFFEBEE) else Color(0xFFF1F3F4),
                    border = BorderStroke(1.dp, if (formState.isRecording) Color.Red else Color.LightGray)
                ) {
                    VoiceNoteBar(isRecording = formState.isRecording, onMicClick = onMicClick, audioWaveformData = List(20){ 0.5f })
                }
            }
        }
    }
}

// Optimized Location helpers
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