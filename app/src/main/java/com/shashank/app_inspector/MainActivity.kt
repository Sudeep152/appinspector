package com.shashank.app_inspector

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.shashank.appinspector.DebugInspector

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        updateStatus()

        findViewById<Button>(R.id.btn_toggle_inspector).setOnClickListener {
            DebugInspector.setEnabled(!DebugInspector.isEnabled())
            updateStatus()
        }

        // Populate the screen with sample views so the inspector has something to inspect
        val container = findViewById<LinearLayout>(R.id.sample_views_container)
        addSampleViews(container)
    }

    private fun updateStatus() {
        val on = DebugInspector.isEnabled()
        findViewById<TextView>(R.id.tv_status).text =
            if (on) "Inspector: ON  (shake or tap FAB)" else "Inspector: OFF"
    }

    private fun addSampleViews(container: LinearLayout) {
        val density = resources.displayMetrics.density

        fun dp(value: Int) = (value * density).toInt()

        val title = TextView(this).apply {
            text = "Sample UI — tap anything with the inspector"
            textSize = 14f
            setTextColor(Color.parseColor("#1565C0"))
            setPadding(0, dp(8), 0, dp(16))
        }
        container.addView(title)

        val colors = listOf("#E3F2FD", "#FCE4EC", "#F3E5F5", "#E8F5E9", "#FFF8E1")
        val labels = listOf("Card View A", "Card View B", "Card View C", "Card View D", "Card View E")

        colors.forEachIndexed { i, hex ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor(hex))
                setPadding(dp(16), dp(12), dp(16), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dp(10) }
            }

            val label = TextView(this).apply {
                text = labels[i]
                textSize = 15f
                setTextColor(Color.parseColor("#212121"))
            }

            val sub = TextView(this).apply {
                text = "Tap this card with the inspector active to see its properties"
                textSize = 12f
                setTextColor(Color.parseColor("#757575"))
                setPadding(0, dp(4), 0, 0)
            }

            card.addView(label)
            card.addView(sub)
            container.addView(card)
        }

        val btn = Button(this).apply {
            text = "Sample Button"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.gravity = Gravity.CENTER_HORIZONTAL; it.topMargin = dp(8) }
        }
        container.addView(btn)
    }
}
