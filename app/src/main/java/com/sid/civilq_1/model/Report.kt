package com.sid.civilq_1.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Report(
    @SerialName("id") // Explicitly map the UUID
    val id: String? = null,

    @SerialName("title")
    val title: String,

    @SerialName("status") // MANDATORY: This fixes your Filtered: 0 issue
    val status: String = "Active",

    @SerialName("department")
    val category: String,

    @SerialName("description")
    val description: String,

    @SerialName("image_url")
    val imageUrl: String? = null,

    @SerialName("location_address")
    val location: String,

    @SerialName("location_lat_long")
    val latLong: String? = null,

    @SerialName("created_at")
    val timestamp: String? = null,

    @SerialName("upvotes") // Explicitly map upvotes
    val upvotes: Int = 0,

    @SerialName("user_id") // Track which user submitted the report
    val userId: String? = null

) : Parcelable