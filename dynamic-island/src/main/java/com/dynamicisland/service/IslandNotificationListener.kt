package com.dynamicisland.service

import android.app.Notification
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.dynamicisland.overlay.IslandEventBus
import com.dynamicisland.overlay.IslandState
import com.dynamicisland.settings.IslandPreferences

class IslandNotificationListener : NotificationListenerService() {

    private lateinit var prefs: IslandPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = IslandPreferences(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Skip our own notifications
        if (sbn.packageName == packageName) return

        // Skip ongoing notifications (media, etc. — handled separately)
        if (sbn.isOngoing) {
            // Check if it's a timer from the clock app
            if (isTimerNotification(sbn)) {
                handleTimerNotification(sbn)
            }
            return
        }

        if (!prefs.notificationsEnabled) return
        if (!prefs.isAppAllowed(sbn.packageName)) return

        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        val appName = getAppName(sbn.packageName)
        val icon = getAppIcon(sbn.packageName)

        IslandEventBus.post(
            IslandState.Notification(
                appName = appName,
                title = title,
                text = text,
                icon = icon,
                packageName = sbn.packageName
            )
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Could dismiss the island if it's showing this notification
    }

    private fun isTimerNotification(sbn: StatusBarNotification): Boolean {
        return sbn.packageName == "com.google.android.deskclock" ||
                sbn.packageName == "com.android.deskclock"
    }

    private fun handleTimerNotification(sbn: StatusBarNotification) {
        if (!prefs.timersEnabled) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: "Timer"
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // Try to parse remaining time from the notification text
        val remainingMs = parseTimerText(text)

        IslandEventBus.post(
            IslandState.Timer(
                label = title,
                remainingMs = remainingMs
            )
        )
    }

    private fun parseTimerText(text: String): Long {
        // Try parsing "MM:SS" or "HH:MM:SS" format
        val parts = text.split(":").mapNotNull { it.trim().toLongOrNull() }
        return when (parts.size) {
            2 -> (parts[0] * 60 + parts[1]) * 1000
            3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
            else -> 0L
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName.substringAfterLast(".")
        }
    }

    private fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}
