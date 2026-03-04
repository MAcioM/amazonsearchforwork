package com.dynamicisland

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dynamicisland.service.OverlayService
import com.dynamicisland.settings.AppFilterAdapter
import com.dynamicisland.settings.IslandPreferences
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: IslandPreferences
    private var appFilterAdapter: AppFilterAdapter? = null

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updatePermissionStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = IslandPreferences(this)

        setupMasterToggle()
        setupPermissionButtons()
        setupAppearanceControls()
        setupFeatureToggles()
        setupAppFilter()
        updatePermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun setupMasterToggle() {
        val switch = findViewById<MaterialSwitch>(R.id.switch_master)
        switch.isChecked = prefs.isEnabled

        switch.setOnCheckedChangeListener { _, isChecked ->
            prefs.isEnabled = isChecked
            if (isChecked) {
                if (hasRequiredPermissions()) {
                    startOverlayService()
                } else {
                    switch.isChecked = false
                }
            } else {
                stopOverlayService()
            }
            updateStatusText()
        }
    }

    private fun setupPermissionButtons() {
        findViewById<View>(R.id.btn_overlay_permission).setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }

        findViewById<View>(R.id.btn_notification_permission).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun setupAppearanceControls() {
        // Size toggle
        val sizeToggle = findViewById<MaterialButtonToggleGroup>(R.id.toggle_size)
        val sizeButtonId = when (prefs.size) {
            IslandPreferences.SIZE_SMALL -> R.id.btn_size_small
            IslandPreferences.SIZE_MEDIUM -> R.id.btn_size_medium
            IslandPreferences.SIZE_LARGE -> R.id.btn_size_large
            else -> R.id.btn_size_medium
        }
        sizeToggle.check(sizeButtonId)
        sizeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                prefs.size = when (checkedId) {
                    R.id.btn_size_small -> IslandPreferences.SIZE_SMALL
                    R.id.btn_size_medium -> IslandPreferences.SIZE_MEDIUM
                    R.id.btn_size_large -> IslandPreferences.SIZE_LARGE
                    else -> IslandPreferences.SIZE_MEDIUM
                }
            }
        }

        // Color toggle
        val colorToggle = findViewById<MaterialButtonToggleGroup>(R.id.toggle_color)
        val colorButtonId = when (prefs.color) {
            IslandPreferences.COLOR_BLACK -> R.id.btn_color_black
            IslandPreferences.COLOR_DARK_GRAY -> R.id.btn_color_dark_gray
            IslandPreferences.COLOR_ACCENT -> R.id.btn_color_accent
            else -> R.id.btn_color_black
        }
        colorToggle.check(colorButtonId)
        colorToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                prefs.color = when (checkedId) {
                    R.id.btn_color_black -> IslandPreferences.COLOR_BLACK
                    R.id.btn_color_dark_gray -> IslandPreferences.COLOR_DARK_GRAY
                    R.id.btn_color_accent -> IslandPreferences.COLOR_ACCENT
                    else -> IslandPreferences.COLOR_BLACK
                }
            }
        }

        // Animation speed toggle
        val speedToggle = findViewById<MaterialButtonToggleGroup>(R.id.toggle_speed)
        val speedButtonId = when (prefs.animationSpeed) {
            IslandPreferences.SPEED_SLOW -> R.id.btn_speed_slow
            IslandPreferences.SPEED_NORMAL -> R.id.btn_speed_normal
            IslandPreferences.SPEED_FAST -> R.id.btn_speed_fast
            else -> R.id.btn_speed_normal
        }
        speedToggle.check(speedButtonId)
        speedToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                prefs.animationSpeed = when (checkedId) {
                    R.id.btn_speed_slow -> IslandPreferences.SPEED_SLOW
                    R.id.btn_speed_normal -> IslandPreferences.SPEED_NORMAL
                    R.id.btn_speed_fast -> IslandPreferences.SPEED_FAST
                    else -> IslandPreferences.SPEED_NORMAL
                }
            }
        }
    }

    private fun setupFeatureToggles() {
        setupSwitch(R.id.switch_notifications, prefs.notificationsEnabled) {
            prefs.notificationsEnabled = it
        }
        setupSwitch(R.id.switch_music, prefs.musicEnabled) {
            prefs.musicEnabled = it
        }
        setupSwitch(R.id.switch_calls, prefs.callsEnabled) {
            prefs.callsEnabled = it
        }
        setupSwitch(R.id.switch_timers, prefs.timersEnabled) {
            prefs.timersEnabled = it
        }
    }

    private fun setupSwitch(id: Int, initialValue: Boolean, onChanged: (Boolean) -> Unit) {
        val switch = findViewById<MaterialSwitch>(id)
        switch.isChecked = initialValue
        switch.setOnCheckedChangeListener { _, isChecked -> onChanged(isChecked) }
    }

    private fun setupAppFilter() {
        val allAppsSwitch = findViewById<MaterialSwitch>(R.id.switch_all_apps)
        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_apps)

        allAppsSwitch.isChecked = prefs.filterAllApps
        recyclerView.visibility = if (prefs.filterAllApps) View.GONE else View.VISIBLE

        allAppsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.filterAllApps = isChecked
            recyclerView.visibility = if (isChecked) View.GONE else View.VISIBLE
            if (!isChecked && appFilterAdapter == null) {
                loadAppList(recyclerView)
            }
        }

        if (!prefs.filterAllApps) {
            loadAppList(recyclerView)
        }
    }

    private fun loadAppList(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }
            .map { AppFilterAdapter.AppInfo(
                packageName = it.packageName,
                label = pm.getApplicationLabel(it).toString(),
                icon = pm.getApplicationIcon(it)
            ) }

        val selectedApps = prefs.filteredApps.toMutableSet()
        appFilterAdapter = AppFilterAdapter(apps, selectedApps) { updatedSet ->
            prefs.filteredApps = updatedSet
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = appFilterAdapter
    }

    private fun hasRequiredPermissions(): Boolean {
        return Settings.canDrawOverlays(this) && isNotificationListenerEnabled()
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val listeners = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return listeners.contains(ComponentName(this,
            com.dynamicisland.service.IslandNotificationListener::class.java).flattenToString())
    }

    private fun updatePermissionStatus() {
        val overlayBtn = findViewById<View>(R.id.btn_overlay_permission)
        val notifBtn = findViewById<View>(R.id.btn_notification_permission)

        overlayBtn.isEnabled = !Settings.canDrawOverlays(this)
        notifBtn.isEnabled = !isNotificationListenerEnabled()

        updateStatusText()
    }

    private fun updateStatusText() {
        val statusText = findViewById<android.widget.TextView>(R.id.text_status)
        val overlayOk = Settings.canDrawOverlays(this)
        val notifOk = isNotificationListenerEnabled()
        val serviceRunning = prefs.isEnabled

        statusText.text = when {
            !overlayOk -> "Overlay permission needed"
            !notifOk -> "Notification access needed"
            serviceRunning -> "Dynamic Island is active"
            else -> "Dynamic Island is disabled"
        }
        statusText.setTextColor(
            ContextCompat.getColor(this,
                if (serviceRunning && overlayOk && notifOk) R.color.status_active
                else R.color.status_inactive
            )
        )
    }

    private fun startOverlayService() {
        ContextCompat.startForegroundService(
            this, Intent(this, OverlayService::class.java)
        )
    }

    private fun stopOverlayService() {
        stopService(Intent(this, OverlayService::class.java))
    }
}
