package com.example.nammametrosahaya

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.nammametrosahaya.data.DataLoader
import com.example.nammametrosahaya.data.Station
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class MetroMapActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private var selectedStation: Station? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metro_map)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Load all station data from JSON
        val network  = DataLoader.loadNetwork(this)
        val stations = network.stations

        // Draw purple line polyline
        val purpleStations = stations
            .filter { it.line == "purple" }
            .sortedBy { it.order }

        val purpleOptions = PolylineOptions()
            .width(8f)
            .color(0xFF7B2D8B.toInt())
        purpleStations.forEach { purpleOptions.add(LatLng(it.lat, it.lng)) }
        map.addPolyline(purpleOptions)

        // Draw green line polyline
        val greenStations = stations
            .filter { it.line == "green" }
            .sortedBy { it.order }

        val greenOptions = PolylineOptions()
            .width(8f)
            .color(0xFF2E7D32.toInt())
        greenStations.forEach { greenOptions.add(LatLng(it.lat, it.lng)) }
        map.addPolyline(greenOptions)

        // Add markers for every station
        stations.forEach { station ->
            val pos    = LatLng(station.lat, station.lng)
            val color  = if (station.line == "purple")
                BitmapDescriptorFactory.HUE_VIOLET
            else
                BitmapDescriptorFactory.HUE_GREEN

            // Interchange gets a star marker
            val markerIcon = if (station.isInterchange)
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
            else
                BitmapDescriptorFactory.defaultMarker(color)

            val marker = map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(station.name)
                    .icon(markerIcon)
            )
            marker?.tag = station.id
        }

        // Tap a marker → show station info card
        map.setOnMarkerClickListener { marker ->
            val stationId = marker.tag as? Int
            val station   = stations.find { it.id == stationId }
            station?.let { showStationInfo(it) }
            true
        }

        // Tap map → hide card
        map.setOnMapClickListener {
            findViewById<CardView>(R.id.cardStationInfo).visibility = View.GONE
            selectedStation = null
        }

        // Move camera to fit all stations when map is fully loaded (avoids size=0 crash)
        map.setOnMapLoadedCallback {
            if (stations.isNotEmpty()) {
                val boundsBuilder = LatLngBounds.Builder()
                stations.forEach { boundsBuilder.include(LatLng(it.lat, it.lng)) }
                val bounds = boundsBuilder.build()
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80))
            }
        }
    }

    private fun showStationInfo(station: Station) {
        selectedStation = station
        val card = findViewById<CardView>(R.id.cardStationInfo)

        // Station name
        findViewById<TextView>(R.id.tvSelectedStation).text = station.name

        // Line label
        val lineLabel = if (station.isInterchange)
            "Interchange — Purple & Green Line"
        else if (station.line == "purple")
            "Purple Line (East–West)"
        else
            "Green Line (North–South)"

        val lineColor = if (station.line == "purple") 0xFF7B2D8B.toInt() else 0xFF2E7D32.toInt()
        val tv = findViewById<TextView>(R.id.tvSelectedLine)
        tv.text      = lineLabel
        tv.setTextColor(lineColor)

        // Exit summary
        val exitSummary = station.exits.joinToString("\n") { exit ->
            "${exit.gate}: ${exit.landmark} — ${exit.direction}"
        }
        findViewById<TextView>(R.id.tvSelectedExits).text = exitSummary

        // Open in Google Maps button — uses deep link, no API needed
        findViewById<Button>(R.id.btnOpenMaps).setOnClickListener {
            val uri    = Uri.parse(
                "geo:${station.lat},${station.lng}?q=${station.lat},${station.lng}(${station.name}+Metro+Station)"
            )
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback — open in browser if Maps app not installed
                val browserUri = Uri.parse(
                    "https://www.google.com/maps/search/?api=1&query=${station.lat},${station.lng}"
                )
                startActivity(Intent(Intent.ACTION_VIEW, browserUri))
            }
        }

        card.visibility = View.VISIBLE
    }
}