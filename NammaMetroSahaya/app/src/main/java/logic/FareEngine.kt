package com.example.nammametrosahaya.logic

import com.example.nammametrosahaya.data.FareRule

object FareEngine {

    // Reads fare purely from JSON rules — nothing hardcoded
    fun calculateFare(stopCount: Int, rules: List<FareRule>): Int {
        val rule = rules.find { stopCount in it.minStops..it.maxStops }
        return rule?.fare ?: rules.last().fare
    }

    fun formatFare(fare: Int): String = "₹$fare"

    fun formatTime(minutes: Int): String = "$minutes min"
}