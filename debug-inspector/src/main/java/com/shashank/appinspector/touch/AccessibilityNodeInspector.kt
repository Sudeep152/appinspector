package com.shashank.appinspector.touch

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider

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
        if (name.contains("AndroidComposeView")) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findComposeViewIn(root.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    fun isComposeOnlyView(view: View): Boolean =
        view.javaClass.name.contains("AndroidComposeView") ||
        view.javaClass.name.contains("ComposeView")

    fun findNodeAt(view: View, screenX: Int, screenY: Int): NodeInfo? {
        val provider = view.accessibilityNodeProvider
        return if (provider != null) {
            findInProvider(provider, AccessibilityNodeProvider.HOST_VIEW_ID, screenX, screenY)
        } else {
            val root = view.createAccessibilityNodeInfo() ?: return null
            findInViewNode(root, screenX, screenY)
        }
    }

    fun getAllNodes(view: View): List<NodeInfo> {
        val provider = view.accessibilityNodeProvider
        return if (provider != null) {
            val results = mutableListOf<NodeInfo>()
            collectFromProvider(provider, AccessibilityNodeProvider.HOST_VIEW_ID, results, mutableSetOf())
            results
        } else {
            val root = view.createAccessibilityNodeInfo() ?: return emptyList()
            val results = mutableListOf<NodeInfo>()
            collectFromViewNode(root, results)
            results
        }
    }

    // ---------------------------------------------------------------------------
    // Provider traversal (Compose / virtual-view trees)
    // ---------------------------------------------------------------------------

    private fun findInProvider(
        provider: AccessibilityNodeProvider,
        virtualId: Int,
        x: Int,
        y: Int,
        visited: MutableSet<Int> = mutableSetOf()
    ): NodeInfo? {
        if (!visited.add(virtualId)) return null
        val node = provider.createAccessibilityNodeInfo(virtualId) ?: return null
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (!rect.contains(x, y)) return null

        var best: NodeInfo? = null
        for (childId in resolveChildVirtualIds(node, provider)) {
            val childResult = findInProvider(provider, childId, x, y, visited)
            if (childResult != null) best = childResult
        }
        return best ?: if (!rect.isEmpty) nodeInfoFrom(node) else null
    }

    private fun collectFromProvider(
        provider: AccessibilityNodeProvider,
        virtualId: Int,
        results: MutableList<NodeInfo>,
        visited: MutableSet<Int>
    ) {
        if (!visited.add(virtualId)) return
        val node = provider.createAccessibilityNodeInfo(virtualId) ?: return
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (!rect.isEmpty) results.add(nodeInfoFrom(node))
        for (childId in resolveChildVirtualIds(node, provider)) {
            collectFromProvider(provider, childId, results, visited)
        }
    }

    /**
     * Returns the virtual IDs for the children of [node].
     *
     * Strategy 1 – reflection on AccessibilityNodeInfo.mChildNodeIds (fast path).
     * Strategy 2 – brute-force probe of small integer IDs (fallback when reflection
     *              fails or the provider hasn't populated child IDs yet).
     * Compose assigns semantic IDs from a global AtomicInteger starting at 1, so a
     * probe range of 1..MAX_PROBE covers virtually every real-world composition.
     */
    private fun resolveChildVirtualIds(
        node: AccessibilityNodeInfo,
        provider: AccessibilityNodeProvider
    ): List<Int> {
        if (node.childCount == 0) return emptyList()

        val reflectionIds = childVirtualIdsViaReflection(node)
        if (reflectionIds.isNotEmpty()) return reflectionIds

        // Reflection failed — probe small positive IDs that return a valid node.
        return probeChildIds(provider, node.childCount)
    }

    private const val MAX_PROBE = 1000

    /**
     * Probes IDs 1..MAX_PROBE and returns those that the provider resolves to a
     * non-null node with non-empty bounds. Used only when reflection fails.
     */
    private fun probeChildIds(provider: AccessibilityNodeProvider, expectedCount: Int): List<Int> {
        val found = mutableListOf<Int>()
        val rect = Rect()
        for (id in 1..MAX_PROBE) {
            val candidate = provider.createAccessibilityNodeInfo(id) ?: continue
            candidate.getBoundsInScreen(rect)
            if (!rect.isEmpty) {
                found.add(id)
                if (found.size >= expectedCount * 3) break // generous upper bound
            }
        }
        return found
    }

    // ---------------------------------------------------------------------------
    // Reflection-based child ID extraction from AccessibilityNodeInfo
    // ---------------------------------------------------------------------------

    private fun childVirtualIdsViaReflection(node: AccessibilityNodeInfo): List<Int> {
        return try {
            val field = AccessibilityNodeInfo::class.java.getDeclaredField("mChildNodeIds")
            field.isAccessible = true
            val obj = field.get(node) ?: return emptyList()
            extractLongs(obj).map { packed -> (packed ushr 32).toInt() }.filter { it > 0 }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun extractLongs(obj: Any): List<Long> {
        if (obj is LongArray) return obj.toList()
        // android.util.LongArray (API 29+) — fields: long[] mValues, int mSize
        return try {
            val valuesField = obj.javaClass.getDeclaredField("mValues").also { it.isAccessible = true }
            val sizeField = obj.javaClass.getDeclaredField("mSize").also { it.isAccessible = true }
            val values = valuesField.get(obj) as LongArray
            val size = sizeField.getInt(obj)
            values.take(size)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ---------------------------------------------------------------------------
    // Regular (non-virtual) view accessibility node traversal
    // ---------------------------------------------------------------------------

    private fun findInViewNode(node: AccessibilityNodeInfo, x: Int, y: Int): NodeInfo? {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (!rect.contains(x, y)) return null
        var best: NodeInfo? = null
        for (i in 0 until node.childCount) {
            val child = safeGetChild(node, i) ?: continue
            val childResult = findInViewNode(child, x, y)
            if (childResult != null) best = childResult
        }
        return best ?: if (!rect.isEmpty) nodeInfoFrom(node) else null
    }

    private fun collectFromViewNode(node: AccessibilityNodeInfo, results: MutableList<NodeInfo>) {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (!rect.isEmpty) results.add(nodeInfoFrom(node))
        for (i in 0 until node.childCount) {
            val child = safeGetChild(node, i) ?: continue
            collectFromViewNode(child, results)
        }
    }

    private fun safeGetChild(node: AccessibilityNodeInfo, index: Int): AccessibilityNodeInfo? =
        try { node.getChild(index) } catch (_: Exception) { null }

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
