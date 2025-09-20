package com.sid.civilq_1.model


import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Report(
    val id: Int,
    val title: String,
    val status: String,
    val category: String,
    var upvotes: Int = 0,
    val description: String,
    val imageUrl: String,
    val audioUrl : String,
    val location: String,
    val latitude: Double?,  // add this
    val longitude: Double?,
    val timestamp: String,
    val departmentHeadName :String,

    val workerName: String,

    val workerPhone:String

) : Parcelable
