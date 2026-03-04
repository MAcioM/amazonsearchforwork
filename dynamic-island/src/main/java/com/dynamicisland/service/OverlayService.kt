package com.dynamicisland.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.dynamicisland.DynamicIslandApp
import com.dynamicisland.R
import com.dynamicisland.SettingsActivity
import com.dynamicisland.media.MediaCallbackManager
import com.dynamicisland.media.PhoneStateMonitor
import com.dynamicisland.overlay.DynamicIslandView
import com.dynamicisland.overlay.IslandEventBus
import com.dynamicisland.overlay.IslandState
import com.dynamicisland.settings.IslandPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OverlayService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.dynamicisland.STOP_SERVICE"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var islandView: DynamicIslandView
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var prefs: IslandPreferences
    private var mediaManager: MediaCallbackManager? = null
    private var phoneMonitor: PhoneStateMonitor? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var autoDismissJob: Job? = null
    private var isViewAdded = false

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        islandView.updateFromPreferences()
        updateLayoutPosition()
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = IslandPreferences(this)

        startForeground(NOTIFICATION_ID, buildNotification())
        setupOverlayView()
        collectEvents()
        setupMediaMonitor()

        prefs.registerOnChangeListener(prefsListener)
    }

    private fun setupOverlayView() {
        islandView = DynamicIslandView(this)

        val density = resources.displayMetrics.density
        val maxWidth = (prefs.getExpandedWidthDp() + 8f) * density
        val maxHeight = (prefs.getExpandedHeightDp() + 8f) * density

        layoutParams = WindowManager.LayoutParams(
            maxWidth.toInt(),
            maxHeight.toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = prefs.verticalOffset + (12 * density).toInt()
        }

        windowManager.addView(islandView, layoutParams)
        isViewAdded = true

        islandView.onExpandRequest = { expandIsland() }
        islandView.onCollapseRequest = { collapseIsland() }
    }

    private fun updateLayoutPosition() {
        if (!isViewAdded) return
        val density = resources.displayMetrics.density
        layoutParams.y = prefs.verticalOffset + (12 * density).toInt()
        windowManager.updateViewLayout(islandView, layoutParams)
    }

    private fun collectEvents() {
        serviceScope.launch {
            IslandEventBus.events.collect { state ->
                handleState(state)
            }
        }
    }

    private fun setupMediaMonitor() {
        if (prefs.musicEnabled) {
            mediaManager = MediaCallbackManager(this)
        }
        if (prefs.callsEnabled) {
            phoneMonitor = PhoneStateMonitor(this)
        }
    }

    private fun handleState(state: IslandState) {
        when (state) {
            is IslandState.Idle -> {
                autoDismissJob?.cancel()
                islandView.setState(IslandState.Idle)
            }
            is IslandState.Notification -> {
                if (!prefs.notificationsEnabled) return
                if (!prefs.isAppAllowed(state.packageName)) return
                showState(state)
            }
            is IslandState.Music -> {
                if (!prefs.musicEnabled) return
                if (!prefs.isAppAllowed(state.packageName)) return
                showState(state)
            }
            is IslandState.PhoneCall -> {
                if (!prefs.callsEnabled) return
                showState(state)
            }
            is IslandState.Timer -> {
                if (!prefs.timersEnabled) return
                showState(state)
            }
        }
    }

    private fun showState(state: IslandState) {
        islandView.setState(state)
        scheduleAutoDismiss(state)
    }

    private fun expandIsland() {
        autoDismissJob?.cancel()
        islandView.expand()
    }

    private fun collapseIsland() {
        islandView.collapse()
        scheduleAutoDismiss(null)
    }

    private fun scheduleAutoDismiss(state: IslandState?) {
        autoDismissJob?.cancel()
        // Don't auto-dismiss music or active calls
        if (state is IslandState.Music && state.isPlaying) return
        if (state is IslandState.PhoneCall && !state.isIncoming) return

        autoDismissJob = serviceScope.launch {
            delay(4000L)
            islandView.setState(IslandState.Idle)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, SettingsActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, DynamicIslandApp.SERVICE_CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        prefs.unregisterOnChangeListener(prefsListener)
        mediaManager?.release()
        phoneMonitor?.release()
        if (isViewAdded) {
            windowManager.removeView(islandView)
            isViewAdded = false
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
