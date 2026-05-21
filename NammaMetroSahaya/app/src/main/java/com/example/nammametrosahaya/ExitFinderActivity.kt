package com.example.nammametrosahaya

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class ExitFinderActivity : BaseActivity() {

    // Data class for a single exit
    data class ExitInfo(
        val gate: String,
        val landmark: String,
        val direction: String
    )

    // Data class for a station and its exits
    data class StationExits(
        val station: String,
        val exits: List<ExitInfo>
    )

    private lateinit var allStationExits: List<StationExits>
    private lateinit var exitAdapter: ExitAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exit_finder)

        // Get the station passed from Route Result screen
        val preSelectedStation = intent.getStringExtra("STATION") ?: ""
        findViewById<TextView>(R.id.tvExitStation).text =
            if (preSelectedStation.isNotEmpty()) preSelectedStation
            else "Select a station below"

        // Load exit data from assets/exits.json
        allStationExits = loadExitsFromJson()

        // Set up station spinner
        val stationNames = allStationExits.map { it.station }
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            stationNames
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val spinner = findViewById<Spinner>(R.id.spinnerStation)
        spinner.adapter = spinnerAdapter

        // Pre-select the station passed from Route Result if it exists
        val preSelectIndex = stationNames.indexOf(preSelectedStation)
        if (preSelectIndex >= 0) spinner.setSelection(preSelectIndex)

        // Set up RecyclerView
        val rv = findViewById<RecyclerView>(R.id.rvExits)
        rv.layoutManager = LinearLayoutManager(this)
        exitAdapter = ExitAdapter(emptyList())
        rv.adapter = exitAdapter

        // Show exits when station is selected
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                val selected = allStationExits[position]
                findViewById<TextView>(R.id.tvExitStation).text = selected.station
                exitAdapter.updateData(selected.exits)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // Read exits.json from assets folder
    private fun loadExitsFromJson(): List<StationExits> {
        val result = mutableListOf<StationExits>()
        try {
            val jsonString = assets.open("exits.json")
                .bufferedReader()
                .use { it.readText() }

            val root      = JSONObject(jsonString)
            val exitsArray = root.getJSONArray("exits")

            for (i in 0 until exitsArray.length()) {
                val stationObj  = exitsArray.getJSONObject(i)
                val stationName = stationObj.getString("station")
                val exitsArr    = stationObj.getJSONArray("exits")

                val exitList = mutableListOf<ExitInfo>()
                for (j in 0 until exitsArr.length()) {
                    val exitObj = exitsArr.getJSONObject(j)
                    exitList.add(
                        ExitInfo(
                            gate      = exitObj.getString("gate"),
                            landmark  = exitObj.getString("landmark"),
                            direction = exitObj.getString("direction")
                        )
                    )
                }
                result.add(StationExits(stationName, exitList))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    inner class ExitAdapter(private var items: List<ExitInfo>) :
        RecyclerView.Adapter<ExitAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val gate      = view.findViewById<TextView>(R.id.tvGateBadge)
            val landmark  = view.findViewById<TextView>(R.id.tvLandmark)
            val direction = view.findViewById<TextView>(R.id.tvDirection)
        }

        fun updateData(newItems: List<ExitInfo>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_exit, parent, false)
            return VH(view)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val exit = items[position]
            // Show short gate label e.g. "E1" instead of "Exit 1"
            holder.gate.text      = exit.gate.replace("Exit ", "E")
            holder.landmark.text  = exit.landmark
            holder.direction.text = exit.direction
        }
    }
}