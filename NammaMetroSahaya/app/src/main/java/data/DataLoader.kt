package com.example.nammametrosahaya.data

import android.content.Context
import org.json.JSONObject

object DataLoader {

    private var cachedNetwork: MetroNetwork? = null

    // Call this once — result is cached so JSON is only read once
    fun loadNetwork(context: Context): MetroNetwork {
        cachedNetwork?.let { return it }

        val jsonString = context.assets
            .open("stations.json")
            .bufferedReader()
            .use { it.readText() }

        val root = JSONObject(jsonString)

        // Parse fare rules
        val fareRulesArray = root.getJSONArray("fareRules")
        val fareRules = (0 until fareRulesArray.length()).map { i ->
            val obj = fareRulesArray.getJSONObject(i)
            FareRule(
                minStops = obj.getInt("minStops"),
                maxStops = obj.getInt("maxStops"),
                fare     = obj.getInt("fare")
            )
        }

        // Parse stations
        val stationsArray = root.getJSONArray("stations")
        val stations = (0 until stationsArray.length()).map { i ->
            val obj  = stationsArray.getJSONObject(i)
            val exitsArray = obj.getJSONArray("exits")
            val exits = (0 until exitsArray.length()).map { j ->
                val e = exitsArray.getJSONObject(j)
                Exit(
                    gate      = e.getString("gate"),
                    landmark  = e.getString("landmark"),
                    direction = e.getString("direction")
                )
            }
            Station(
                id            = obj.getInt("id"),
                name          = obj.getString("name"),
                line          = obj.getString("line"),
                order         = obj.getInt("order"),
                isInterchange = obj.getBoolean("isInterchange"),
                lat           = obj.getDouble("lat"),
                lng           = obj.getDouble("lng"),
                exits         = exits
            )
        }

        val network = MetroNetwork(stations, fareRules)
        cachedNetwork = network
        return network
    }
}