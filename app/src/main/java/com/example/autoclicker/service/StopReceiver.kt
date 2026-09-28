package com.example.autoutil.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.autoutil.engine.AutomationController

/** Handles the STOP action tapped from the foreground-service notification. */
class StopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AutomationForegroundService.ACTION_STOP) {
            AutomationController.stop()
        }
    }
}
