package com.example.autoutil.engine

import android.view.accessibility.AccessibilityNodeInfo
import com.example.autoutil.data.InterruptionRule

/** Outcome of scanning the current foreground window for a known/allowed interruption. */
sealed class InterruptionCheck {
    object None : InterruptionCheck()
    data class Handled(val rule: InterruptionRule) : InterruptionCheck()
    data class UnknownBlocking(val windowText: String) : InterruptionCheck()
}

/**
 * Checks the active window against the profile's explicit allow-list before each step.
 * A window is only ever acted on when it matches a configured rule; anything else that looks
 * like a dialog/popup is reported as UnknownBlocking so the engine can pause and notify the
 * user instead of guessing.
 */
class InterruptionGuard(private val nodeFinder: NodeFinder) {

    fun check(root: AccessibilityNodeInfo, rules: List<InterruptionRule>): InterruptionCheck {
        val windowText = collectText(root, maxDepth = 15)

        for (rule in rules) {
            if (windowText.contains(rule.dialogTextContains, ignoreCase = true)) {
                return InterruptionCheck.Handled(rule)
            }
        }

        if (looksLikeUnknownDialog(root)) {
            return InterruptionCheck.UnknownBlocking(windowText.take(300))
        }

        return InterruptionCheck.None
    }

    fun performHandledAction(root: AccessibilityNodeInfo, rule: InterruptionRule): Boolean {
        val node = nodeFinder.find(root, rule.buttonTargetType, rule.buttonTargetValue) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /**
     * Heuristic only: a window whose class name suggests a system/app dialog and that is not
     * matched by any allow-list rule counts as an unknown interruption. This deliberately does
     * NOT click anything — it only decides whether to report a block.
     */
    private fun looksLikeUnknownDialog(root: AccessibilityNodeInfo): Boolean {
        val className = root.className?.toString() ?: return false
        return className.contains("Dialog", ignoreCase = true) ||
            className.contains("AlertDialog", ignoreCase = true) ||
            className.contains("PopupWindow", ignoreCase = true)
    }

    private fun collectText(root: AccessibilityNodeInfo, maxDepth: Int): String {
        val sb = StringBuilder()
        val stack = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        stack.addLast(root to 0)
        var visited = 0
        while (stack.isNotEmpty() && visited < 400) {
            val (node, depth) = stack.removeLast()
            visited++
            node.text?.let { sb.append(it).append(' ') }
            node.contentDescription?.let { sb.append(it).append(' ') }
            if (depth >= maxDepth) continue
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                stack.addLast(child to depth + 1)
            }
        }
        return sb.toString()
    }
}
