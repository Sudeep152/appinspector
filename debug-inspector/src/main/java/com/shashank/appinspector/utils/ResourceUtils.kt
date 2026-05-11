package com.shashank.appinspector.utils

import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup

internal object ResourceUtils {

    fun getResourceName(view: View): String? {
        if (view.id == View.NO_ID) return null
        return try {
            val fullName = view.resources.getResourceName(view.id)
            fullName.substringAfter("/")
        } catch (_: Resources.NotFoundException) {
            "0x${Integer.toHexString(view.id)}"
        }
    }

    fun getResourceEntryName(view: View): String? {
        if (view.id == View.NO_ID) return null
        return try {
            view.resources.getResourceEntryName(view.id)
        } catch (_: Resources.NotFoundException) {
            null
        }
    }

    fun pxToDp(px: Int, density: Float): Int = (px / density + 0.5f).toInt()

    fun dpToPx(dp: Int, density: Float): Int = (dp * density + 0.5f).toInt()

    fun getVisibilityString(visibility: Int): String = when (visibility) {
        View.VISIBLE -> "VISIBLE"
        View.INVISIBLE -> "INVISIBLE"
        View.GONE -> "GONE"
        else -> "UNKNOWN($visibility)"
    }

    fun getMarginsString(view: View, density: Float): String? {
        val lp = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return null
        val l = pxToDp(lp.leftMargin, density)
        val t = pxToDp(lp.topMargin, density)
        val r = pxToDp(lp.rightMargin, density)
        val b = pxToDp(lp.bottomMargin, density)
        return "$l, $t, $r, $b dp"
    }

    fun getElevationDp(view: View, density: Float): String {
        val dp = pxToDp(view.elevation.toInt(), density)
        return "${dp}dp"
    }

    fun getAlphaString(view: View): String = "%.2f".format(view.alpha)

    fun getBackgroundColorHex(view: View): String? {
        val bg = view.background
        if (bg is ColorDrawable) {
            return "#${Integer.toHexString(bg.color).uppercase().padStart(8, '0')}"
        }
        return bg?.javaClass?.simpleName
    }

    fun getRotationString(view: View): String? {
        if (view.rotation == 0f && view.rotationX == 0f && view.rotationY == 0f) return null
        return "Z:${view.rotation} X:${view.rotationX} Y:${view.rotationY}"
    }

    fun getScaleString(view: View): String? {
        if (view.scaleX == 1f && view.scaleY == 1f) return null
        return "${view.scaleX} x ${view.scaleY}"
    }

    fun getStateFlags(view: View): String {
        val flags = mutableListOf<String>()
        if (view.isClickable) flags.add("clickable")
        if (view.isFocusable) flags.add("focusable")
        if (!view.isEnabled) flags.add("disabled")
        if (view.isSelected) flags.add("selected")
        if (view.isActivated) flags.add("activated")
        return if (flags.isEmpty()) "none" else flags.joinToString(", ")
    }

    fun buildParentChain(view: View, maxDepth: Int = 8): String {
        val chain = StringBuilder()
        var current: View? = view.parent as? View
        var depth = 0
        while (current != null && depth < maxDepth) {
            val indent = "  ".repeat(depth)
            val name = current.javaClass.simpleName
            val id = getResourceEntryName(current)
            chain.append("$indent> $name")
            if (id != null) chain.append(" (#$id)")
            chain.append("\n")
            current = current.parent as? View
            depth++
        }
        return chain.toString().trimEnd()
    }
}
