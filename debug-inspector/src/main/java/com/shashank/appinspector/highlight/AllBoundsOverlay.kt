package com.shashank.appinspector.highlight

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import com.shashank.appinspector.DebugConfig
import com.shashank.appinspector.R

internal class AllBoundsOverlay(context: Context) : View(context) {

    private val density = context.resources.displayMetrics.density
    private val viewBounds = mutableListOf<BoundEntry>()
    private val locationOffset = IntArray(2)

    private val depthColors = intArrayOf(
        0xAAE53935.toInt(),
        0xAAFB8C00.toInt(),
        0xAAFDD835.toInt(),
        0xAA43A047.toInt(),
        0xAA1E88E5.toInt(),
        0xAA8E24AA.toInt()
    )

    private val boundsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 8f * density
        isFakeBoldText = true
    }

    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private data class BoundEntry(val rect: Rect, val depth: Int, val label: String)

    init {
        setTag(R.id.debug_inspector_tag, DebugConfig.INSPECTOR_VIEW_TAG)
        setWillNotDraw(false)
        isClickable = false
        isFocusable = false
    }

    fun scanViews(root: View) {
        viewBounds.clear()
        getLocationOnScreen(locationOffset)
        collectBounds(root, 0)
        invalidate()
    }

    fun showAccessibilityBounds(nodes: List<com.shashank.appinspector.touch.AccessibilityNodeInspector.NodeInfo>) {
        viewBounds.clear()
        getLocationOnScreen(locationOffset)
        nodes.forEachIndexed { index, node ->
            val rect = node.boundsInScreen
            val localRect = Rect(
                rect.left - locationOffset[0],
                rect.top - locationOffset[1],
                rect.right - locationOffset[0],
                rect.bottom - locationOffset[1]
            )
            viewBounds.add(BoundEntry(localRect, index % depthColors.size, node.className))
        }
        invalidate()
    }

    fun clearBounds() {
        viewBounds.clear()
        invalidate()
    }

    private fun collectBounds(view: View, depth: Int) {
        if (view.getTag(R.id.debug_inspector_tag) == DebugConfig.INSPECTOR_VIEW_TAG) return
        if (view.visibility == GONE) return

        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val rect = Rect(
            loc[0] - locationOffset[0],
            loc[1] - locationOffset[1],
            loc[0] - locationOffset[0] + view.width,
            loc[1] - locationOffset[1] + view.height
        )

        val widthDp = (view.width / density).toInt()
        val heightDp = (view.height / density).toInt()
        val label = if (widthDp >= 40 && heightDp >= 20) view.javaClass.simpleName else ""

        viewBounds.add(BoundEntry(rect, depth, label))

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                collectBounds(view.getChildAt(i), depth + 1)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (entry in viewBounds) {
            val color = depthColors[entry.depth.coerceAtMost(depthColors.size - 1)]
            boundsPaint.color = color

            canvas.drawRect(
                entry.rect.left.toFloat(),
                entry.rect.top.toFloat(),
                entry.rect.right.toFloat(),
                entry.rect.bottom.toFloat(),
                boundsPaint
            )

            if (entry.label.isNotEmpty()) {
                labelBgPaint.color = (color and 0x00FFFFFF) or 0x99000000.toInt()
                labelPaint.color = 0xFFFFFFFF.toInt()

                val textWidth = labelPaint.measureText(entry.label)
                val pad = 2f * density
                val textHeight = labelPaint.textSize

                canvas.drawRect(
                    entry.rect.left.toFloat(),
                    entry.rect.top.toFloat(),
                    entry.rect.left + textWidth + pad * 2,
                    entry.rect.top + textHeight + pad,
                    labelBgPaint
                )
                canvas.drawText(
                    entry.label,
                    entry.rect.left + pad,
                    entry.rect.top + textHeight,
                    labelPaint
                )
            }
        }
    }
}
