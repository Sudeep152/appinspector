package com.shashank.appinspector.designoverlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.shashank.appinspector.DebugConfig
import com.shashank.appinspector.R
import kotlin.math.abs

internal enum class CompareMode { OPACITY, SLIDER, FLICKER, PIXEL_DIFF }

@SuppressLint("ViewConstructor")
internal class DesignOverlayView(
    context: Context,
    private var overlayBitmap: Bitmap?
) : View(context) {

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.debug_inspector_grid_line)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val sliderLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f * context.resources.displayMetrics.density
    }
    private val sliderHandlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val density = context.resources.displayMetrics.density
    private val gridSpacing = 8f * density

    var compareMode = CompareMode.OPACITY
        set(value) {
            stopFlicker()
            field = value
            if (value == CompareMode.FLICKER) startFlicker()
            if (value == CompareMode.PIXEL_DIFF) generateDiffBitmap()
            invalidate()
        }

    private var overlayAlpha = 128
    var isGridEnabled = false
        set(value) { field = value; invalidate() }

    private var sliderX = 0f
    private var isDraggingSlider = false

    private val flickerHandler = Handler(Looper.getMainLooper())
    private var flickerVisible = true
    private val flickerRunnable = object : Runnable {
        override fun run() {
            flickerVisible = !flickerVisible
            invalidate()
            flickerHandler.postDelayed(this, 200)
        }
    }

    private var diffBitmap: Bitmap? = null
    private var screenBitmap: Bitmap? = null

    init {
        setTag(R.id.debug_inspector_tag, DebugConfig.INSPECTOR_VIEW_TAG)
        setWillNotDraw(false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        sliderX = w / 2f
    }

    fun setOverlayAlpha(alpha: Int) {
        overlayAlpha = alpha.coerceIn(0, 255)
        invalidate()
    }

    fun setScreenBitmap(bitmap: Bitmap?) {
        screenBitmap = bitmap
        if (compareMode == CompareMode.PIXEL_DIFF) generateDiffBitmap()
    }

    @Volatile private var isRecycled = false

    fun recycleBitmap() {
        isRecycled = true
        stopFlicker()
        synchronized(this) {
            overlayBitmap?.recycle()
            overlayBitmap = null
            diffBitmap?.recycle()
            diffBitmap = null
            screenBitmap = null
        }
    }

    private fun getScaledBitmap(): Bitmap? {
        val bitmap = overlayBitmap ?: return null
        if (bitmap.isRecycled || width <= 0 || height <= 0) return null
        return try {
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } catch (_: Exception) { null }
    }

    override fun onDraw(canvas: Canvas) {
        try {
            if (isRecycled) return
            super.onDraw(canvas)
            synchronized(this) {
                when (compareMode) {
                    CompareMode.OPACITY -> drawOpacityMode(canvas)
                    CompareMode.SLIDER -> drawSliderMode(canvas)
                    CompareMode.FLICKER -> drawFlickerMode(canvas)
                    CompareMode.PIXEL_DIFF -> drawPixelDiffMode(canvas)
                }
            }
            if (isGridEnabled) drawGrid(canvas)
        } catch (_: Exception) { }
    }

    private fun drawBitmapSafe(canvas: Canvas, bitmap: Bitmap?, alpha: Int, clipLeft: Float = 0f) {
        val bmp = bitmap ?: return
        if (bmp.isRecycled) return
        bitmapPaint.alpha = alpha
        val src = Rect(0, 0, bmp.width, bmp.height)
        val dst = RectF(0f, 0f, width.toFloat(), height.toFloat())
        if (clipLeft > 0f) {
            canvas.save()
            canvas.clipRect(clipLeft, 0f, width.toFloat(), height.toFloat())
            canvas.drawBitmap(bmp, src, dst, bitmapPaint)
            canvas.restore()
        } else {
            canvas.drawBitmap(bmp, src, dst, bitmapPaint)
        }
    }

    private fun drawOpacityMode(canvas: Canvas) {
        drawBitmapSafe(canvas, overlayBitmap, overlayAlpha)
    }

    private fun drawSliderMode(canvas: Canvas) {
        val sliderPos = sliderX.coerceIn(0f, width.toFloat())
        drawBitmapSafe(canvas, overlayBitmap, 255, clipLeft = sliderPos)

        canvas.drawLine(sliderPos, 0f, sliderPos, height.toFloat(), sliderLinePaint)
        val handleRadius = 14f * density
        canvas.drawCircle(sliderPos, height / 2f, handleRadius, sliderHandlePaint)
    }

    private fun drawFlickerMode(canvas: Canvas) {
        if (!flickerVisible) return
        drawBitmapSafe(canvas, overlayBitmap, 255)
    }

    private fun drawPixelDiffMode(canvas: Canvas) {
        val diff = diffBitmap
        if (diff != null && !diff.isRecycled) {
            drawBitmapSafe(canvas, diff, 200)
        } else {
            drawOpacityMode(canvas)
        }
    }

    private fun generateDiffBitmap() {
        val screen = screenBitmap ?: return
        val overlay = overlayBitmap ?: return
        if (screen.isRecycled || overlay.isRecycled) return

        Thread {
            try {
                if (isRecycled) return@Thread
                val w = screen.width.coerceAtMost(overlay.width).coerceAtMost(1080)
                val h = screen.height.coerceAtMost(overlay.height).coerceAtMost(1920)
                if (w <= 0 || h <= 0) return@Thread

                val scaledScreen = Bitmap.createScaledBitmap(screen, w, h, true)
                val scaledOverlay = Bitmap.createScaledBitmap(overlay, w, h, true)
                val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

                val threshold = 30
                for (y in 0 until h) {
                    if (isRecycled) { result.recycle(); return@Thread }
                    for (x in 0 until w) {
                        val pxA = scaledScreen.getPixel(x, y)
                        val pxB = scaledOverlay.getPixel(x, y)
                        val dr = abs(Color.red(pxA) - Color.red(pxB))
                        val dg = abs(Color.green(pxA) - Color.green(pxB))
                        val db = abs(Color.blue(pxA) - Color.blue(pxB))
                        if (dr > threshold || dg > threshold || db > threshold) {
                            result.setPixel(x, y, Color.argb(180, 229, 57, 53))
                        }
                    }
                }

                if (scaledScreen !== screen) scaledScreen.recycle()
                if (scaledOverlay !== overlay) scaledOverlay.recycle()

                synchronized(this@DesignOverlayView) {
                    if (!isRecycled) {
                        diffBitmap?.recycle()
                        diffBitmap = result
                    } else {
                        result.recycle()
                    }
                }
                if (!isRecycled && isAttachedToWindow) post { invalidate() }
            } catch (_: Exception) { }
        }.start()
    }

    private fun startFlicker() {
        flickerVisible = true
        flickerHandler.postDelayed(flickerRunnable, 200)
    }

    private fun stopFlicker() {
        flickerHandler.removeCallbacks(flickerRunnable)
        flickerVisible = true
    }

    private fun drawGrid(canvas: Canvas) {
        if (gridSpacing < 1f) return
        var x = 0f
        while (x < width) { canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint); x += gridSpacing }
        var y = 0f
        while (y < height) { canvas.drawLine(0f, y, width.toFloat(), y, gridPaint); y += gridSpacing }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        try {
            if (compareMode == CompareMode.SLIDER) return handleSliderTouch(event)
        } catch (_: Exception) { }
        return true
    }

    private fun handleSliderTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isDraggingSlider = abs(event.x - sliderX) < 40 * density
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingSlider) {
                    sliderX = event.x.coerceIn(0f, width.toFloat())
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDraggingSlider = false
                return true
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopFlicker()
    }
}
