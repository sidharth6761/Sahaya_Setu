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
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

// Precomputed static data for performance
private val DEPARTMENTS = listOf(
    "Fire", "Road", "Potholes", "Sanitation", "Traffic",
    "Administration", "Urban Planning", "Water Supply", "Electricity", "Others"
)
private val BACKGROUND_COLOR = Color(0xFFF0F0F0)
private val CARD_SHAPE = RoundedCornerShape(16.dp)
private val FIELD_SHAPE = RoundedCornerShape(12.dp)

// Data class for form state to reduce recompositions
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
    var isGeneratingDescription by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // ✅ FIXED: GeminiHelper no longer needs the API key passed in its constructor.
    // It now securely gets the key from BuildConfig.
    val geminiHelper = remember { GeminiHelper() }

    var formState by remember { mutableStateOf(ReportFormState()) }
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val scrollState = rememberScrollState()
    val waveformData = remember { mutableStateListOf<Float>().apply { repeat(20) { add(0.5f) } } }
    var mediaRecorder: MediaRecorder? by remember { mutableStateOf(null) }
    var audioFile: File? by remember { mutableStateOf(null) }

    val onGenerateDescription = { 
        if (formState.imageUri != null && formState.category.isNotBlank()) {
            scope.launch {
                isGeneratingDescription = true
                geminiHelper.generateReportDescription(
                    context = context,
                    imageUri = formState.imageUri!!,
                    department = formState.category,
                    title = formState.title,
                    location = Location("").apply {
                        val geo = Geocoder(context, Locale.getDefault())
                        val coords = try {
                            geo.getFromLocationName(formState.location, 1)
                        } catch (e: IOException) {
                            null
                        }
                        val lat = coords?.firstOrNull()?.latitude ?: 0.0
                        val lon = coords?.firstOrNull()?.longitude ?: 0.0
                        latitude = lat
                        longitude = lon
                    }
                ).onSuccess { description ->
                    formState = formState.copy(description = description)
                    isGeneratingDescription = false
                }.onFailure { error ->
                    Toast.makeText(context, "AI failed: ${error.message}", Toast.LENGTH_LONG).show()
                    isGeneratingDescription = false
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            getCurrentLocationOptimized(fusedClient) { loc ->
                formState = formState.copy(location = getAddressFromLocationOptimized(context, loc))
            }
        } else {
            formState = formState.copy(location = "Location permission denied")
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        formState = formState.copy(imageUri = uri)
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val onSubmitClick = {
        if (formState.title.isNotBlank() && formState.category.isNotBlank()) {
            val geo = Geocoder(context, Locale.getDefault())
            val coords = try {
                geo.getFromLocationName(formState.location, 1)
            } catch (e: IOException) {
                null
            }
            val lat = coords?.firstOrNull()?.latitude
            val lon = coords?.firstOrNull()?.longitude

            val newReport = Report(
                id = (0..1000).random(), // Use a better ID generation in a real app
                title = formState.title,
                category = formState.category,
                status = "Active",
                description = formState.description,
                location = formState.location,
                latitude = lat,
                longitude = lon,
                imageUrl = formState.imageUri?.toString() ?: "",
                audioUrl = formState.recordedAudioUri?.toString() ?: "",
                upvotes = 0,
                timestamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date()),
                departmentHeadName = "Not Assigned",
                workerName = "Not Assigned",
                workerPhone = "N/A"
            )

            reportViewModel.addReport(newReport)
            navController.popBackStack()
        }
    }

    val onMicClick = {
        if (!formState.isRecording) {
            audioFile = File(context.cacheDir, "report_audio_${System.currentTimeMillis()}.mp3")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile!!.absolutePath)
                try {
                    prepare()
                    start()
                    formState = formState.copy(isRecording = true)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } else {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            formState = formState.copy(
                isRecording = false,
                recordedAudioUri = audioFile?.toUri()
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BACKGROUND_COLOR)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HeaderCard()

            FormCard(
                formState = formState,
                isGeneratingDescription = isGeneratingDescription,
                onTitleChange = { formState = formState.copy(title = it) },
                onCategoryChange = { formState = formState.copy(category = it) },
                onDescriptionChange = { formState = formState.copy(description = it) },
                onGenerateDescription = onGenerateDescription
            )

            MediaCard(
                formState = formState,
                onImageClick = { imagePickerLauncher.launch("image/*") },
                onMicClick = onMicClick,
                waveformData = waveformData
            )

            SubmitButton(
                enabled = formState.title.isNotBlank() && formState.category.isNotBlank(),
                onClick = onSubmitClick
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun HeaderCard() {
    Card(
        modifier = Modifier
            .padding(top = 45.dp)
            .fillMaxWidth()
            .height(70.dp)
            .shadow(4.dp, CARD_SHAPE),
        shape = CARD_SHAPE,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding(start = 14.dp, top = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Submit New Report",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Help us improve your community",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun FormCard(
    formState: ReportFormState,
    isGeneratingDescription: Boolean,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onGenerateDescription: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, CARD_SHAPE),
        shape = CARD_SHAPE,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            FormField(
                icon = Icons.Outlined.Edit,
                label = "Report Title",
                content = {
                    OutlinedTextField(
                        value = formState.title,
                        onValueChange = onTitleChange,
                        placeholder = { Text("Enter a descriptive title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = FIELD_SHAPE,
                        colors = optimizedTextFieldColors()
                    )
                }
            )

            FormField(
                icon = Icons.Outlined.Menu,
                label = "Department",
                content = {
                    DepartmentDropdownOptimized(
                        selectedCategory = formState.category,
                        onCategorySelected = onCategoryChange
                    )
                }
            )

            FormField(
                icon = Icons.Outlined.LocationOn,
                label = "Location",
                content = { 
                    OutlinedTextField(
                        value = formState.location,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = FIELD_SHAPE,
                        colors = optimizedTextFieldColors()
                    )
                }
            )

            FormField(
                icon = Icons.Outlined.Info,
                label = "Description",
                content = {
                    Column {
                        OutlinedTextField(
                            value = formState.description,
                            onValueChange = onDescriptionChange,
                            placeholder = {
                                Text(
                                    if (isGeneratingDescription) "AI is generating description..."
                                    else "Describe the issue in detail..."
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            maxLines = 5,
                            shape = FIELD_SHAPE,
                            colors = optimizedTextFieldColors(),
                            enabled = !isGeneratingDescription,
                            trailingIcon = {
                                if (isGeneratingDescription) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                }
                            }
                        )

                        if (formState.imageUri != null && formState.category.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = onGenerateDescription,
                                    enabled = !isGeneratingDescription
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Star, // ✅ FIXED: Using a valid Material Icon
                                        contentDescription = "AI Generate",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Generate with AI")
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepartmentDropdownOptimized(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedCategory.ifBlank { "Select a department" },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = FIELD_SHAPE,
            colors = optimizedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DEPARTMENTS.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun optimizedTextFieldColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
     
    )
}

@Composable
private fun MediaCard(
    formState: ReportFormState,
    onImageClick: () -> Unit,
    onMicClick: () -> Unit,
    waveformData: List<Float>
) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(6.dp, CARD_SHAPE),
        shape = CARD_SHAPE,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(icon = Icons.Outlined.Add, title = "Attachments")
            ImageUploadCard(hasImage = formState.imageUri != null, onClick = onImageClick)

            formState.imageUri?.let {
                Card(modifier = Modifier.fillMaxWidth().height(180.dp), shape = FIELD_SHAPE) {
                    AsyncImage(
                        model = it,
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            VoiceNoteSection(
                isRecording = formState.isRecording,
                hasRecording = formState.recordedAudioUri != null,
                onMicClick = onMicClick,
                waveformData = waveformData
            )
        }
    }
}

@Composable
private fun FormField(icon: ImageVector, label: String, content: @Composable () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(text = label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ImageUploadCard(hasImage: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = FIELD_SHAPE,
        colors = CardDefaults.cardColors(
            containerColor = if (hasImage) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            2.dp,
            if (hasImage) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (hasImage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = if (hasImage) "Photo Selected" else "Add Photo",
                style = MaterialTheme.typography.bodyLarge,
                color = if (hasImage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (hasImage) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun VoiceNoteSection(
    isRecording: Boolean,
    hasRecording: Boolean,
    onMicClick: () -> Unit,
    waveformData: List<Float>
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                painter = painterResource(R.drawable.mic_svgrepo_com),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(text = "Voice Note", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        VoiceNoteBar(
            isRecording = isRecording, onMicClick = onMicClick,
            audioWaveformData = waveformData,
        )
    }
}

@Composable
private fun SubmitButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, 
        enabled = enabled, 
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text("Submit Report")
    }
}

@SuppressLint("MissingPermission")
private fun getCurrentLocationOptimized(fusedClient: FusedLocationProviderClient, onLocation: (Location) -> Unit) {
    fusedClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            onLocation(location)
        } else {
            val request = LocationRequest.create().apply {
                priority = Priority.PRIORITY_HIGH_ACCURACY
                interval = 10000
                fastestInterval = 5000
                numUpdates = 1
            }
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.locations.firstOrNull()?.let(onLocation)
                    fusedClient.removeLocationUpdates(this)
                }
            }
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        }
    }
}

private fun getAddressFromLocationOptimized(context: Context, location: Location): String {
    val geocoder = Geocoder(context, Locale.getDefault())
    return try {
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        addresses?.firstOrNull()?.getAddressLine(0) ?: "Lat: ${location.latitude}, Lng: ${location.longitude}"
    } catch (e: IOException) {
        "Location not found"
    }
}
