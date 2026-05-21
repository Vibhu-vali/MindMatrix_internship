package com.example.nammametrosahaya.logic

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.nammametrosahaya.GeofenceBroadcastReceiver
import com.example.nammametrosahaya.data.Station
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object ArrivalNotificationManager {

    private const val GEOFENCE_RADIUS_METERS = 500f

    @SuppressLint("MissingPermission")
    fun registerArrivalAlert(context: Context, station: Station) {
        val client = LocationServices.getGeofencingClient(context)

        val geofence = Geofence.Builder()
            .setRequestId(station.name)
            .setCircularRegion(station.lat, station.lng, GEOFENCE_RADIUS_METERS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent        = Intent(context, GeofenceBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        client.addGeofences(request, pendingIntent)
    }

    fun clearAllAlerts(context: Context) {
        val client = LocationServices.getGeofencingClient(context)
        client.removeGeofences(
            PendingIntent.getBroadcast(
                context, 0,
                Intent(context, GeofenceBroadcastReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        )
    }
}