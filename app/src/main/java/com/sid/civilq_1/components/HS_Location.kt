package com.sid.civilq_1.components

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import java.util.Locale

@SuppressLint("MissingPermission")
fun getUserLocation(context: Context, onResult: (String, String) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

            val address = addresses?.firstOrNull()
            if (address != null) {
                val mainAddress = address.subLocality ?: address.locality ?: "Unknown area"
                val exactAddress = buildString {
                    append(address.featureName ?: "")
                    if (!address.thoroughfare.isNullOrEmpty()) append(", ${address.thoroughfare}")
                    if (!address.subThoroughfare.isNullOrEmpty()) append(", ${address.subThoroughfare}")
                    /*if (!address.premises.isNullOrEmpty()) append(", ${address.premises}")*/
                }.ifEmpty { address.getAddressLine(0) ?: "Unknown exact address" }

                onResult(mainAddress, exactAddress)
            } else {
                onResult("Unknown area", "Unknown exact address")
            }
        } else {
            onResult("Location not found", "")
        }
    }.addOnFailureListener {
        onResult("Error", "")
    }
}
