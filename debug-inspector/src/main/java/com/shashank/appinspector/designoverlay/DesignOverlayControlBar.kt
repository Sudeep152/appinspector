package com.shashank.appinspector.designoverlay

import android.app.Activity
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.shashank.appinspector.DebugConfig
import com.shashank.appinspector.R

internal class DesignOverlayControlBar(
    private val activity: Activity,
    private val onOpacityChanged: (Int) -> Unit,
    private val onModeChanged: (CompareMode) -> Unit,
    private val onGridToggle: () -> Unit,
    private val onClose: () -> Unit
) {

    private var controlView: View? = null
    private var isGridActive = false
    private var currentMode = CompareMode.OPACITY
    private val modeButtons = mutableMapOf<CompareMode, TextView>()

    fun attach() {
        try {
            val decorView = activity.window.decorView as? FrameLayout ?: return
            val view = LayoutInflater.from(activity)
                .inflate(R.layout.layout_design_overlay_controls, null)

            view.setTag(R.id.debug_inspector_tag, DebugConfig.INSPECTOR_VIEW_TAG)

            setupModeButtons(view)
            setupOpacitySlider(view)
            setupGridAndClose(view)

            val margin = activity.resources.getDimensionPixelSize(R.dimen.debug_inspector_control_bar_margin)
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            ).apply {
                leftMargin = margin
                rightMargin = margin
                bottomMargin = margin * 3
            }

            decorView.addView(view, params)
            controlView = view
            updateModeButtonStates()
        } catch (_: Exception) { }
    }

    fun detach() {
        controlView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        controlView = null
        modeButtons.clear()
        isGridActive = false
    }

    private fun setupModeButtons(view: View) {
        val opacityBtn = view.findViewById<TextView>(R.id.di_overlay_mode_opacity)
        val sliderBtn = view.findViewById<TextView>(R.id.di_overlay_mode_slider)
        val flickerBtn = view.findViewById<TextView>(R.id.di_overlay_mode_flicker)
        val diffBtn = view.findViewById<TextView>(R.id.di_overlay_mode_diff)

        modeButtons[CompareMode.OPACITY] = opacityBtn
        modeButtons[CompareMode.SLIDER] = sliderBtn
        modeButtons[CompareMode.FLICKER] = flickerBtn
        modeButtons[CompareMode.PIXEL_DIFF] = diffBtn

        opacityBtn.setOnClickListener { selectMode(CompareMode.OPACITY) }
        sliderBtn.setOnClickListener { selectMode(CompareMode.SLIDER) }
        flickerBtn.setOnClickListener { selectMode(CompareMode.FLICKER) }
        diffBtn.setOnClickListener { selectMode(CompareMode.PIXEL_DIFF) }
    }

    private fun selectMode(mode: CompareMode) {
        currentMode = mode
        updateModeButtonStates()
        onModeChanged(mode)

        val opacityLabel = controlView?.findViewById<TextView>(R.id.di_overlay_opacity_label)
        val opacitySlider = controlView?.findViewById<SeekBar>(R.id.di_overlay_opacity_slider)
        val showOpacity = mode == CompareMode.OPACITY
        opacityLabel?.visibility = if (showOpacity) View.VISIBLE else View.GONE
        opacitySlider?.visibility = if (showOpacity) View.VISIBLE else View.GONE
    }

    private fun updateModeButtonStates() {
        val activeColor = ContextCompat.getColor(activity, R.color.debug_inspector_primary)
        val inactiveAlpha = 0.5f

        modeButtons.forEach { (mode, btn) ->
            if (mode == currentMode) {
                btn.alpha = 1f
                btn.setTypeface(null, Typeface.BOLD)
                btn.setBackgroundResource(R.drawable.bg_apply_btn)
            } else {
                btn.alpha = inactiveAlpha
                btn.setTypeface(null, Typeface.NORMAL)
                btn.setBackgroundResource(R.drawable.bg_control_btn)
            }
        }
    }

    private fun setupOpacitySlider(view: View) {
        val opacityLabel = view.findViewById<TextView>(R.id.di_overlay_opacity_label)
        val slider = view.findViewById<SeekBar>(R.id.di_overlay_opacity_slider)

        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                opacityLabel.text = "$progress%"
                onOpacityChanged((progress * 255) / 100)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupGridAndClose(view: View) {
        val gridBtn = view.findViewById<ImageView>(R.id.di_overlay_grid_btn)
        val closeBtn = view.findViewById<ImageView>(R.id.di_overlay_close_btn)

        gridBtn.alpha = 0.5f
        gridBtn.setOnClickListener {
            isGridActive = !isGridActive
            gridBtn.alpha = if (isGridActive) 1f else 0.5f
            onGridToggle()
        }

        closeBtn.setOnClickListener { onClose() }
    }
}
