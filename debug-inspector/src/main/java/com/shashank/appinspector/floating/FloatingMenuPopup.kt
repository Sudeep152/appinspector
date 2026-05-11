package com.shashank.appinspector.floating

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.shashank.appinspector.DebugConfig
import com.shashank.appinspector.R

internal class FloatingMenuPopup(
    private val activity: Activity,
    private val config: DebugConfig,
    private val onInspectToggle: () -> Unit,
    private val onBoundsToggle: () -> Unit,
    private val onRulerToggle: () -> Unit,
    private val onDesignOverlay: () -> Unit
) {

    private var popup: PopupWindow? = null

    fun show(anchor: View) {
        try {
            if (activity.isDestroyed || activity.isFinishing) return
            dismiss()

            val contentView = LayoutInflater.from(activity).inflate(R.layout.layout_floating_menu, null)

            setupItem(contentView, R.id.di_menu_inspect, R.id.di_menu_inspect_icon,
                R.id.di_menu_inspect_label, R.id.di_menu_inspect_dot, config.isInspectModeEnabled
            ) { dismiss(); onInspectToggle() }

            setupItem(contentView, R.id.di_menu_bounds, R.id.di_menu_bounds_icon,
                R.id.di_menu_bounds_label, R.id.di_menu_bounds_dot, config.isBoundsOverlayActive
            ) { dismiss(); onBoundsToggle() }

            setupItem(contentView, R.id.di_menu_ruler, R.id.di_menu_ruler_icon,
                R.id.di_menu_ruler_label, R.id.di_menu_ruler_dot, config.isRulerActive
            ) { dismiss(); onRulerToggle() }

            setupItem(contentView, R.id.di_menu_design_overlay, R.id.di_menu_overlay_icon,
                R.id.di_menu_overlay_label, R.id.di_menu_overlay_dot, config.isDesignOverlayActive
            ) { dismiss(); onDesignOverlay() }

            val widthPx = activity.resources.getDimensionPixelSize(R.dimen.debug_inspector_menu_width)
            popup = PopupWindow(contentView, widthPx, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
                isOutsideTouchable = true
                elevation = activity.resources.getDimension(R.dimen.debug_inspector_menu_elevation)
                setBackgroundDrawable(ContextCompat.getDrawable(activity, R.drawable.bg_menu_popup))
                animationStyle = android.R.style.Animation_Dialog
            }

            contentView.measure(
                View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val popupHeight = contentView.measuredHeight
            val density = activity.resources.displayMetrics.density
            val anchorLoc = IntArray(2)
            anchor.getLocationOnScreen(anchorLoc)
            val screenWidth = activity.resources.displayMetrics.widthPixels
            val screenHeight = activity.resources.displayMetrics.heightPixels
            val margin = (12 * density).toInt()

            val x = (anchorLoc[0] + anchor.width / 2 - widthPx / 2).coerceIn(margin, screenWidth - widthPx - margin)
            val y = if (anchorLoc[1] > screenHeight - anchorLoc[1] - anchor.height) {
                (anchorLoc[1] - popupHeight - margin).coerceAtLeast(margin)
            } else {
                (anchorLoc[1] + anchor.height + margin).coerceAtMost(screenHeight - popupHeight - margin)
            }

            popup?.showAtLocation(activity.window?.decorView ?: return, Gravity.NO_GRAVITY, x, y)
        } catch (_: Exception) { dismiss() }
    }

    fun dismiss() { popup?.dismiss(); popup = null }

    fun isShowing(): Boolean = popup?.isShowing == true

    private fun setupItem(
        contentView: View, rootId: Int, iconId: Int, labelId: Int, dotId: Int,
        isActive: Boolean, onClick: () -> Unit
    ) {
        val root = contentView.findViewById<View>(rootId)
        val icon = contentView.findViewById<ImageView>(iconId)
        val label = contentView.findViewById<TextView>(labelId)
        val dot = contentView.findViewById<View>(dotId)

        if (isActive) {
            val color = ContextCompat.getColor(activity, R.color.debug_inspector_menu_icon_active)
            icon.setColorFilter(color)
            label.setTextColor(color)
            dot.visibility = View.VISIBLE
            (dot.background as? GradientDrawable)?.setColor(color)
            root.setBackgroundResource(R.drawable.bg_menu_item_active)
        } else {
            icon.setColorFilter(ContextCompat.getColor(activity, R.color.debug_inspector_menu_icon_inactive))
            label.setTextColor(ContextCompat.getColor(activity, R.color.debug_inspector_text_primary))
            dot.visibility = View.GONE
        }
        root.setOnClickListener { onClick() }
    }
}
