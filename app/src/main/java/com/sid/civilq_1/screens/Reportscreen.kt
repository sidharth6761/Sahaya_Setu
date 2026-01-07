package com.sid.civilq_1.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.android.gms.location.*
import com.sid.civilq_1.ai.GeminiHelper
import com.sid.civilq_1.components.VoiceNoteBar
import com.sid.civilq_1.model.Report
import com.sid.civilq_1.viewmodel.ReportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
    reportViewModel: ReportViewModel
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isUiReady by reportViewModel.isUiReady.collectAsStateWithLifecycle()

    // UI States
    var formState by remember { mutableStateOf(ReportFormState()) }
    var isGeneratingDescription by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Hardware Controllers (Initialized only when needed)
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }

    val animatedWaveformData = remember { mutableStateListOf<Float>().apply { addAll(List(40) { 0.2f }) } }

    // Permission Launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms.values.any { it }) {
            getCurrentLocationOptimized(fusedClient) { loc ->
                scope.launch { formState = formState.copy(location = getAddressFromLocationOptimized(context, loc)) }
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        formState = formState.copy(imageUri = uri)
    }

    // Effect: Delay hardware request until UI transition finishes
    LaunchedEffect(isUiReady) {
        if ( isUiReady ) {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
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

                    item {
                        MediaCard(
                            formState = formState,
                            waveformData = animatedWaveformData,
                            onImageClick = { imagePickerLauncher.launch("image/*") },
                            onMicClick = {
                                val hasAudioPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (!hasAudioPerm) {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    handleRecording(context, formState, { mediaRecorder = it }, { audioFile = it }) { newState ->
                                        formState = newState
                                    }
                                }
                            },
                            onPlayClick = {
                                playAudio(context, formState.recordedAudioUri) { mediaPlayer = it }
                            }
                        )
                    }

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
                                        GeminiHelper().generateReportDescription(
                                            context, formState.imageUri!!, formState.category, formState.title, Location("")
                                        ).onSuccess { formState = formState.copy(description = it) }
                                        isGeneratingDescription = false
                                    }
                                }
                            }
                        )
                    }

                    item {
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
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A7C59))
                        ) {
                            if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else Text("Submit Report", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

// Optimized Recording Logic
private fun handleRecording(
    context: Context,
    state: ReportFormState,
    setRecorder: (MediaRecorder?) -> Unit,
    setFile: (File?) -> Unit,
    onStateChange: (ReportFormState) -> Unit
) {
    if (!state.isRecording) {
        try {
            val file = File(context.cacheDir, "rec_${System.currentTimeMillis()}.mp4")
            setFile(file)
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            setRecorder(recorder)
            onStateChange(state.copy(isRecording = true))
        } catch (e: Exception) {
            Toast.makeText(context, "Mic Busy", Toast.LENGTH_SHORT).show()
        }
    } else {
        // Stop logic
        onStateChange(state.copy(isRecording = false))
    }
}

private fun playAudio(context: Context, uri: Uri?, setPlayer: (MediaPlayer?) -> Unit) {
    uri?.let {
        val mp = MediaPlayer().apply {
            setDataSource(context, it)
            prepare()
            start()
        }
        setPlayer(mp)
    }
}

@Composable
private fun MediaCard(formState: ReportFormState, waveformData: List<Float>, onImageClick: () -> Unit, onMicClick: () -> Unit, onPlayClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = CARD_SHAPE, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Attachments", fontWeight = FontWeight.Bold)
            Surface(modifier = Modifier.fillMaxWidth().height(120.dp).clickable { onImageClick() }, shape = FIELD_SHAPE, color = Color(0xFFF1F3F4)) {
                if (formState.imageUri != null) {
                    AsyncImage(model = formState.imageUri, contentDescription = null, contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(32.dp), tint = Color.Gray)
                }
            }
            VoiceNoteBar(isRecording = formState.isRecording, onMicClick = onMicClick, audioWaveformData = waveformData)
            if (formState.recordedAudioUri != null) {
                Button(onClick = onPlayClick) { Text("Play Recording") }
            }
        }
    }
}

private suspend fun submitReportLogic(context: Context, state: ReportFormState, viewModel: ReportViewModel) {
    withContext(Dispatchers.IO) {
        val report = Report(
            id = (0..999999).random(),
            title = state.title,
            category = state.category,
            status = "Active",
            description = state.description,
            location = state.location,
            latitude = 0.0,
            longitude = 0.0,
            imageUrl = state.imageUri?.toString() ?: "",
            audioUrl = state.recordedAudioUri?.toString() ?: "",
            timestamp = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date()),
            upvotes = 0,
            departmentHeadName = "Pending",
            workerName = "Unassigned",
            workerPhone = ""
        )
        viewModel.addReport(report)
    }
}

@Composable private fun HeaderSection() {
    Column {
        Text("Create Report", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Provide details to help authorities act faster", color = Color.Gray)
    }
}

@Composable
private fun FormCard(formState: ReportFormState, isGeneratingDescription: Boolean, onTitleChange: (String) -> Unit, onCategoryChange: (String) -> Unit, onDescriptionChange: (String) -> Unit, onGenerateAI: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = CARD_SHAPE, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = formState.title, onValueChange = onTitleChange, label = { Text("Issue Title") }, modifier = Modifier.fillMaxWidth())
            DepartmentDropdown(formState.category, onCategoryChange)
            OutlinedTextField(value = formState.location, onValueChange = {}, readOnly = true, label = { Text("Location") }, trailingIcon = { Icon(Icons.Default.LocationOn, null, tint = Color.Red) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = formState.description, onValueChange = onDescriptionChange, label = { Text("Description") }, modifier = Modifier.fillMaxWidth().height(120.dp))
            if (formState.imageUri != null) {
                Button(
                    onClick = onGenerateAI,
                    enabled = !isGeneratingDescription
                ) { Text("✨ AI Describe") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepartmentDropdown(selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = selected, onValueChange = {}, readOnly = true, label = {Text("Category")}, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DEPARTMENTS.forEach { dept -> DropdownMenuItem(text = { Text(dept) }, onClick = { onSelected(dept); expanded = false }) }
        }
    }
}

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