package com.example.nammametrosahaya

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.nammametrosahaya.data.Station

class OfflineMapActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_map)

        val routeStations = intent.getStringArrayListExtra("ROUTE_STATIONS")
        val canvasView    = findViewById<MetroCanvasView>(R.id.metroCanvasView)
        val card          = findViewById<CardView>(R.id.cardOfflineStation)
        val tvName        = findViewById<TextView>(R.id.tvOfflineStationName)
        val tvLine        = findViewById<TextView>(R.id.tvOfflineStationLine)
        val tvExits       = findViewById<TextView>(R.id.tvOfflineExits)

        // Highlight route stations if coming from Route Result
        if (!routeStations.isNullOrEmpty()) {
            canvasView.highlightedStations = routeStations
        }

        canvasView.onStationClickListener = object : MetroCanvasView.OnStationClickListener {
            override fun onStationClicked(station: Station) {
                tvName.text = station.name
                tvLine.text = if (station.isInterchange)
                    "Interchange — Purple & Green Line"
                else if (station.line == "purple")
                    "Purple Line (East–West)"
                else
                    "Green Line (North–South)"

                tvLine.setTextColor(
                    if (station.line == "purple") 0xFF7B2D8B.toInt()
                    else 0xFF2E7D32.toInt()
                )

                tvExits.text = station.exits.joinToString("\n") { exit ->
                    "${exit.gate}: ${exit.landmark} — ${exit.direction}"
                }

                card.visibility = View.VISIBLE
            }
        }

        // Tap map background to hide card
        canvasView.setOnClickListener { card.visibility = View.GONE }
    }
}