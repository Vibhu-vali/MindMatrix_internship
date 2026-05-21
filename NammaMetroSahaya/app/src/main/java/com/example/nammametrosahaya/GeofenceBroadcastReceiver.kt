package com.example.nammametrosahaya

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val stationName = event.triggeringGeofences
                ?.firstOrNull()?.requestId ?: return
            showNotification(context, stationName)
        }
    }

    private fun showNotification(context: Context, stationName: String) {
        val channelId = "metro_arrival"
        val manager   = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        // Create channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Metro Arrival Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifies when approaching your metro station" }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("Approaching your stop!")
            .setContentText("Get ready — $stationName station is coming up next.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
    }
}