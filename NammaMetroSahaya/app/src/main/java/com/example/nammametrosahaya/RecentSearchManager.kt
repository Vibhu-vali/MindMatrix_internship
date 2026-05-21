package com.example.nammametrosahaya.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object RecentSearchManager {

    private const val PREF_NAME       = "MetroPrefs"
    private const val KEY_RECENT      = "recent_searches"
    private const val MAX_RECENT      = 5

    data class RecentSearch(
        val from: String,
        val to: String
    )

    // Save a new search to the top of the list
    fun saveSearch(context: Context, from: String, to: String) {
        val current = getSearches(context).toMutableList()

        // Remove duplicate if it already exists
        current.removeAll { it.from == from && it.to == to }

        // Add new search to top
        current.add(0, RecentSearch(from, to))

        // Keep only last MAX_RECENT searches
        val trimmed = current.take(MAX_RECENT)

        // Save back to SharedPreferences as JSON
        val jsonArray = JSONArray()
        trimmed.forEach { search ->
            val obj = JSONObject()
            obj.put("from", search.from)
            obj.put("to",   search.to)
            jsonArray.put(obj)
        }

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECENT, jsonArray.toString())
            .apply()
    }

    // Load all saved searches
    fun getSearches(context: Context): List<RecentSearch> {
        val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RECENT, null) ?: return emptyList()

        return try {
            val array  = JSONArray(json)
            val result = mutableListOf<RecentSearch>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    RecentSearch(
                        from = obj.getString("from"),
                        to   = obj.getString("to")
                    )
                )
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Clear all recent searches
    fun clearSearches(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_RECENT)
            .apply()
    }
}