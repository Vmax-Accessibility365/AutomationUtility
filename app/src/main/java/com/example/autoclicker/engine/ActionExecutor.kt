package com.example.autoutil.engine

import android.view.accessibility.AccessibilityNodeInfo
import com.example.autoutil.data.ActionType

/** Performs a single, already-safety-checked action on a located node. */
class ActionExecutor(private val inputHandler: InputHandler) {

    fun execute(node: AccessibilityNodeInfo, action: ActionType, resolvedInput: String): Boolean {
        return when (action) {
            ActionType.CLICK -> node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            ActionType.INPUT -> inputHandler.setText(node, resolvedInput)
            ActionType.SCROLL_FORWARD -> node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            ActionType.SCROLL_BACKWARD -> node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            ActionType.WAIT -> true
        }
    }
}
