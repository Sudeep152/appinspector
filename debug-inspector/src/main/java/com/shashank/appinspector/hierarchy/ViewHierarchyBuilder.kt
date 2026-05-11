package com.shashank.appinspector.hierarchy

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import com.shashank.appinspector.DebugConfig
import com.shashank.appinspector.R
import com.shashank.appinspector.touch.ViewTypeColorResolver
import com.shashank.appinspector.utils.ResourceUtils
import java.lang.ref.WeakReference

internal data class ViewNode(
    val className: String,
    val idName: String?,
    val bounds: Rect,
    val depth: Int,
    val visibility: Int,
    val childCount: Int,
    val viewRef: WeakReference<View>,
    val children: List<ViewNode>
)

internal data class FlatViewNode(
    val className: String,
    val idName: String?,
    val depth: Int,
    val viewRef: WeakReference<View>,
    val hasChildren: Boolean,
    val isSelected: Boolean = false,
    val viewTypeColor: Int = 0,
    val childCountLabel: String = "",
    val isGone: Boolean = false,
    val isCollapsed: Boolean = false
)

internal class HierarchyTreeController(
    private val rootNode: ViewNode,
    private val selectedView: View?
) {
    private val collapsedKeys = mutableSetOf<Int>()

    fun getVisibleNodes(): List<FlatViewNode> {
        val result = mutableListOf<FlatViewNode>()
        flatten(rootNode, result, parentHidden = false)
        return result
    }

    fun toggleCollapse(node: FlatViewNode) {
        val key = System.identityHashCode(node.viewRef.get() ?: return)
        if (collapsedKeys.contains(key)) collapsedKeys.remove(key)
        else collapsedKeys.add(key)
    }

    private fun flatten(node: ViewNode, result: MutableList<FlatViewNode>, parentHidden: Boolean) {
        val view = node.viewRef.get()
        val isNodeCollapsed = view != null && collapsedKeys.contains(System.identityHashCode(view))

        if (!parentHidden) {
            result.add(
                FlatViewNode(
                    className = node.className,
                    idName = node.idName,
                    depth = node.depth,
                    viewRef = node.viewRef,
                    hasChildren = node.children.isNotEmpty(),
                    isSelected = view != null && view === selectedView,
                    viewTypeColor = if (view != null) ViewTypeColorResolver.getColor(view) else 0,
                    childCountLabel = if (node.children.isNotEmpty()) "${node.children.size}" else "",
                    isGone = node.visibility == View.GONE,
                    isCollapsed = isNodeCollapsed
                )
            )
        }

        for (child in node.children) {
            flatten(child, result, parentHidden = parentHidden || isNodeCollapsed)
        }
    }
}

internal object ViewHierarchyBuilder {

    fun buildTree(root: View, depth: Int = 0): ViewNode? {
        if (root.getTag(R.id.debug_inspector_tag) == DebugConfig.INSPECTOR_VIEW_TAG) return null

        val location = IntArray(2)
        root.getLocationOnScreen(location)
        val bounds = Rect(location[0], location[1], location[0] + root.width, location[1] + root.height)

        val children = mutableListOf<ViewNode>()
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val childNode = buildTree(root.getChildAt(i), depth + 1)
                if (childNode != null) children.add(childNode)
            }
        }

        return ViewNode(
            className = root.javaClass.simpleName,
            idName = ResourceUtils.getResourceEntryName(root),
            bounds = bounds,
            depth = depth,
            visibility = root.visibility,
            childCount = if (root is ViewGroup) root.childCount else 0,
            viewRef = WeakReference(root),
            children = children
        )
    }

    fun flattenTree(node: ViewNode, selectedView: View? = null): List<FlatViewNode> {
        return HierarchyTreeController(node, selectedView).getVisibleNodes()
    }
}
