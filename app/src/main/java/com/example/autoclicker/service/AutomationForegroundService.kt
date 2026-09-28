package com.example.autoutil.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import com.example.autoutil.R
import com.example.autoutil.engine.AutomationController
import com.example.autoutil.engine.WorkflowStatus
import com.example.autoutil.ui.MainActivity

/**
 * Shows the required foreground-service notification while a workflow is running, with a
 * visible STOP action. Lifecycle is tied to AutomationController's status: the service starts
 * itself when a run begins and stops itself once the workflow reaches a terminal state.
 */
class AutomationForegroundService : Service() {

    // STOP notification action is handled by the manifest-registered StopReceiver, which
    // calls AutomationController.stop() directly — no receiver is registered here to avoid
    // a duplicate handler for the same broadcast.

    private val statusObserver = Observer<WorkflowStatus> { status ->
        when (status) {
            WorkflowStatus.RUNNING -> updateNotification(getString(R.string.status_running))
            WorkflowStatus.COMPLETED -> {
                updateNotification(getString(R.string.status_completed))
                stopSelf()
            }
            WorkflowStatus.FAILED -> {
                updateNotification(getString(R.string.status_failed))
                stopSelf()
            }
            WorkflowStatus.STOPPED -> {
                updateNotification(getString(R.string.status_stopped))
                stopSelf()
            }
            WorkflowStatus.IDLE -> Unit
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannelIfNeeded()
        AutomationController.statusLiveData.observeForever(statusObserver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.status_running)))
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        AutomationController.statusLiveData.removeObserver(statusObserver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Explicit component target (not an implicit action broadcast) so the statically
        // declared, non-exported StopReceiver always receives it regardless of intent-filter
        // matching rules.
        val stopIntent = PendingIntent.getBroadcast(
            this, 0,
            Intent(this, StopReceiver::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, getString(R.string.action_stop), stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val existing = manager?.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.notification_channel_desc)
                }
                manager?.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        const val ACTION_STOP = "com.example.autoutil.ACTION_STOP"
        private const val CHANNEL_ID = "automation_running_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
