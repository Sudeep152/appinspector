package com.shashank.appinspector.touch

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.shashank.appinspector.DebugConfig
import com.shashank.appinspector.R
import kotlin.math.abs

@SuppressLint("ViewConstructor")
internal class InspectTouchInterceptor(
    context: Context,
    private val onTap: (screenX: Int, screenY: Int) -> Unit,
    private val onLongPress: (screenX: Int, screenY: Int) -> Unit
) : View(context) {

    private var downX = 0f
    private var downY = 0f
    private var longPressTriggered = false

    private val longPressHandler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable {
        longPressTriggered = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onLongPress(downX.toInt(), downY.toInt())
    }

    companion object {
        private const val TAP_SLOP_PX = 20
        private const val LONG_PRESS_TIMEOUT_MS = 400L
    }

    init {
        setTag(R.id.debug_inspector_tag, DebugConfig.INSPECTOR_VIEW_TAG)
        isClickable = true
        isFocusable = false
        setBackgroundColor(ContextCompat.getColor(context, R.color.debug_inspector_touch_interceptor))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                longPressTriggered = false
                longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT_MS)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val moved = abs(event.rawX - downX) > TAP_SLOP_PX ||
                        abs(event.rawY - downY) > TAP_SLOP_PX
                if (moved) {
                    longPressHandler.removeCallbacks(longPressRunnable)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                longPressHandler.removeCallbacks(longPressRunnable)
                if (!longPressTriggered) {
                    val dx = abs(event.rawX - downX)
                    val dy = abs(event.rawY - downY)
                    if (dx < TAP_SLOP_PX && dy < TAP_SLOP_PX) {
                        onTap(event.rawX.toInt(), event.rawY.toInt())
                    }
                }
                longPressTriggered = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                longPressHandler.removeCallbacks(longPressRunnable)
                longPressTriggered = false
                return true
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        longPressHandler.removeCallbacks(longPressRunnable)
    }
}
