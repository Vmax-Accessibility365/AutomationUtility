package com.example.autoutil.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.autoutil.engine.WorkflowStatus

/**
 * Fires a one-shot vibration/notification when a workflow finishes. Deliberately has no
 * mechanism to trigger a restart from here — completion feedback is display-only.
 */
class CompletionNotifier(private val context: Context) {

    fun notifyCompletion(status: WorkflowStatus, vibrate: Boolean, sound: Boolean) {
        if (vibrate) {
            vibrateFor(status)
        }
        // Sound playback intentionally uses the system notification sound via
        // AutomationForegroundService's NotificationCompat, rather than a raw MediaPlayer
        // here, to keep a single owner of notification state. See AutomationForegroundService.
        // 'sound' flag is read by the service when posting the final status notification.
        @Suppress("UNUSED_EXPRESSION")
        sound
    }

    private fun vibrateFor(status: WorkflowStatus) {
        val pattern = when (status) {
            WorkflowStatus.COMPLETED -> longArrayOf(0, 150, 80, 150)
            WorkflowStatus.FAILED -> longArrayOf(0, 400)
            WorkflowStatus.STOPPED -> longArrayOf(0, 100)
            else -> return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(VibratorManager::class.java)
                manager?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            // Vibration is a nice-to-have; never let it crash the workflow.
        }
    }
}
