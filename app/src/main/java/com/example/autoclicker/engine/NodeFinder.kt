package com.example.autoutil.engine

import android.view.accessibility.AccessibilityNodeInfo
import com.example.autoutil.data.TargetType

/**
 * Locates on-screen nodes. Search order is always targeted-first:
 *   1. viewId (findAccessibilityNodeInfosByViewId)
 *   2. exact/contains text match (findAccessibilityNodeInfosByText, filtered)
 *   3. contentDescription match (hierarchy walk, since there is no direct API for it)
 * A full-tree hierarchy walk is only ever used as the last-resort fallback for (3) and for
 * re-validating a previously found node, never as the default path — this keeps normal
 * step execution lightweight.
 */
class NodeFinder {

    /** Finds the first visible, non-stale node matching [type]/[value] under [root]. */
    fun find(root: AccessibilityNodeInfo, type: TargetType, value: String): AccessibilityNodeInfo? {
        if (value.isBlank()) return null
        val candidate = when (type) {
            TargetType.VIEW_ID -> findByViewId(root, value)
            TargetType.TEXT -> findByText(root, value)
            TargetType.CONTENT_DESC -> findByContentDescription(root, value)
        }
        return candidate?.takeIf { isUsable(it) }
    }

    private fun findByViewId(root: AccessibilityNodeInfo, viewId: String): AccessibilityNodeInfo? {
        val results = try {
            root.findAccessibilityNodeInfosByViewId(viewId)
        } catch (e: Exception) {
            null
        }
        return results?.firstOrNull { isUsable(it) }
    }

    private fun findByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val results = try {
            root.findAccessibilityNodeInfosByText(text)
        } catch (e: Exception) {
            null
        }
        // findAccessibilityNodeInfosByText does substring matching already; prefer an exact
        // match first, then fall back to the first usable substring match.
        val exact = results?.firstOrNull { it.text?.toString() == text && isUsable(it) }
        return exact ?: results?.firstOrNull { isUsable(it) }
    }

    private fun findByContentDescription(root: AccessibilityNodeInfo, desc: String): AccessibilityNodeInfo? {
        return hierarchyWalk(root) { node ->
            node.contentDescription?.toString() == desc
        }
    }

    /**
     * Bounded-depth hierarchy fallback search. Depth is capped so a deeply nested or
     * pathological view tree cannot cause unbounded scanning.
     */
    private fun hierarchyWalk(
        root: AccessibilityNodeInfo,
        maxDepth: Int = 60,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        val stack = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        stack.addLast(root to 0)
        while (stack.isNotEmpty()) {
            val (node, depth) = stack.removeLast()
            if (predicate(node) && isUsable(node)) return node
            if (depth >= maxDepth) continue
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                stack.addLast(child to depth + 1)
            }
        }
        return null
    }

    /**
     * Re-validates a previously located node right before an action is performed on it,
     * guarding against stale AccessibilityNodeInfo references (screen changed since it was
     * found).
     */
    fun isUsable(node: AccessibilityNodeInfo): Boolean {
        return try {
            node.refresh() && node.isVisibleToUser
        } catch (e: Exception) {
            false
        }
    }
}
