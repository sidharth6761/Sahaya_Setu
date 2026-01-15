package com.sid.civilq_1.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sid.civilq_1.SupabaseModule.kt.SupabaseClient
import com.sid.civilq_1.model.Report
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportViewModel : ViewModel() {

    private val _isUiReady = MutableStateFlow(false)
    val isUiReady: StateFlow<Boolean> = _isUiReady.asStateFlow()

    private val _userName = MutableStateFlow("Citizen")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    private val _recordedAudioUri = MutableStateFlow<Uri?>(null)
    val recordedAudioUri: StateFlow<Uri?> = _recordedAudioUri.asStateFlow()

    init {
        fetchUserProfile()
        fetchReportsFromSupabase()
        triggerUiReady()
    }

    private fun triggerUiReady() {
        viewModelScope.launch {
            delay(400)
            _isUiReady.value = true
        }
    }

    /**
     * Dynamically fetches user profile name from Firebase Auth.
     */
    fun fetchUserProfile() {
        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            user?.let {
                // Fetch the name; fallback to email or "Citizen" if displayName is empty
                val name = it.displayName ?: it.email?.substringBefore("@") ?: "Citizen"
                _userName.value = name
                Log.d("Auth", "Active User: $name (UID: ${it.uid})")
            }
        }
    }

    // --- SUPABASE INTEGRATION ---

    /**
     * Fetches all rows from 'issues' sorted by UPVOTES (Highest first).
     */
    fun fetchReportsFromSupabase() {
        viewModelScope.launch {
            try {
                val result = SupabaseClient.client.from("issues")
                    .select {
                        // LIVE SORTING: Sort by upvotes (Descending), then newest (Descending)
                        order("upvotes", order = Order.DESCENDING)
                        order("created_at", order = Order.DESCENDING)
                    }
                    .decodeList<Report>()

                _reports.value = result
                Log.d("Supabase", "Successfully fetched ${result.size} reports sorted by votes")
            } catch (e: Exception) {
                Log.e("SupabaseError", "Fetch failed: ${e.message}")
            }
        }
    }

    /**
     * Uploads image to Storage, then inserts record with Firebase UID.
     */
    fun addReport(context: Context, report: Report, imageUri: Uri?) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        viewModelScope.launch {
            try {
                var finalImageUrl: String? = null

                // 1. Upload file to Storage bucket
                imageUri?.let { uri ->
                    val fileName = "report_${System.currentTimeMillis()}.jpg"
                    val bucket = SupabaseClient.client.storage.from("report-image")

                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() ?: return@let

                    bucket.upload("reports/$fileName", bytes)
                    finalImageUrl = bucket.publicUrl("reports/$fileName")
                }

                // 2. Prepare final report object
                val reportWithMetaData = report.copy(
                    userId = firebaseUser?.uid,
                    imageUrl = finalImageUrl ?: report.imageUrl
                )

                // 3. Insert into database
                SupabaseClient.client.from("issues").insert(reportWithMetaData)
                Log.d("Supabase", "Report inserted successfully")

                fetchReportsFromSupabase()
            } catch (e: Exception) {
                Log.e("SupabaseError", "Submission failed: ${e.message}")
            }
        }
    }

    /**
     * Updates upvote count with Optimistic UI update for instant feedback.
     */
    fun upvoteReport(reportId: String) {
        viewModelScope.launch {
            try {
                // 1. Local Optimistic Update (Immediate UI change)
                val currentList = _reports.value.toMutableList()
                val index = currentList.indexOfFirst { it.id == reportId }

                if (index != -1) {
                    val report = currentList[index]
                    val updatedUpvotes = (report.upvotes ?: 0) + 1

                    // Update the list locally and re-sort so the item moves up
                    currentList[index] = report.copy(upvotes = updatedUpvotes)
                    _reports.value = currentList.sortedByDescending { it.upvotes }

                    // 2. Database Update (Sync in background)
                    SupabaseClient.client.from("issues").update(
                        mapOf("upvotes" to updatedUpvotes)
                    ) {
                        filter { eq("id", reportId) }
                    }

                    Log.d("Supabase", "Upvote successful and synced for ID: $reportId")
                }
            } catch (e: Exception) {
                Log.e("SupabaseError", "Upvote sync failed: ${e.message}")
                // If DB update fails, fetch fresh data to correct the UI
                fetchReportsFromSupabase()
            }
        }
    }

    // --- AUDIO HANDLING ---

    fun setRecordedAudio(uri: Uri) {
        _recordedAudioUri.value = uri
    }

    fun clearRecordedAudio() {
        _recordedAudioUri.value = null
    }
}