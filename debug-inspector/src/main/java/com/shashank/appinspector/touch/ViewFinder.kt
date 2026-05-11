package com.shashank.appinspector.touch

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import com.shashank.appinspector.DebugConfig
import com.shashank.appinspector.R

internal object ViewFinder {

    fun findViewAt(root: View, screenX: Int, screenY: Int): View? {
        if (isInspectorView(root)) return null
        if (root.visibility != View.VISIBLE) return null

        if (root is ViewGroup) {
            for (i in root.childCount - 1 downTo 0) {
                val child = root.getChildAt(i)
                val found = findViewAt(child, screenX, screenY)
                if (found != null) return found
            }
        }

        if (isPointInsideView(root, screenX, screenY)) {
            return root
        }
        return null
    }

    fun findAllViewsAt(root: View, screenX: Int, screenY: Int): List<View> {
        val results = mutableListOf<View>()
        collectViewsAt(root, screenX, screenY, results)
        return results
    }

    private fun collectViewsAt(root: View, screenX: Int, screenY: Int, results: MutableList<View>) {
        if (isInspectorView(root)) return
        if (root.visibility != View.VISIBLE) return

        if (isPointInsideView(root, screenX, screenY)) {
            results.add(root)
        }

        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                collectViewsAt(root.getChildAt(i), screenX, screenY, results)
            }
        }
    }

    private fun isPointInsideView(view: View, screenX: Int, screenY: Int): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val rect = Rect(
            location[0],
            location[1],
            location[0] + view.width,
            location[1] + view.height
        )
        return rect.contains(screenX, screenY)
    }

    private fun isInspectorView(view: View): Boolean {
        return view.getTag(R.id.debug_inspector_tag) == DebugConfig.INSPECTOR_VIEW_TAG
    }
}
