package com.shashank.appinspector.context

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

internal data class ActivityContextInfo(
    val activitySimpleName: String,
    val activityFullName: String,
    val visibleFragmentName: String?,
    val visibleFragmentTag: String?,
    val backStackDepth: Int,
    val fragmentPath: String
)

internal object ActivityContextResolver {

    fun resolve(activity: Activity): ActivityContextInfo {
        val simpleName = activity.javaClass.simpleName
        val fullName = activity.javaClass.name

        val fragmentActivity = activity as? FragmentActivity
            ?: return ActivityContextInfo(
                activitySimpleName = simpleName,
                activityFullName = fullName,
                visibleFragmentName = null,
                visibleFragmentTag = null,
                backStackDepth = 0,
                fragmentPath = simpleName
            )

        val fm = fragmentActivity.supportFragmentManager
        val topFragment = findTopmostVisibleFragment(fm)
        val fragmentPath = buildFragmentPath(simpleName, fm)

        return ActivityContextInfo(
            activitySimpleName = simpleName,
            activityFullName = fullName,
            visibleFragmentName = topFragment?.javaClass?.simpleName,
            visibleFragmentTag = topFragment?.tag,
            backStackDepth = fm.backStackEntryCount,
            fragmentPath = fragmentPath
        )
    }

    private fun findTopmostVisibleFragment(fm: FragmentManager): Fragment? {
        val visibleFragments = fm.fragments.filter { it.isVisible && it.isAdded }
        val topFragment = visibleFragments.lastOrNull() ?: return null
        val deeperChild = findTopmostVisibleFragment(topFragment.childFragmentManager)
        return deeperChild ?: topFragment
    }

    private fun buildFragmentPath(activityName: String, fm: FragmentManager): String {
        val parts = mutableListOf(activityName)
        appendFragmentPath(fm, parts)
        return parts.joinToString(" › ")
    }

    private fun appendFragmentPath(fm: FragmentManager, parts: MutableList<String>) {
        val visible = fm.fragments.lastOrNull { it.isVisible && it.isAdded } ?: return
        parts.add(visible.javaClass.simpleName)
        appendFragmentPath(visible.childFragmentManager, parts)
    }
}
