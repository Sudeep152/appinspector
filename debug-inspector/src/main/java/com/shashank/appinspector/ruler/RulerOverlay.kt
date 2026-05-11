package com.shashank.appinspector.ruler

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.shashank.appinspector.DebugConfig
import com.shashank.appinspector.R
import com.shashank.appinspector.utils.ResourceUtils
import kotlin.math.sqrt

@SuppressLint("ViewConstructor")
internal class RulerOverlay(context: Context, private val onDeactivate: () -> Unit) : View(context) {

    private var pointA: Pair<Float, Float>? = null
    private var pointB: Pair<Float, Float>? = null

    private val density = context.resources.displayMetrics.density

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.debug_inspector_ruler_dot)
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.debug_inspector_ruler_line)
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f * density, 4f * density), 0f)
    }

    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.debug_inspector_ruler_label_bg)
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 12f * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val dotRadius = 6f * density

    init {
        setTag(R.id.debug_inspector_tag, DebugConfig.INSPECTOR_VIEW_TAG)
        setWillNotDraw(false)
        isClickable = true
        setBackgroundColor(ContextCompat.getColor(context, R.color.debug_inspector_touch_interceptor))
        try {
            Toast.makeText(context.applicationContext, R.string.debug_inspector_ruler_tap_first, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}
    }

    fun reset() {
        pointA = null
        pointB = null
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val x = event.x
            val y = event.y

            when {
                pointA == null -> {
                    pointA = x to y
                    try {
                        Toast.makeText(context.applicationContext, R.string.debug_inspector_ruler_tap_second, Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {}
                    invalidate()
                }
                pointB == null -> {
                    pointB = x to y
                    invalidate()
                }
                else -> {
                    pointA = x to y
                    pointB = null
                    invalidate()
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val a = pointA ?: return
        canvas.drawCircle(a.first, a.second, dotRadius, dotPaint)

        val b = pointB
        if (b != null) {
            canvas.drawCircle(b.first, b.second, dotRadius, dotPaint)
            canvas.drawLine(a.first, a.second, b.first, b.second, linePaint)

            val dx = Math.abs(b.first - a.first)
            val dy = Math.abs(b.second - a.second)
            val diagonal = sqrt(dx * dx + dy * dy)

            val dxDp = ResourceUtils.pxToDp(dx.toInt(), density)
            val dyDp = ResourceUtils.pxToDp(dy.toInt(), density)
            val diagDp = ResourceUtils.pxToDp(diagonal.toInt(), density)

            val label = "H:${dxDp}dp  V:${dyDp}dp  D:${diagDp}dp"
            drawLabel(canvas, label, (a.first + b.first) / 2, (a.second + b.second) / 2 - 20 * density)
        }
    }

    private fun drawLabel(canvas: Canvas, text: String, cx: Float, cy: Float) {
        val textWidth = labelPaint.measureText(text)
        val padH = 10f * density
        val padV = 6f * density
        val h = labelPaint.textSize + padV * 2

        val rect = RectF(cx - textWidth / 2 - padH, cy - h / 2, cx + textWidth / 2 + padH, cy + h / 2)
        canvas.drawRoundRect(rect, h / 2, h / 2, labelBgPaint)
        canvas.drawText(text, cx, cy + labelPaint.textSize / 3, labelPaint)
    }
}
