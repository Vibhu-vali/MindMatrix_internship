package com.example.nammametrosahaya.logic

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.nammametrosahaya.data.Station
import kotlin.math.*

object LocationEngine {

    // Check if location permission is granted
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Find nearest station to given lat/lng using Haversine formula
    fun findNearestStation(
        userLat: Double,
        userLng: Double,
        stations: List<Station>
    ): Station? {
        return stations.minByOrNull { station ->
            haversineDistance(userLat, userLng, station.lat, station.lng)
        }
    }

    // Haversine formula — calculates distance between two GPS points in km
    private fun haversineDistance(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val r    = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a    = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    // Format distance nicely
    fun formatDistance(
        userLat: Double, userLng: Double, station: Station
    ): String {
        val distKm = haversineDistance(userLat, userLng, station.lat, station.lng)
        return if (distKm < 1.0)
            "${(distKm * 1000).toInt()}m away"
        else
            "${"%.1f".format(distKm)}km away"
    }
}