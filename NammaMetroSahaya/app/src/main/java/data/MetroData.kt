package com.example.nammametrosahaya.data

data class Exit(
    val gate: String,
    val landmark: String,
    val direction: String
)

data class Station(
    val id: Int = 0,
    val name: String,
    val line: String,
    val order: Int = 0,
    val isInterchange: Boolean = false,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val exits: List<Exit> = emptyList()
)

data class FareRule(
    val minStops: Int,
    val maxStops: Int,
    val fare: Int
)

data class MetroNetwork(
    val stations: List<Station>,
    val fareRules: List<FareRule>
)

data class RouteResult(
    val stations: List<Station>,
    val fare: Int,
    val travelTimeMinutes: Int,
    val needsInterchange: Boolean
)