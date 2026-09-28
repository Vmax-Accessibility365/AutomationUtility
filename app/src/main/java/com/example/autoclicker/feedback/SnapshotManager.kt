package com.example.autoutil.feedback

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Optional diagnostic capture on step timeout/error. Off by default at the profile level, and
 * never invoked for steps individually marked sensitive regardless of the profile setting —
 * that check happens in WorkflowEngine before this class is ever called.
 */
class SnapshotManager(private val context: Context) {

    fun save(bytes: ByteArray, stepId: String): String? {
        return try {
            val dir = File(context.filesDir, "snapshots").apply { mkdirs() }
            val file = File(dir, "snapshot_${stepId}_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { it.write(bytes) }
            file.absolutePath
        } catch (e: Exception) {
            Log.w("SnapshotManager", "Snapshot save failed: ${e.message}")
            null
        }
    }

    fun clearAll() {
        val dir = File(context.filesDir, "snapshots")
        if (dir.exists()) dir.listFiles()?.forEach { it.delete() }
    }
}
