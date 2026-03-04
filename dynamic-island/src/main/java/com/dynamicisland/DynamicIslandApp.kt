package com.dynamicisland

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class DynamicIslandApp : Application() {

    companion object {
        const val SERVICE_CHANNEL_ID = "dynamic_island_service"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            getString(R.string.service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.service_channel_description)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
