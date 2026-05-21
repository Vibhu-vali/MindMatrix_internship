package com.example.nammametrosahaya.logic

import android.content.Context
import org.json.JSONObject
import java.util.Calendar

object TrainTimingEngine {

    data class TimingInfo(
        val nextTrainMinutes: Int,
        val intervalMinutes: Int,
        val isPeak: Boolean,
        val isOperating: Boolean
    )

    fun getTimingForLine(context: Context, line: String): TimingInfo {
        val json   = context.assets.open("timetable.json")
            .bufferedReader().use { it.readText() }
        val root   = JSONObject(json)
        val lineKey = if (line == "purple") "purple_line" else "green_line"
        val lineObj = root.getJSONObject(lineKey)

        val now          = Calendar.getInstance()
        val currentMins  = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val firstTrain   = parseTime(lineObj.getString("first_train"))
        val lastTrain    = parseTime(lineObj.getString("last_train"))
        val peakInterval = lineObj.getInt("peak_interval_minutes")
        val offInterval  = lineObj.getInt("offpeak_interval_minutes")

        // Check if metro is operating
        if (currentMins < firstTrain || currentMins > lastTrain) {
            return TimingInfo(0, offInterval, false, false)
        }

        // Check if peak hours
        val peakHours  = lineObj.getJSONArray("peak_hours")
        var isPeak     = false
        for (i in 0 until peakHours.length()) {
            val ph    = peakHours.getJSONObject(i)
            val start = parseTime(ph.getString("start"))
            val end   = parseTime(ph.getString("end"))
            if (currentMins in start..end) { isPeak = true; break }
        }

        val interval      = if (isPeak) peakInterval else offInterval
        val minsSinceFirst = currentMins - firstTrain
        val nextTrainMins  = interval - (minsSinceFirst % interval)

        return TimingInfo(
            nextTrainMinutes = nextTrainMins,
            intervalMinutes  = interval,
            isPeak           = isPeak,
            isOperating      = true
        )
    }

    private fun parseTime(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    fun formatNextTrain(info: TimingInfo): String {
        return when {
            !info.isOperating -> "Metro not operating"
            info.nextTrainMinutes <= 1 -> "Train arriving now"
            else -> "Next train in ${info.nextTrainMinutes} min"
        }
    }

    fun formatInterval(info: TimingInfo): String {
        return if (info.isOperating)
            "Every ${info.intervalMinutes} min · ${if (info.isPeak) "Peak hours" else "Off-peak"}"
        else
            "Opens at 05:30"
    }
}