package com.example.autoutil.engine

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Sets text into a plain editable field: focuses it, then uses ACTION_SET_TEXT. Never invoked
 * for a node that SafetyGate has flagged as sensitive — callers must check SafetyGate first.
 */
class InputHandler {

    fun setText(node: AccessibilityNodeInfo, value: String): Boolean {
        if (!node.isEditable) return false

        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

        val arguments = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                value
            )
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    fun readText(node: AccessibilityNodeInfo): String {
        return node.text?.toString() ?: ""
    }
}
