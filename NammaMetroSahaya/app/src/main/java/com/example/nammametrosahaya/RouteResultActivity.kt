package com.example.nammametrosahaya

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nammametrosahaya.data.DataLoader
import com.example.nammametrosahaya.data.Station
import com.example.nammametrosahaya.logic.ArrivalNotificationManager
import com.example.nammametrosahaya.logic.TrainTimingEngine

class RouteResultActivity : BaseActivity() {

    private var tokenFare    = 0
    private var smartCardFare = 0
    private var isTokenMode  = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_result)

        val from             = intent.getStringExtra("FROM")             ?: ""
        val to               = intent.getStringExtra("TO")               ?: ""
        val fareStr          = intent.getStringExtra("FARE")             ?: "₹0"
        val time             = intent.getStringExtra("TIME")             ?: "0 min"
        val needsInterchange = intent.getBooleanExtra("INTERCHANGE", false)
        val stationNames     = intent.getStringArrayListExtra("STATIONS") ?: arrayListOf()

        // Parse fare number from string e.g. "₹20" → 20
        tokenFare     = fareStr.replace("₹", "").trim().toIntOrNull() ?: 0
        smartCardFare = (tokenFare * 0.9).toInt() // 10% Smart Card discount

        // Fill header
        findViewById<TextView>(R.id.tvFromTo).text = "$from  →  $to"
        updateFareDisplay()
        findViewById<TextView>(R.id.tvTime).text   = time

        // Interchange warning
        if (needsInterchange) {
            findViewById<View>(R.id.cardInterchange).visibility = View.VISIBLE
        }

        // Train timings — detect line from first station
        val network       = DataLoader.loadNetwork(this)
        val firstStation  = network.stations.find { it.name == from }
        val line          = firstStation?.line ?: "purple"
        val timing        = TrainTimingEngine.getTimingForLine(this, line)
        val tvTiming      = findViewById<TextView>(R.id.tvTrainTiming)
        val tvInterval    = findViewById<TextView>(R.id.tvTrainInterval)
        tvTiming.text     = TrainTimingEngine.formatNextTrain(timing)
        tvInterval.text   = TrainTimingEngine.formatInterval(timing)

        // Station list
        val stations = stationNames.mapIndexed { index, name ->
            Station(
                id            = index,
                name          = name,
                line          = if (index < stationNames.size / 2) "purple" else "green",
                order         = index,
                isInterchange = name == "Majestic",
                lat           = 0.0,
                lng           = 0.0,
                exits         = emptyList()
            )
        }
        val rv = findViewById<RecyclerView>(R.id.rvStations)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter        = StationAdapter(stations)

        // Token toggle
        findViewById<TextView>(R.id.btnToken).setOnClickListener {
            isTokenMode = true
            updateFareDisplay()
            findViewById<TextView>(R.id.tvSavingsHint).visibility = View.GONE
        }

        // Smart Card toggle
        findViewById<TextView>(R.id.btnSmartCard).setOnClickListener {
            isTokenMode = false
            updateFareDisplay()
            val saving = tokenFare - smartCardFare
            val hint   = findViewById<TextView>(R.id.tvSavingsHint)
            hint.text       = "You save ₹$saving with Smart Card!"
            hint.visibility = View.VISIBLE
        }

        // Visual Guide
        findViewById<Button>(R.id.btnStartGuide).setOnClickListener {
            val intent = Intent(this, VisualGuideActivity::class.java)
            intent.putExtra("FROM",        from)
            intent.putExtra("TO",          to)
            intent.putExtra("INTERCHANGE", needsInterchange)
            startActivity(intent)
        }

        // Exit Finder
        findViewById<Button>(R.id.btnFindExit).setOnClickListener {
            val intent = Intent(this, ExitFinderActivity::class.java)
            intent.putExtra("STATION", to)
            startActivity(intent)
        }

        // Offline Map — shows route highlighted
        findViewById<Button>(R.id.btnOfflineMap).setOnClickListener {
            val intent = Intent(this, OfflineMapActivity::class.java)
            intent.putStringArrayListExtra("ROUTE_STATIONS", stationNames)
            startActivity(intent)
        }

        // Share journey via WhatsApp
        findViewById<Button>(R.id.btnShare).setOnClickListener {
            val fare    = if (isTokenMode) "₹$tokenFare" else "₹$smartCardFare"
            val message = "I'm travelling from $from to $to via Namma Metro.\n" +
                    "Fare: $fare · Time: $time\n" +
                    "Next train: ${TrainTimingEngine.formatNextTrain(timing)}\n" +
                    "Tracked with Namma Metro Sahaya app 🚇"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type    = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage("com.whatsapp")
            }

            if (shareIntent.resolveActivity(packageManager) != null) {
                startActivity(shareIntent)
            } else {
                // Fallback to general share if WhatsApp not installed
                val fallback = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                }
                startActivity(Intent.createChooser(fallback, "Share journey via"))
            }
        }

        // Arrival notification
        findViewById<Button>(R.id.btnNotifyArrival).setOnClickListener {
            val destStation = network.stations.find { it.name == to }
            if (destStation == null) {
                Toast.makeText(this, "Station not found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    101
                )
                return@setOnClickListener
            }

            ArrivalNotificationManager.registerArrivalAlert(this, destStation)
            Toast.makeText(
                this,
                "You'll be notified when approaching $to",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateFareDisplay() {
        val fare = if (isTokenMode) "₹$tokenFare" else "₹$smartCardFare"
        findViewById<TextView>(R.id.tvFare).text = fare
    }

    inner class StationAdapter(private val items: List<Station>) :
        RecyclerView.Adapter<StationAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val dot   = view.findViewById<View>(R.id.viewLineDot)
            val name  = view.findViewById<TextView>(R.id.tvStationName)
            val badge = view.findViewById<TextView>(R.id.tvInterchangeBadge)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_station, parent, false)
            return VH(view)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val station = items[position]
            holder.name.text = station.name
            val color = if (station.line == "purple") 0xFF7B2D8B.toInt()
            else 0xFF2E7D32.toInt()
            holder.dot.background.setTint(color)
            holder.badge.visibility =
                if (station.isInterchange) View.VISIBLE else View.GONE
        }
    }
}