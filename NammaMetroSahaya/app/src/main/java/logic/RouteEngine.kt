package com.example.nammametrosahaya.logic

import com.example.nammametrosahaya.data.DataLoader
import com.example.nammametrosahaya.data.MetroNetwork
import com.example.nammametrosahaya.data.RouteResult
import com.example.nammametrosahaya.data.Station

object RouteEngine {

    fun findRoute(
        fromName: String,
        toName: String,
        network: MetroNetwork
    ): RouteResult? {

        val stations = network.stations

        // Find matching stations by name
        val from = stations.find {
            it.name.equals(fromName, ignoreCase = true)
        } ?: return null

        val to = stations.find {
            it.name.equals(toName, ignoreCase = true)
        } ?: return null

        // Same line — direct route
        val route: List<Station> = if (from.line == to.line) {
            buildDirectRoute(from, to, stations)
        } else {
            // Different lines — route via Majestic interchange
            buildInterchangeRoute(from, to, stations)
        }

        val stopCount        = route.size - 1
        val fare             = FareEngine.calculateFare(stopCount, network.fareRules)
        val travelTime       = stopCount * 3 // ~3 min per stop
        val needsInterchange = from.line != to.line

        return RouteResult(
            stations           = route,
            fare               = fare,
            travelTimeMinutes  = travelTime,
            needsInterchange   = needsInterchange
        )
    }

    private fun buildDirectRoute(
        from: Station,
        to: Station,
        all: List<Station>
    ): List<Station> {
        // Get all stations on the same line sorted by order
        val lineStations = all
            .filter { it.line == from.line }
            .sortedBy { it.order }

        val fromIdx = lineStations.indexOfFirst { it.id == from.id }
        val toIdx   = lineStations.indexOfFirst { it.id == to.id }

        return if (fromIdx <= toIdx)
            lineStations.subList(fromIdx, toIdx + 1)
        else
            lineStations.subList(toIdx, fromIdx + 1).reversed()
    }

    private fun buildInterchangeRoute(
        from: Station,
        to: Station,
        all: List<Station>
    ): List<Station> {
        // Find Majestic on from's line and to's line
        val interchangeFrom = all.find {
            it.isInterchange && it.line == from.line
        } ?: return emptyList()

        val interchangeTo = all.find {
            it.isInterchange && it.line == to.line
        } ?: return emptyList()

        // Build: from → Majestic (on from's line)
        val firstLeg  = buildDirectRoute(from, interchangeFrom, all)
        // Build: Majestic → to (on to's line)
        val secondLeg = buildDirectRoute(interchangeTo, to, all)

        // Merge — drop duplicate Majestic station
        return firstLeg + secondLeg.drop(1)
    }
}