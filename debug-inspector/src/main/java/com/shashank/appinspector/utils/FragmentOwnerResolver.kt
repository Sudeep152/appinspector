package com.shashank.appinspector.utils

import android.app.Activity
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

internal object FragmentOwnerResolver {

    fun findOwnerFragment(activity: Activity, targetView: View): Fragment? {
        val fragmentActivity = activity as? FragmentActivity ?: return null
        return findFragmentForView(fragmentActivity.supportFragmentManager, targetView)
    }

    fun getOwnerName(activity: Activity, targetView: View): String? {
        return findOwnerFragment(activity, targetView)?.javaClass?.simpleName
    }

    private fun findFragmentForView(fm: FragmentManager, targetView: View): Fragment? {
        for (fragment in fm.fragments) {
            if (!fragment.isAdded) continue
            if (fragment.view != null && isViewInsideFragment(fragment.view!!, targetView)) {
                val deeper = findFragmentForView(fragment.childFragmentManager, targetView)
                return deeper ?: fragment
            }
        }
        return null
    }

    private fun isViewInsideFragment(fragmentRoot: View, target: View): Boolean {
        if (fragmentRoot === target) return true
        var current: View? = target
        while (current != null) {
            if (current === fragmentRoot) return true
            current = current.parent as? View
        }
        return false
    }
}
