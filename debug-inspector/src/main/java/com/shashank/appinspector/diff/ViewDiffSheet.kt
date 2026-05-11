package com.shashank.appinspector.diff

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.shashank.appinspector.R
import com.shashank.appinspector.utils.FragmentOwnerResolver
import com.shashank.appinspector.utils.ResourceUtils

internal class ViewDiffSheet(private val activity: Activity) {

    fun show(viewA: View, viewB: View) {
        if (activity.isDestroyed || activity.isFinishing) return

        val dialog = BottomSheetDialog(activity)
        val density = activity.resources.displayMetrics.density

        val container = ScrollView(activity).apply {
            setBackgroundResource(R.drawable.bg_bottom_sheet)
            clipToPadding = false
        }

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        addTitle(content)
        addSeparator(content)

        val props = buildPropertyList(viewA, viewB, density)
        for ((name, valA, valB) in props) {
            addRow(content, name, valA, valB)
        }

        container.addView(content)
        dialog.setContentView(container)
        dialog.behavior.peekHeight = (activity.resources.displayMetrics.heightPixels * 0.6).toInt()
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.show()
    }

    private data class PropRow(val name: String, val valueA: String, val valueB: String)

    private fun buildPropertyList(viewA: View, viewB: View, density: Float): List<PropRow> {
        val list = mutableListOf<PropRow>()

        list.add(PropRow("Class", viewA.javaClass.simpleName, viewB.javaClass.simpleName))
        list.add(PropRow("ID",
            ResourceUtils.getResourceName(viewA) ?: "no-id",
            ResourceUtils.getResourceName(viewB) ?: "no-id"
        ))

        val wa = ResourceUtils.pxToDp(viewA.width, density)
        val ha = ResourceUtils.pxToDp(viewA.height, density)
        val wb = ResourceUtils.pxToDp(viewB.width, density)
        val hb = ResourceUtils.pxToDp(viewB.height, density)
        list.add(PropRow("Size", "${wa}x${ha}dp", "${wb}x${hb}dp"))

        val pA = "${ResourceUtils.pxToDp(viewA.paddingLeft, density)},${ResourceUtils.pxToDp(viewA.paddingTop, density)},${ResourceUtils.pxToDp(viewA.paddingRight, density)},${ResourceUtils.pxToDp(viewA.paddingBottom, density)}"
        val pB = "${ResourceUtils.pxToDp(viewB.paddingLeft, density)},${ResourceUtils.pxToDp(viewB.paddingTop, density)},${ResourceUtils.pxToDp(viewB.paddingRight, density)},${ResourceUtils.pxToDp(viewB.paddingBottom, density)}"
        list.add(PropRow("Padding", pA, pB))

        list.add(PropRow("Margins",
            ResourceUtils.getMarginsString(viewA, density) ?: "none",
            ResourceUtils.getMarginsString(viewB, density) ?: "none"
        ))
        list.add(PropRow("Elevation", ResourceUtils.getElevationDp(viewA, density), ResourceUtils.getElevationDp(viewB, density)))
        list.add(PropRow("Alpha", ResourceUtils.getAlphaString(viewA), ResourceUtils.getAlphaString(viewB)))
        list.add(PropRow("Visibility",
            ResourceUtils.getVisibilityString(viewA.visibility),
            ResourceUtils.getVisibilityString(viewB.visibility)
        ))
        list.add(PropRow("Background",
            ResourceUtils.getBackgroundColorHex(viewA) ?: "none",
            ResourceUtils.getBackgroundColorHex(viewB) ?: "none"
        ))

        if (viewA is TextView || viewB is TextView) {
            val textA = (viewA as? TextView)?.text?.toString() ?: "N/A"
            val textB = (viewB as? TextView)?.text?.toString() ?: "N/A"
            list.add(PropRow("Text", textA, textB))
        }

        list.add(PropRow("Fragment",
            FragmentOwnerResolver.getOwnerName(activity, viewA) ?: "none",
            FragmentOwnerResolver.getOwnerName(activity, viewB) ?: "none"
        ))

        return list
    }

    private fun addTitle(parent: LinearLayout) {
        val title = TextView(activity).apply {
            text = activity.getString(R.string.debug_inspector_compare_title)
            setTextColor(ContextCompat.getColor(activity, R.color.debug_inspector_text_primary))
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        }
        parent.addView(title)

        val header = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        header.addView(makeCell("View A", true, 0))
        header.addView(makeCell("Property", true, 0))
        header.addView(makeCell("View B", true, 0))
        parent.addView(header)
    }

    private fun addSeparator(parent: LinearLayout) {
        val sep = View(activity).apply {
            setBackgroundColor(ContextCompat.getColor(activity, R.color.debug_inspector_divider))
        }
        parent.addView(sep, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
        ).apply { topMargin = dp(4); bottomMargin = dp(4) })
    }

    private fun addRow(parent: LinearLayout, name: String, valA: String, valB: String) {
        val isDifferent = valA != valB
        val bgColor = if (isDifferent) {
            ContextCompat.getColor(activity, R.color.debug_inspector_diff_different)
        } else {
            0
        }

        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            if (isDifferent) setBackgroundColor(bgColor)
            setPadding(0, dp(4), 0, dp(4))
        }

        row.addView(makeCell(valA, false, if (isDifferent) ContextCompat.getColor(activity, R.color.debug_inspector_priority_high) else 0))
        row.addView(makeCell(name, true, 0))
        row.addView(makeCell(valB, false, if (isDifferent) ContextCompat.getColor(activity, R.color.debug_inspector_priority_high) else 0))

        parent.addView(row)
    }

    private fun makeCell(text: String, isBold: Boolean, textColor: Int): TextView {
        return TextView(activity).apply {
            this.text = text
            textSize = 11f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            if (isBold) setTypeface(null, android.graphics.Typeface.BOLD)
            if (textColor != 0) {
                setTextColor(textColor)
            } else {
                setTextColor(ContextCompat.getColor(activity, R.color.debug_inspector_text_primary))
            }
            maxLines = 2
            setPadding(dp(4), 0, dp(4), 0)
        }
    }

    private fun dp(value: Int): Int = (value * activity.resources.displayMetrics.density).toInt()
}
