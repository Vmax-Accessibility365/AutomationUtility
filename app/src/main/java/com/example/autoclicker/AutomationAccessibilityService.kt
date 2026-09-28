package com.example.autoutil.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.hardware.HardwareBuffer
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.autoutil.engine.AccessibilityRootProvider
import com.example.autoutil.engine.AutomationController
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The only component that actually talks to Android's Accessibility APIs. It only acts when
 * WorkflowEngine (via AutomationController) explicitly asks it to — this service does not run
 * any automation on its own from onAccessibilityEvent.
 */
class AutomationAccessibilityService : AccessibilityService(), AccessibilityRootProvider {

    override fun onServiceConnected() {
        super.onServiceConnected()
        AutomationController.init(applicationContext)
        AutomationController.attachProvider(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally empty: the engine pulls the root node on demand (see getRootNode())
        // rather than reacting to every event, which keeps event processing lightweight and
        // avoids blocking the accessibility event pipeline.
    }

    override fun onInterrupt() {
        // No-op: nothing to clean up mid-gesture: actions are atomic node calls.
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        AutomationController.detachProvider()
        return super.onUnbind(intent)
    }

    override fun getRootNode(): AccessibilityNodeInfo? {
        return try {
            rootInActiveWindow
        } catch (e: Exception) {
            Log.w(TAG, "getRootNode failed: ${e.message}")
            null
        }
    }

    override suspend fun captureSnapshot(): ByteArray? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null

        return try {
            suspendCancellableCoroutine { cont ->
                takeScreenshot(
                    android.view.Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(result: ScreenshotResult) {
                            val bytes = try {
                                val hwBitmap = Bitmap.wrapHardwareBuffer(
                                    result.hardwareBuffer,
                                    result.colorSpace
                                )
                                val bitmap = hwBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                                result.hardwareBuffer.close()
                                if (bitmap != null) {
                                    val stream = ByteArrayOutputStream()
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
                                    stream.toByteArray()
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                            if (cont.isActive) cont.resume(bytes)
                        }

                        override fun onFailure(errorCode: Int) {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Optional, best-effort emergency stop via the volume-down key. This is NOT guaranteed on
     * every device/Android version — accessibility key-event filtering behaves differently
     * across OEMs — so the in-app STOP button remains the primary, reliable emergency stop.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
            if (AutomationController.isRunning()) {
                AutomationController.stop()
                return true
            }
        }
        return super.onKeyEvent(event)
    }

    companion object {
        private const val TAG = "AutomationAccService"
    }
}
