package com.example.nammametrosahaya

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class VisualGuideActivity : BaseActivity() {

    // Each step has a title, description and a tip
    data class GuideStep(
        val title: String,
        val description: String,
        val tip: String
    )

    private var currentStep = 0
    private lateinit var steps: List<GuideStep>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visual_guide)

        val from = intent.getStringExtra("FROM") ?: "Start"
        val to   = intent.getStringExtra("TO")   ?: "Destination"
        val needsInterchange = intent.getBooleanExtra("INTERCHANGE", false)

        findViewById<TextView>(R.id.tvGuideRoute).text = "$from  →  $to"

        // Build steps based on whether interchange is needed
        steps = buildSteps(from, to, needsInterchange)

        // Wire up buttons
        findViewById<Button>(R.id.btnNextStep).setOnClickListener {
            if (currentStep < steps.size - 1) {
                currentStep++
                updateUI()
            } else {
                // Last step — go back to home
                finish()
            }
        }

        findViewById<Button>(R.id.btnPrevStep).setOnClickListener {
            if (currentStep > 0) {
                currentStep--
                updateUI()
            }
        }

        updateUI()
    }

    private fun buildSteps(
        from: String,
        to: String,
        needsInterchange: Boolean
    ): List<GuideStep> {

        val baseSteps = mutableListOf(
            GuideStep(
                title = "Go to $from station",
                description = "Head to the nearest Namma Metro station entrance. Look for the red and white Metro sign above the entrance gate.",
                tip = "If you are unsure of the entrance, ask any Metro staff in a blue uniform — they are always happy to help."
            ),
            GuideStep(
                title = "Buy your token",
                description = "At the token machine, touch the screen. Select your destination: $to. The machine shows the fare. Insert coins or notes. Collect your round token.",
                tip = "The machine accepts ₹10, ₹20, ₹50 notes and coins. Keep your token safe — you need it to exit."
            ),
            GuideStep(
                title = "Pass through the gate",
                description = "Hold your token against the circular sensor on the gate. The gate will open automatically. Walk through and the gate closes behind you.",
                tip = "Do not rush — wait for the person ahead of you to fully pass through before you tap your token."
            ),
            GuideStep(
                title = "Find the correct platform",
                description = "Look at the signs on the ceiling. Follow signs showing your line colour. Purple line goes East–West. Green line goes North–South.",
                tip = "Check the digital display board on the platform — it shows the next train destination and arrival time in Kannada and English."
            )
        )

        // Add interchange step if needed
        if (needsInterchange) {
            baseSteps.add(
                GuideStep(
                    title = "Change lines at Majestic",
                    description = "Get off at Majestic station. Follow the signs that say 'Interchange'. Walk to the other platform. Board the train on the other line towards $to.",
                    tip = "Majestic is a busy station. Hold your belongings close. The interchange walk takes about 3–4 minutes."
                )
            )
        }

        baseSteps.addAll(listOf(
            GuideStep(
                title = "Board the train",
                description = "Stand behind the yellow line on the platform. When the train arrives, let passengers exit first. Then step in and hold the handrail.",
                tip = "Trains arrive every 6–10 minutes during peak hours. If the train is full, wait for the next one — it is safer."
            ),
            GuideStep(
                title = "Watch for your stop",
                description = "Inside the train, a display shows the next station. An announcement plays in Kannada and English. Get ready when you hear $to announced.",
                tip = "Count the stops on your phone — you booked $to. Do not panic if you miss it — just get off at the next stop and take a train back."
            ),
            GuideStep(
                title = "Exit at $to",
                description = "When the doors open at $to, walk to the exit gate. Place your token on the sensor — the gate opens and collects your token automatically.",
                tip = "Follow the Exit signs to reach the street. Each exit leads to a different landmark — check the board near the gate for directions."
            )
        ))

        return baseSteps
    }

    private fun updateUI() {
        val step = steps[currentStep]
        val total = steps.size

        // Update text views
        findViewById<TextView>(R.id.tvStepNumber).text    = (currentStep + 1).toString()
        findViewById<TextView>(R.id.tvStepTitle).text     = step.title
        findViewById<TextView>(R.id.tvStepDesc).text      = step.description
        findViewById<TextView>(R.id.tvStepTip).text       = step.tip
        findViewById<TextView>(R.id.tvStepProgress).text  = "Step ${currentStep + 1} of $total"
        findViewById<TextView>(R.id.tvStepFraction).text  = "${((currentStep + 1) * 100 / total)}% done"

        // Update progress bar
        val progress = findViewById<ProgressBar>(R.id.progressGuide)
        progress.max      = total
        progress.progress = currentStep + 1

        // Update button labels on last step
        val btnNext = findViewById<Button>(R.id.btnNextStep)
        btnNext.text = if (currentStep == total - 1) "Finish ✓" else "Next →"

        // Hide Previous on first step
        findViewById<Button>(R.id.btnPrevStep).visibility =
            if (currentStep == 0) View.INVISIBLE else View.VISIBLE
    }
}