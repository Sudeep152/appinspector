package com.shashank.appinspector.touch

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo

internal object AccessibilityNodeInspector {

    data class NodeInfo(
        val className: String,
        val text: String?,
        val contentDescription: String?,
        val boundsInScreen: Rect,
        val isClickable: Boolean,
        val isEnabled: Boolean,
        val resourceId: String?
    ) {
        val widthPx: Int get() = boundsInScreen.width()
        val heightPx: Int get() = boundsInScreen.height()
    }

    fun findComposeViewIn(root: View): View? {
        val name = root.javaClass.name
        if (name.contains("ComposeView") || name.contains("AndroidComposeView")) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findComposeViewIn(root.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    fun isComposeOnlyView(view: View): Boolean {
        val name = view.javaClass.name
        return name.contains("ComposeView") || name.contains("AndroidComposeView")
    }

    fun findNodeAt(view: View, screenX: Int, screenY: Int): NodeInfo? {
        val rootNode = view.createAccessibilityNodeInfo() ?: return null
        val found = findDeepestNodeAt(rootNode, screenX, screenY) ?: return null
        return nodeInfoFrom(found)
    }

    fun findAllNodesAt(view: View, screenX: Int, screenY: Int): List<NodeInfo> {
        val rootNode = view.createAccessibilityNodeInfo() ?: return emptyList()
        val results = mutableListOf<AccessibilityNodeInfo>()
        collectNodesAt(rootNode, screenX, screenY, results)
        return results.map { nodeInfoFrom(it) }
    }

    fun getAllNodes(view: View): List<NodeInfo> {
        val rootNode = view.createAccessibilityNodeInfo() ?: return emptyList()
        val results = mutableListOf<AccessibilityNodeInfo>()
        collectAllNodes(rootNode, results)
        return results.map { nodeInfoFrom(it) }
    }

    private fun findDeepestNodeAt(node: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (!rect.contains(x, y)) return null
        var deepest: AccessibilityNodeInfo? = null
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val childResult = findDeepestNodeAt(child, x, y)
            if (childResult != null) deepest = childResult
        }
        return deepest ?: node
    }

    private fun collectNodesAt(node: AccessibilityNodeInfo, x: Int, y: Int, results: MutableList<AccessibilityNodeInfo>) {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.contains(x, y)) results.add(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectNodesAt(child, x, y, results)
        }
    }

    private fun collectAllNodes(node: AccessibilityNodeInfo, results: MutableList<AccessibilityNodeInfo>) {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (!rect.isEmpty) results.add(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectAllNodes(child, results)
        }
    }

    private fun nodeInfoFrom(node: AccessibilityNodeInfo): NodeInfo {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return NodeInfo(
            className = node.className?.toString()?.substringAfterLast('.') ?: "Composable",
            text = node.text?.toString()?.takeIf { it.isNotBlank() },
            contentDescription = node.contentDescription?.toString()?.takeIf { it.isNotBlank() },
            boundsInScreen = rect,
            isClickable = node.isClickable,
            isEnabled = node.isEnabled,
            resourceId = node.viewIdResourceName?.substringAfterLast('/')
        )
    }
}
