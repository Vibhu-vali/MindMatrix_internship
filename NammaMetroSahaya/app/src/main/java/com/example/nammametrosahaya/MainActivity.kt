package com.example.nammametrosahaya

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.nammametrosahaya.data.DataLoader
import com.example.nammametrosahaya.data.RecentSearchManager
import com.example.nammametrosahaya.logic.FareEngine
import com.example.nammametrosahaya.logic.LocationEngine
import com.example.nammametrosahaya.logic.RouteEngine
import com.google.android.gms.location.LocationServices

class MainActivity : BaseActivity() {

    private val languages = listOf(
        Pair("English", "en"),
        Pair("ಕನ್ನಡ",   "kn"),
        Pair("हिंदी",   "hi")
    )

    private var spinnerReady    = false
    private lateinit var etFrom: AutoCompleteTextView
    private lateinit var etTo:   AutoCompleteTextView
    private lateinit var layoutRecent:    LinearLayout
    private lateinit var containerRecent: LinearLayout

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val network      = DataLoader.loadNetwork(this)
        val stationNames = network.stations.map { it.name }.distinct().sorted()

        etFrom          = findViewById(R.id.etFrom)
        etTo            = findViewById(R.id.etTo)
        val btnFind     = findViewById<Button>(R.id.btnFindRoute)
        val btnMap      = findViewById<Button>(R.id.btnMetroMap)
        val spinnerLang = findViewById<Spinner>(R.id.spinnerLanguage)
        val btnLocation = findViewById<TextView>(R.id.btnUseLocation)
        layoutRecent    = findViewById(R.id.layoutRecent)
        containerRecent = findViewById(R.id.containerRecent)
        val tvClear     = findViewById<TextView>(R.id.tvClearRecent)

        // ── Language Spinner ──────────────────────────────────────────────
        val labels = languages.map { it.first }
        val spinnerAdapter = object : ArrayAdapter<String>(
            this, R.layout.item_language_spinner, R.id.text1, labels
        ) {
            override fun getDropDownView(
                position: Int, convertView: View?, parent: ViewGroup
            ): View {
                val view = layoutInflater.inflate(
                    R.layout.item_language_dropdown, parent, false
                ) as TextView
                view.text = labels[position]
                return view
            }
        }
        spinnerLang.adapter = spinnerAdapter

        val currentLang  = LanguageManager.getSavedLanguage(this)
        val currentIndex = languages.indexOfFirst { it.second == currentLang }
        if (currentIndex >= 0) spinnerLang.setSelection(currentIndex)

        spinnerLang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                if (!spinnerReady) { spinnerReady = true; return }
                val selected = languages[position].second
                if (selected != LanguageManager.getSavedLanguage(this@MainActivity)) {
                    LanguageManager.saveLanguage(this@MainActivity, selected)
                    val intent = this@MainActivity.intent
                    finish()
                    startActivity(intent)
                    overridePendingTransition(
                        android.R.anim.fade_in, android.R.anim.fade_out
                    )
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // ── Autocomplete ──────────────────────────────────────────────────
        val adapter = ArrayAdapter(
            this, android.R.layout.simple_dropdown_item_1line, stationNames
        )
        etFrom.setAdapter(adapter)
        etTo.setAdapter(adapter)

        // ── GPS Nearby Station ────────────────────────────────────────────
        btnLocation.setOnClickListener {
            if (LocationEngine.hasLocationPermission(this)) {
                findNearestStation()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST
                )
            }
        }

        // ── Recent Searches ───────────────────────────────────────────────
        loadRecentSearches()

        tvClear.setOnClickListener {
            RecentSearchManager.clearSearches(this)
            layoutRecent.visibility = View.GONE
        }

        // ── Find Route ────────────────────────────────────────────────────
        btnFind.setOnClickListener {
            val from = etFrom.text.toString().trim()
            val to   = etTo.text.toString().trim()

            when {
                from.isEmpty() -> Toast.makeText(this,
                    getString(R.string.error_enter_start), Toast.LENGTH_SHORT).show()
                to.isEmpty()   -> Toast.makeText(this,
                    getString(R.string.error_enter_dest),  Toast.LENGTH_SHORT).show()
                from == to     -> Toast.makeText(this,
                    getString(R.string.error_same_station), Toast.LENGTH_SHORT).show()
                else -> {
                    val result = RouteEngine.findRoute(from, to, network)
                    if (result == null) {
                        Toast.makeText(this,
                            getString(R.string.error_no_route), Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                    RecentSearchManager.saveSearch(this, from, to)
                    val intent = Intent(this, RouteResultActivity::class.java)
                    intent.putExtra("FROM",        from)
                    intent.putExtra("TO",          to)
                    intent.putExtra("FARE",        FareEngine.formatFare(result.fare))
                    intent.putExtra("TIME",        FareEngine.formatTime(result.travelTimeMinutes))
                    intent.putExtra("INTERCHANGE", result.needsInterchange)
                    intent.putStringArrayListExtra(
                        "STATIONS", ArrayList(result.stations.map { it.name })
                    )
                    startActivity(intent)
                }
            }
        }

        // ── Metro Map ─────────────────────────────────────────────────────
        btnMap.setOnClickListener {
            startActivity(Intent(this, MetroMapActivity::class.java))
        }
    }

    @SuppressLint("MissingPermission")
    private fun findNearestStation() {
        val network  = DataLoader.loadNetwork(this)
        val client   = LocationServices.getFusedLocationProviderClient(this)
        client.lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                Toast.makeText(this,
                    "Could not get location. Try again.",
                    Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            val nearest = LocationEngine.findNearestStation(
                location.latitude, location.longitude, network.stations
            )
            if (nearest != null) {
                etFrom.setText(nearest.name)
                val distance = LocationEngine.formatDistance(
                    location.latitude, location.longitude, nearest
                )
                Toast.makeText(this,
                    "Nearest station: ${nearest.name} ($distance)",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            findNearestStation()
        } else {
            Toast.makeText(this,
                "Location permission needed to find nearest station",
                Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadRecentSearches()
    }

    private fun loadRecentSearches() {
        val searches = RecentSearchManager.getSearches(this)
        if (searches.isEmpty()) { layoutRecent.visibility = View.GONE; return }

        layoutRecent.visibility = View.VISIBLE
        containerRecent.removeAllViews()

        searches.forEach { search ->
            val row = LayoutInflater.from(this)
                .inflate(R.layout.item_recent_search, containerRecent, false)
            row.findViewById<TextView>(R.id.tvRecentRoute).text =
                "${search.from}  →  ${search.to}"
            row.setOnClickListener {
                etFrom.setText(search.from)
                etTo.setText(search.to)
                etFrom.dismissDropDown()
                etTo.dismissDropDown()
            }
            containerRecent.addView(row)
            if (searches.indexOf(search) < searches.size - 1) {
                val divider = View(this)
                divider.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                )
                divider.setBackgroundColor(0xFFF5F5F5.toInt())
                containerRecent.addView(divider)
            }
        }
    }
}