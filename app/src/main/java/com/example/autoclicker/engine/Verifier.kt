package com.example.autoutil.engine

import android.view.accessibility.AccessibilityNodeInfo
import com.example.autoutil.data.Step

/**
 * Verifies a step's action actually took effect before letting the engine advance.
 * With no explicit verifyValue configured, verification falls back to confirming the acted-on
 * node is still resolvable and not stale (i.e. the UI didn't silently reject the action).
 */
class Verifier(private val nodeFinder: NodeFinder) {

    fun verify(root: AccessibilityNodeInfo, step: Step, actedNode: AccessibilityNodeInfo): Boolean {
        if (step.verifyValue.isBlank()) {
            return nodeFinder.isUsable(actedNode)
        }

        val byText = nodeFinder.find(root, com.example.autoutil.data.TargetType.TEXT, step.verifyValue)
        if (byText != null) return true

        val byViewId = nodeFinder.find(root, com.example.autoutil.data.TargetType.VIEW_ID, step.verifyValue)
        return byViewId != null
    }
}
