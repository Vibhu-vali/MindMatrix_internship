package com.example.nammametrosahaya

import android.app.Activity
import android.content.Context
import android.content.Intent
import java.util.Locale

object LanguageManager {

    private const val PREF_NAME    = "MetroPrefs"
    private const val KEY_LANGUAGE = "language"
    const val LANG_ENGLISH         = "en"
    const val LANG_KANNADA         = "kn"
    const val LANG_HINDI           = "hi"

    fun saveLanguage(context: Context, langCode: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, langCode)
            .apply()
    }

    fun getSavedLanguage(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, LANG_ENGLISH) ?: LANG_ENGLISH
    }

    fun applyLanguage(context: Context): Context {
        val lang   = getSavedLanguage(context)
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    // Cycles through EN → KN → HI → EN → ...
    fun toggleAndRestart(activity: Activity) {
        val current = getSavedLanguage(activity)
        val newLang = when (current) {
            LANG_ENGLISH -> LANG_KANNADA
            LANG_KANNADA -> LANG_HINDI
            LANG_HINDI   -> LANG_ENGLISH
            else         -> LANG_ENGLISH
        }
        saveLanguage(activity, newLang)

        // Restart current activity with fade animation
        val intent = activity.intent
        activity.finish()
        activity.startActivity(intent)
        activity.overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    }

    // Returns label for the toggle button based on current language
    fun getToggleLabel(context: Context): String {
        return when (getSavedLanguage(context)) {
            LANG_ENGLISH -> "ಕನ್ನಡ"
            LANG_KANNADA -> "हिंदी"
            LANG_HINDI   -> "English"
            else         -> "ಕನ್ನಡ"
        }
    }
}