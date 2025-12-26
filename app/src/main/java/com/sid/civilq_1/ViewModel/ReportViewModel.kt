package com.sid.civilq_1.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sid.civilq_1.model.Report
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportViewModel : ViewModel() {

    // 1. User Profile State (For "Hello $name")
    private val _userName = MutableStateFlow("Citizen")
    val userName: StateFlow<String> = _userName.asStateFlow()

    // 2. Reports State
    private val _reports = MutableStateFlow(initialReportData)
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    // 3. Audio Recording State
    private val _recordedAudioUri = MutableStateFlow<Uri?>(null)
    val recordedAudioUri: StateFlow<Uri?> = _recordedAudioUri.asStateFlow()

    init {
        fetchUserProfile()
    }

    /**
     * Fetches the display name from Firebase Auth.
     * To reduce overloading, this is called once during initialization.
     */
    private fun fetchUserProfile() {
        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            user?.let {
                // If displayName is null, it defaults to "Citizen"
                _userName.value = it.displayName ?: "Citizen"
            }
        }
    }

    /**
     * Updates upvotes efficiently using copy() to trigger recomposition only for the specific item.
     */
    fun upvoteReport(id: Int) {
        _reports.value = _reports.value.map { report ->
            if (report.id == id) report.copy(upvotes = report.upvotes + 1)
            else report
        }
    }

    fun addReport(report: Report) {
        // Appends new report to the existing list
        _reports.value = (listOf(report) + _reports.value)
    }

    fun setRecordedAudio(uri: Uri) {
        _recordedAudioUri.value = uri
    }

    fun clearRecordedAudio() {
        _recordedAudioUri.value = null
    }
}

/**
 * Static Data moved outside the class to keep the ViewModel clean
 * and prevent memory overhead during object initialization.
 */
private val initialReportData = listOf(
    Report(
        id = 21,
        title = "Garbage Uncollected for 10 Days in Vepery",
        status = "Active",
        category = "Sanitation",
        upvotes = 15,
        description = "Residents of Arian Lane in Vepery have reported that garbage has remained uncollected for over 10 days...",
        location = "Vepery, Chennai",
        latitude = 13.0853,
        longitude = 80.2591,
        imageUrl = "https://images.hindustantimes.com/rf/image_size_640x362/HT/p2/2016/06/17/Pictures/ghaziabad-wednesday-kaushambi-ghaziabad-hindustan-garbage-dumps_25ada0cc-3462-11e6-b762-306eb096a216.jpg",
        audioUrl = "https://example.com/vepery_garbage_audio.mp3",
        timestamp = "4:45 PM, 20th June 2024",
        departmentHeadName = "Mr. R. Sharma",
        workerName = "Suresh Kumar",
        workerPhone = "9876543210"
    ),
    Report(
        id = 30,
        title = "Unauthorized Construction in Riverside Area",
        status = "Active",
        category = "Urban Planning",
        upvotes = 3,
        description = "Citizens report unauthorized construction activity near the riverside...",
        location = "Riverside Area, Pune",
        latitude = 18.52043,
        longitude = 73.85674,
        imageUrl = "https://5.imimg.com/data5/HL/DF/GO/ANDROID-109871693/product-jpeg-500x500.jpeg",
        audioUrl = "https://example.com/riverside_construction_audio.mp3",
        timestamp = "2025-09-18T09:45:00Z",
        departmentHeadName = "Ms. Anita Desai",
        workerName = "Rohan Patil",
        workerPhone = "9123456780"
    ),
    Report(
        id = 22,
        title = "Potholes on 5th Main Road, Anna Nagar",
        status = "Active",
        category = "Road Maintenance",
        upvotes = 30,
        description = "Several large potholes have developed on 5th Main Road in Anna Nagar...",
        location = "Anna Nagar, Chennai",
        latitude = 13.081736,
        longitude = 80.211362,
        imageUrl = "https://th-i.thgim.com/public/incoming/b61z12/article67480497.ece/alternates/FREE_1200/Anna%20Nagar%205th%20Avenue1.jpg",
        audioUrl = "https://example.com/anna_nagar_potholes_audio.mp3",
        timestamp = "10:15 AM, 18th June 2024",
        departmentHeadName = "Mr. S. Ramesh",
        workerName = "Manoj",
        workerPhone = "9876541230"
    ),
    Report(

        id = 29,
        title = "Waterlogging Near Central Park",
        status = "Solved",
        category = "Roads",
        upvotes = 15,
        description = "Heavy rains have caused waterlogging near Central Park, making it difficult for vehicles and pedestrians to pass.",
        location = "Central Park, Jaipur",
        latitude = 26.904779,
        longitude = 75.810177,
        imageUrl = "https://images.hindustantimes.com/img/2022/07/06/1600x900/69064976-fd6c-11ec-8171-8e816335ea07_1657140365046.jpg",
        audioUrl = "https://example.com/central_park_waterlogging_audio.mp3",
        timestamp = "2025-09-18T08:00:00Z",
        departmentHeadName = "Mr. Ajay Singh",
        workerName = "Vikram Mehta",
        workerPhone = "9988776655"

    ),
    Report(

        id = 27,
        title = "Overflowing Garbage Bins on MG Road",
        status = "Solved",
        category = "Sanitation",
        upvotes = 8,
        description = "Residents complain that garbage bins along MG Road have not been emptied for over a week, causing foul smell and unhygienic conditions.",
        location = "MG Road, Bangalore",
        latitude = 12.974831,
        longitude = 77.60935,
        imageUrl = "https://static.toiimg.com/thumb/resizemode-4,width-1280,height-720,msid-121710513/121710513.jpg",
        audioUrl = "https://example.com/mg_road_garbage_audio.mp3",
        timestamp = "2025-09-18T06:30:00Z",
        departmentHeadName = "Ms. Priya Reddy",
        workerName = "Karthik",
        workerPhone = "9871234560"

    ),

)