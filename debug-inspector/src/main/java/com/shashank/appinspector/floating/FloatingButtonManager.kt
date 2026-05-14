package com.shashank.appinspector.floating

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.shashank.appinspector.DebugConfig
import com.shashank.appinspector.R
import com.shashank.appinspector.designoverlay.DesignOverlayManager
import com.shashank.appinspector.diff.ViewDiffSheet
import com.shashank.appinspector.highlight.AllBoundsOverlay
import com.shashank.appinspector.highlight.HighlightOverlay
import com.shashank.appinspector.overlay.InspectorBottomSheet
import com.shashank.appinspector.ruler.RulerOverlay
import com.shashank.appinspector.touch.InspectTouchInterceptor
import com.shashank.appinspector.touch.AccessibilityNodeInspector
import com.shashank.appinspector.touch.ViewFinder
import com.shashank.appinspector.touch.ViewStackPickerPopup
import java.lang.ref.WeakReference

internal class FloatingButtonManager(private val config: DebugConfig) {

    private companion object {
        const val TAG = "DebugInspector"
        const val PREF_KEY_ENABLED = "geekstats_debug_inspector_enabled"
    }

    private val activityStates = mutableMapOf<Int, ActivityState>()
    private val pendingAttachKeys = mutableSetOf<Int>()
    private var lastButtonX = -1
    private var lastButtonY = -1

    private var compareViewA: WeakReference<View>? = null

    private class ActivityState(
        val activityRef: WeakReference<Activity>,
        val floatingButton: ImageView,
        val highlightOverlay: HighlightOverlay,
        val touchInterceptor: InspectTouchInterceptor,
        val bottomSheet: InspectorBottomSheet,
        val stackPicker: ViewStackPickerPopup,
        val menuPopup: FloatingMenuPopup,
        val designOverlayManager: DesignOverlayManager,
        val boundsOverlay: AllBoundsOverlay,
        var rulerOverlay: RulerOverlay?
    )

    fun attach(activity: Activity) {
        try {
            if (!config.isEnabled) return
            if (!isEnabledInPrefs(activity)) return
            val key = System.identityHashCode(activity)
            if (activityStates.containsKey(key) || pendingAttachKeys.contains(key)) return
            pendingAttachKeys.add(key)

            activity.window.decorView.post {
                pendingAttachKeys.remove(key)
                if (!activity.isDestroyed && !activity.isFinishing) {
                    try { attachInternal(activity, key) }
                    catch (e: Exception) { Log.e(TAG, "Error attaching inspector", e) }
                }
            }
        } catch (e: Exception) { Log.e(TAG, "Error in attach", e) }
    }

    private fun attachInternal(activity: Activity, key: Int) {
        if (activityStates.containsKey(key)) return
        val decorView = activity.window.decorView as? FrameLayout ?: return

        val highlightOverlay = HighlightOverlay(activity)
        val boundsOverlay = AllBoundsOverlay(activity)

        val bottomSheet = InspectorBottomSheet(activity) { selectedView ->
            highlightOverlay.highlightView(selectedView, isLongPress = false)
            activityStates[key]?.bottomSheet?.show(selectedView)
        }

        val stackPicker = ViewStackPickerPopup(activity) { selectedView ->
            highlightOverlay.highlightView(selectedView, isLongPress = false)
            bottomSheet.show(selectedView)
        }

        val touchInterceptor = InspectTouchInterceptor(
            context = activity,
            onTap = { screenX, screenY -> handleTap(key, decorView, screenX, screenY) },
            onLongPress = { screenX, screenY -> handleLongPress(key, decorView, screenX, screenY) }
        )

        val designOverlayManager = DesignOverlayManager(activity, config)
        val floatingButton = createFloatingButton(activity)

        val menuPopup = FloatingMenuPopup(
            activity = activity,
            config = config,
            onInspectToggle = { toggleInspectMode() },
            onBoundsToggle = { toggleBoundsOverlay(key) },
            onRulerToggle = { toggleRuler(key, activity) },
            onDesignOverlay = {
                if (config.isDesignOverlayActive) {
                    designOverlayManager.dismiss()
                    config.isDesignOverlayActive = false
                    updateFabState()
                } else {
                    designOverlayManager.showPicker {
                        config.isDesignOverlayActive = true
                        updateFabState()
                    }
                }
            }
        )

        decorView.addView(highlightOverlay, createMatchParentParams())
        decorView.addView(boundsOverlay, createMatchParentParams())
        decorView.addView(touchInterceptor, createMatchParentParams())

        decorView.addView(floatingButton, createFloatingButtonParams(activity))
        floatingButton.bringToFront()

        highlightOverlay.visibility = View.GONE
        boundsOverlay.visibility = View.GONE
        touchInterceptor.visibility = View.GONE

        if (config.isInspectModeEnabled) {
            touchInterceptor.visibility = View.VISIBLE
            highlightOverlay.visibility = View.VISIBLE
        }
        updateFabState()

        activityStates[key] = ActivityState(
            activityRef = WeakReference(activity),
            floatingButton = floatingButton,
            highlightOverlay = highlightOverlay,
            touchInterceptor = touchInterceptor,
            bottomSheet = bottomSheet,
            stackPicker = stackPicker,
            menuPopup = menuPopup,
            designOverlayManager = designOverlayManager,
            boundsOverlay = boundsOverlay,
            rulerOverlay = null
        )
    }

    private fun handleTap(key: Int, decorView: ViewGroup, screenX: Int, screenY: Int) {
        try {
            val state = activityStates[key] ?: return
            val contentRoot = (decorView as? ViewGroup)?.getChildAt(0) ?: decorView

            if (config.isCompareActive) {
                val tapped = ViewFinder.findViewAt(contentRoot, screenX, screenY) ?: return
                val a = compareViewA?.get()
                if (a == null) {
                    compareViewA = WeakReference(tapped)
                    state.highlightOverlay.highlightView(tapped, isLongPress = false)
                    try {
                        Toast.makeText(
                            state.activityRef.get()?.applicationContext,
                            R.string.debug_inspector_compare_select_b,
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (_: Exception) {}
                } else {
                    config.isCompareActive = false
                    state.touchInterceptor.visibility = View.GONE
                    state.highlightOverlay.clearHighlight()
                    compareViewA = null
                    updateFabState()
                    val act = state.activityRef.get()
                    if (act != null && !act.isDestroyed && !act.isFinishing) {
                        ViewDiffSheet(act).show(a, tapped)
                    }
                }
                return
            }

            val composeView = AccessibilityNodeInspector.findComposeViewIn(contentRoot)
            if (composeView != null) {
                val node = AccessibilityNodeInspector.findNodeAt(composeView, screenX, screenY)
                if (node != null) {
                    state.highlightOverlay.highlightScreenRect(node.boundsInScreen)
                    state.bottomSheet.showNodeInfo(node)
                    return
                }
                // Compose tree inspection failed — fall through to view-based inspection
            }

            val allViews = ViewFinder.findAllViewsAt(contentRoot, screenX, screenY)
            when {
                allViews.isEmpty() -> Unit
                allViews.size == 1 -> {
                    state.highlightOverlay.highlightView(allViews.first(), isLongPress = false)
                    state.bottomSheet.show(allViews.first())
                }
                else -> state.stackPicker.show(allViews, screenX, screenY)
            }
        } catch (e: Exception) { Log.e(TAG, "Error in handleTap", e) }
    }

    private fun handleLongPress(key: Int, decorView: ViewGroup, screenX: Int, screenY: Int) {
        try {
            val state = activityStates[key] ?: return
            val contentRoot = (decorView as? ViewGroup)?.getChildAt(0) ?: decorView
            val deepView = ViewFinder.findViewAt(contentRoot, screenX, screenY)

            val composeView = AccessibilityNodeInspector.findComposeViewIn(contentRoot)
            if (composeView != null) {
                val node = AccessibilityNodeInspector.findNodeAt(composeView, screenX, screenY)
                if (node != null) {
                    state.highlightOverlay.highlightScreenRect(node.boundsInScreen)
                    state.bottomSheet.showNodeInfo(node)
                    return
                }
                // Compose tree inspection failed — fall through to view-based inspection
            }
            if (deepView != null) {
                state.highlightOverlay.highlightView(deepView, isLongPress = true)
                state.bottomSheet.show(deepView)
            }
        } catch (e: Exception) { Log.e(TAG, "Error in handleLongPress", e) }
    }

    fun detach(activity: Activity) {
        try {
            val key = System.identityHashCode(activity)
            pendingAttachKeys.remove(key)
            val state = activityStates.remove(key) ?: return

            state.menuPopup.dismiss()
            state.bottomSheet.dismiss()
            state.stackPicker.dismiss()
            state.designOverlayManager.dismiss()
            saveButtonPosition(state.floatingButton)

            (state.floatingButton.parent as? ViewGroup)?.removeView(state.floatingButton)
            (state.touchInterceptor.parent as? ViewGroup)?.removeView(state.touchInterceptor)
            (state.highlightOverlay.parent as? ViewGroup)?.removeView(state.highlightOverlay)
            (state.boundsOverlay.parent as? ViewGroup)?.removeView(state.boundsOverlay)
            state.rulerOverlay?.let { (it.parent as? ViewGroup)?.removeView(it) }

            config.isBoundsOverlayActive = false
            config.isRulerActive = false
            config.isCompareActive = false
            compareViewA = null

            if (activityStates.isEmpty()) {
                config.resetAll()
            }
        } catch (e: Exception) { Log.e(TAG, "Error in detach", e) }
    }

    private fun toggleInspectMode() {
        config.isInspectModeEnabled = !config.isInspectModeEnabled

        activityStates.values.forEach { state ->
            if (config.isInspectModeEnabled) {
                state.touchInterceptor.visibility = View.VISIBLE
                state.highlightOverlay.visibility = View.VISIBLE
            } else {
                state.touchInterceptor.visibility = View.GONE
                state.highlightOverlay.clearHighlight()
                state.highlightOverlay.visibility = View.GONE
                state.bottomSheet.dismiss()
                state.stackPicker.dismiss()
            }
        }
        updateFabState()
    }

    private fun toggleBoundsOverlay(key: Int) {
        config.isBoundsOverlayActive = !config.isBoundsOverlayActive
        val state = activityStates[key] ?: return

        if (config.isBoundsOverlayActive) {
            state.boundsOverlay.visibility = View.VISIBLE
            val decorView = state.activityRef.get()?.window?.decorView ?: return
            val root = (decorView as? ViewGroup)?.getChildAt(0) ?: decorView
            val composeView = AccessibilityNodeInspector.findComposeViewIn(root)
            val composeNodes = if (composeView != null) AccessibilityNodeInspector.getAllNodes(composeView) else emptyList()
            if (composeNodes.isNotEmpty()) {
                state.boundsOverlay.showAccessibilityBounds(composeNodes)
            } else {
                state.boundsOverlay.scanViews(root)
            }
        } else {
            state.boundsOverlay.clearBounds()
            state.boundsOverlay.visibility = View.GONE
        }
        updateFabState()
    }

    private fun toggleRuler(key: Int, activity: Activity) {
        config.isRulerActive = !config.isRulerActive
        val state = activityStates[key] ?: return
        val decorView = activity.window.decorView as? FrameLayout ?: return

        if (config.isRulerActive) {
            val ruler = RulerOverlay(activity) {
                config.isRulerActive = false
                updateFabState()
            }
            decorView.addView(ruler, createMatchParentParams())
            state.rulerOverlay = ruler
        } else {
            state.rulerOverlay?.let { (it.parent as? ViewGroup)?.removeView(it) }
            state.rulerOverlay = null
        }
        updateFabState()
    }

    fun toggleMenuForCurrentActivity() {
        activityStates.values.lastOrNull()?.let { state -> showMenu(state.floatingButton) }
    }

    private fun updateFabState() {
        activityStates.values.forEach { state ->
            if (config.hasActiveFeature) {
                state.floatingButton.setBackgroundResource(R.drawable.bg_floating_button_active)
            } else {
                state.floatingButton.setBackgroundResource(R.drawable.bg_floating_button)
            }
        }
    }

    private fun createFloatingButton(activity: Activity): ImageView {
        val size = activity.resources.getDimensionPixelSize(R.dimen.debug_inspector_fab_size)
        val iconSize = activity.resources.getDimensionPixelSize(R.dimen.debug_inspector_fab_icon_size)
        val iconPad = (size - iconSize) / 2

        return ImageView(activity).apply {
            setTag(R.id.debug_inspector_tag, DebugConfig.INSPECTOR_VIEW_TAG)
            setImageResource(R.drawable.ic_debug_inspector)
            setBackgroundResource(R.drawable.bg_floating_button)
            scaleType = ImageView.ScaleType.CENTER
            imageTintList = ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.debug_inspector_white))
            elevation = 24f * activity.resources.displayMetrics.density
            setPadding(iconPad, iconPad, iconPad, iconPad)
            isClickable = true
            isFocusable = true
            contentDescription = activity.getString(R.string.debug_inspector_cd_fab)
            setOnTouchListener(DragTouchListener { showMenu(this) })
        }
    }

    private fun showMenu(anchor: ImageView) {
        val activity = anchor.context as? Activity ?: return
        val key = System.identityHashCode(activity)
        val state = activityStates[key] ?: return
        if (state.menuPopup.isShowing()) state.menuPopup.dismiss() else state.menuPopup.show(anchor)
    }

    private fun createFloatingButtonParams(activity: Activity): FrameLayout.LayoutParams {
        val size = activity.resources.getDimensionPixelSize(R.dimen.debug_inspector_fab_size)
        val margin = activity.resources.getDimensionPixelSize(R.dimen.debug_inspector_fab_margin)
        val params = FrameLayout.LayoutParams(size, size, Gravity.TOP or Gravity.START)

        if (lastButtonX >= 0 && lastButtonY >= 0) {
            params.leftMargin = lastButtonX
            params.topMargin = lastButtonY
        } else {
            params.leftMargin = activity.resources.displayMetrics.widthPixels - size - margin
            params.topMargin = activity.resources.displayMetrics.heightPixels / 3
        }
        return params
    }

    private fun isEnabledInPrefs(activity: Activity): Boolean {
        return try {
            val prefs = activity.applicationContext.getSharedPreferences(
                activity.packageName + "_preferences", Context.MODE_PRIVATE
            )
            prefs.getString(PREF_KEY_ENABLED, "true").equals("true", ignoreCase = true)
        } catch (_: Exception) {
            true
        }
    }

    private fun saveButtonPosition(button: ImageView) {
        val params = button.layoutParams as? FrameLayout.LayoutParams ?: return
        lastButtonX = params.leftMargin
        lastButtonY = params.topMargin
    }

    private fun createMatchParentParams() = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
    )
}
