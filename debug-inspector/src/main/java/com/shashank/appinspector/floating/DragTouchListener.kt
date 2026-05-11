package com.shashank.appinspector.floating

import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import com.shashank.appinspector.R

internal class DragTouchListener(
    private val onClickAction: () -> Unit
) : View.OnTouchListener {

    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    companion object {
        private const val CLICK_THRESHOLD_PX = 15
        private const val SNAP_ANIMATION_DURATION = 250L
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val layoutParams = view.layoutParams as? FrameLayout.LayoutParams ?: return false
        val parent = view.parent as? ViewGroup ?: return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = layoutParams.leftMargin.toFloat()
                initialY = layoutParams.topMargin.toFloat()
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                view.alpha = 0.85f
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY

                if (!isDragging && (Math.abs(dx) > CLICK_THRESHOLD_PX || Math.abs(dy) > CLICK_THRESHOLD_PX)) {
                    isDragging = true
                }

                if (isDragging) {
                    val newX = (initialX + dx).coerceIn(0f, (parent.width - view.width).toFloat())
                    val newY = (initialY + dy).coerceIn(0f, (parent.height - view.height).toFloat())
                    layoutParams.leftMargin = newX.toInt()
                    layoutParams.topMargin = newY.toInt()
                    layoutParams.rightMargin = 0
                    layoutParams.bottomMargin = 0
                    view.layoutParams = layoutParams
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.alpha = 1f
                if (!isDragging) {
                    view.performClick()
                    onClickAction()
                } else {
                    snapToNearestEdge(view, parent)
                }
                return true
            }
        }
        return false
    }

    private fun snapToNearestEdge(view: View, parent: ViewGroup) {
        val layoutParams = view.layoutParams as? FrameLayout.LayoutParams ?: return
        val currentX = layoutParams.leftMargin
        val parentWidth = parent.width
        val midPoint = parentWidth / 2

        val margin = view.resources.getDimensionPixelSize(R.dimen.debug_inspector_fab_margin)
        val targetX = if (currentX + view.width / 2 < midPoint) margin
        else parentWidth - view.width - margin

        val animator = ValueAnimator.ofInt(currentX, targetX)
        animator.duration = SNAP_ANIMATION_DURATION
        animator.interpolator = OvershootInterpolator(0.8f)
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            layoutParams.leftMargin = value
            view.layoutParams = layoutParams
        }
        animator.start()
    }
}
