package com.shashank.appinspector.highlight

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import com.shashank.appinspector.DebugConfig
import com.shashank.appinspector.R

internal class HighlightOverlay(context: Context) : View(context) {

    private var highlightRect: Rect? = null
    private val locationOffset = IntArray(2)
    private var pulseAnimator: ValueAnimator? = null

    private val density = context.resources.displayMetrics.density

    private val borderColor = ContextCompat.getColor(context, R.color.debug_inspector_highlight_border)
    private val fillColor = ContextCompat.getColor(context, R.color.debug_inspector_highlight_fill)
    private val fillFlashColor = ContextCompat.getColor(context, R.color.debug_inspector_highlight_fill_flash)
    private val cornerColor = ContextCompat.getColor(context, R.color.debug_inspector_highlight_corner)
    private val pillBgColor = ContextCompat.getColor(context, R.color.debug_inspector_highlight_pill_bg)

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderColor
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }

    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cornerColor
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        strokeCap = Paint.Cap.SQUARE
    }

    private val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pillBgColor
        style = Paint.Style.FILL
    }

    private val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 11f * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val cornerLength = 14f * density

    init {
        setTag(R.id.debug_inspector_tag, DebugConfig.INSPECTOR_VIEW_TAG)
        setWillNotDraw(false)
        isClickable = false
        isFocusable = false
        visibility = GONE
    }

    fun highlightView(target: View, isLongPress: Boolean = false) {
        animate().cancel()
        stopPulse()

        val targetLoc = IntArray(2)
        target.getLocationOnScreen(targetLoc)
        getLocationOnScreen(locationOffset)

        highlightRect = Rect(
            targetLoc[0] - locationOffset[0],
            targetLoc[1] - locationOffset[1],
            targetLoc[0] - locationOffset[0] + target.width,
            targetLoc[1] - locationOffset[1] + target.height
        )

        visibility = VISIBLE
        alpha = 1f

        scaleX = 0.88f
        scaleY = 0.88f
        animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(180)
            .setInterpolator(OvershootInterpolator(1.5f))
            .withEndAction { startPulse() }
            .start()

        if (isLongPress) {
            fillPaint.color = fillFlashColor
            invalidate()
            postDelayed({
                fillPaint.color = fillColor
                invalidate()
            }, 140)
        } else {
            invalidate()
        }
    }

    fun highlightScreenRect(screenRect: Rect) {
        animate().cancel()
        stopPulse()
        getLocationOnScreen(locationOffset)
        highlightRect = Rect(
            screenRect.left - locationOffset[0],
            screenRect.top - locationOffset[1],
            screenRect.right - locationOffset[0],
            screenRect.bottom - locationOffset[1]
        )
        visibility = VISIBLE
        alpha = 1f
        scaleX = 0.88f
        scaleY = 0.88f
        animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(180)
            .setInterpolator(OvershootInterpolator(1.5f))
            .withEndAction { startPulse() }
            .start()
        invalidate()
    }

    fun clearHighlight() {
        stopPulse()
        animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                visibility = GONE
                highlightRect = null
            }
            .start()
    }

    private fun startPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofInt(255, 120).apply {
            duration = 900
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                borderPaint.alpha = it.animatedValue as Int
                invalidate()
            }
            start()
        }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        borderPaint.alpha = 255
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopPulse()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rect = highlightRect ?: return
        val rf = RectF(rect)

        canvas.drawRect(rf, fillPaint)
        canvas.drawRect(rf, borderPaint)
        drawCornerBrackets(canvas, rect)
        drawDimensionPill(canvas, rect)
    }

    private fun drawCornerBrackets(canvas: Canvas, rect: Rect) {
        val l = rect.left.toFloat()
        val t = rect.top.toFloat()
        val r = rect.right.toFloat()
        val b = rect.bottom.toFloat()
        val cl = cornerLength

        canvas.drawLine(l, t, l + cl, t, cornerPaint)
        canvas.drawLine(l, t, l, t + cl, cornerPaint)
        canvas.drawLine(r - cl, t, r, t, cornerPaint)
        canvas.drawLine(r, t, r, t + cl, cornerPaint)
        canvas.drawLine(l, b, l + cl, b, cornerPaint)
        canvas.drawLine(l, b - cl, l, b, cornerPaint)
        canvas.drawLine(r - cl, b, r, b, cornerPaint)
        canvas.drawLine(r, b - cl, r, b, cornerPaint)
    }

    private fun drawDimensionPill(canvas: Canvas, rect: Rect) {
        val widthDp = (rect.width() / density + 0.5f).toInt()
        val heightDp = (rect.height() / density + 0.5f).toInt()
        val label = "${widthDp}dp x ${heightDp}dp"

        val textWidth = pillTextPaint.measureText(label)
        val pillPaddingH = 8f * density
        val pillPaddingV = 4f * density
        val pillW = textWidth + pillPaddingH * 2
        val pillH = pillTextPaint.textSize + pillPaddingV * 2

        val pillLeft = rect.centerX() - pillW / 2f
        val pillRight = rect.centerX() + pillW / 2f
        val margin = 6f * density
        val useTop = rect.top > (pillH + margin)
        val pillTop = if (useTop) rect.top - pillH - margin else rect.bottom + margin
        val pillBottom = pillTop + pillH

        val pillRect = RectF(pillLeft, pillTop, pillRight, pillBottom)
        canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillBgPaint)
        canvas.drawText(
            label,
            rect.centerX().toFloat(),
            pillBottom - pillPaddingV - 1f * density,
            pillTextPaint
        )
    }
}
