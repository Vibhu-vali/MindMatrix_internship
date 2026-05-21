package com.example.nammametrosahaya

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    private val languages = listOf(
        Pair("English", "en"),
        Pair("ಕನ್ನಡ",   "kn"),
        Pair("हिंदी",   "hi")
    )

    private var spinnerReady = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase))
    }

    override fun onResume() {
        super.onResume()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        setupLanguageSpinner()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    // Call this from any activity after setContentView to wire up the spinner
    fun setupLanguageSpinner() {
        val spinner = findViewById<Spinner>(R.id.spinnerLanguageToolbar) ?: return
        spinnerReady = false

        val labels = languages.map { it.first }

        val adapter = object : ArrayAdapter<String>(
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

        spinner.adapter = adapter

        // Pre-select current language
        val currentLang  = LanguageManager.getSavedLanguage(this)
        val currentIndex = languages.indexOfFirst { it.second == currentLang }
        if (currentIndex >= 0) spinner.setSelection(currentIndex)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                if (!spinnerReady) { spinnerReady = true; return }
                val selected = languages[position].second
                if (selected != LanguageManager.getSavedLanguage(this@BaseActivity)) {
                    LanguageManager.saveLanguage(this@BaseActivity, selected)
                    val intent = this@BaseActivity.intent
                    finish()
                    startActivity(intent)
                    overridePendingTransition(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    )
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
}