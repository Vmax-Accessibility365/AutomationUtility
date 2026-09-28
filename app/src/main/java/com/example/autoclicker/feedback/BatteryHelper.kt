package com.example.autoutil.feedback

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Only ever opens Android's own battery-optimization settings screen so the user can choose
 * "Unrestricted" themselves. Deliberately does NOT call
 * REQUEST_IGNORE_BATTERY_OPTIMIZATIONS or attempt to silently obtain the exemption — the user
 * makes this choice explicitly in system settings.
 */
class BatteryHelper(private val context: Context) {

    fun openBatterySettings() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: general application details settings if the list screen isn't
            // available on this OEM/Android version.
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }
}
