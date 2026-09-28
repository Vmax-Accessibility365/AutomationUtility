package com.example.autoutil.engine

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Minimal abstraction over the AccessibilityService that the engine needs. Kept as an
 * interface so WorkflowEngine does not depend directly on the Android Service class.
 */
interface AccessibilityRootProvider {
    /** Current active window's root node, or null if unavailable right now. */
    fun getRootNode(): AccessibilityNodeInfo?

    /** Best-effort diagnostic screenshot; null if unsupported or disabled. Never called for
     *  steps marked sensitive, and only when a profile has snapshots enabled. */
    suspend fun captureSnapshot(): ByteArray?
}
