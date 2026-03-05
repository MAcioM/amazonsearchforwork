package com.dynamicisland.settings

import android.content.Context
import android.content.SharedPreferences

class IslandPreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "dynamic_island_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SIZE = "size"
        private const val KEY_COLOR = "color"
        private const val KEY_ANIMATION_SPEED = "animation_speed"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_MUSIC_ENABLED = "music_enabled"
        private const val KEY_CALLS_ENABLED = "calls_enabled"
        private const val KEY_TIMERS_ENABLED = "timers_enabled"
        private const val KEY_FILTER_ALL_APPS = "filter_all_apps"
        private const val KEY_FILTERED_APPS = "filtered_apps"
        private const val KEY_VERTICAL_OFFSET = "vertical_offset"

        const val SIZE_SMALL = 0
        const val SIZE_MEDIUM = 1
        const val SIZE_LARGE = 2

        const val COLOR_BLACK = 0
        const val COLOR_DARK_GRAY = 1
        const val COLOR_ACCENT = 2

        const val SPEED_SLOW = 0
        const val SPEED_NORMAL = 1
        const val SPEED_FAST = 2
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var size: Int
        get() = prefs.getInt(KEY_SIZE, SIZE_MEDIUM)
        set(value) = prefs.edit().putInt(KEY_SIZE, value).apply()

    var color: Int
        get() = prefs.getInt(KEY_COLOR, COLOR_BLACK)
        set(value) = prefs.edit().putInt(KEY_COLOR, value).apply()

    var animationSpeed: Int
        get() = prefs.getInt(KEY_ANIMATION_SPEED, SPEED_NORMAL)
        set(value) = prefs.edit().putInt(KEY_ANIMATION_SPEED, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var musicEnabled: Boolean
        get() = prefs.getBoolean(KEY_MUSIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MUSIC_ENABLED, value).apply()

    var callsEnabled: Boolean
        get() = prefs.getBoolean(KEY_CALLS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CALLS_ENABLED, value).apply()

    var timersEnabled: Boolean
        get() = prefs.getBoolean(KEY_TIMERS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TIMERS_ENABLED, value).apply()

    var filterAllApps: Boolean
        get() = prefs.getBoolean(KEY_FILTER_ALL_APPS, true)
        set(value) = prefs.edit().putBoolean(KEY_FILTER_ALL_APPS, value).apply()

    var filteredApps: Set<String>
        get() = prefs.getStringSet(KEY_FILTERED_APPS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_FILTERED_APPS, value).apply()

    var verticalOffset: Int
        get() = prefs.getInt(KEY_VERTICAL_OFFSET, 0)
        set(value) = prefs.edit().putInt(KEY_VERTICAL_OFFSET, value).apply()

    fun getAnimationDurationMs(): Long = when (animationSpeed) {
        SPEED_SLOW -> 500L
        SPEED_NORMAL -> 300L
        SPEED_FAST -> 150L
        else -> 300L
    }

    fun getIslandColor(): Int = when (color) {
        COLOR_BLACK -> 0xFF000000.toInt()
        COLOR_DARK_GRAY -> 0xFF1A1A1A.toInt()
        COLOR_ACCENT -> 0xFF6750A4.toInt()
        else -> 0xFF000000.toInt()
    }

    fun getIslandWidthDp(): Float = when (size) {
        SIZE_SMALL -> 130f
        SIZE_MEDIUM -> 160f
        SIZE_LARGE -> 200f
        else -> 160f
    }

    fun getIslandHeightDp(): Float = when (size) {
        SIZE_SMALL -> 30f
        SIZE_MEDIUM -> 36f
        SIZE_LARGE -> 42f
        else -> 36f
    }

    fun getExpandedWidthDp(): Float = when (size) {
        SIZE_SMALL -> 280f
        SIZE_MEDIUM -> 320f
        SIZE_LARGE -> 360f
        else -> 320f
    }

    fun getExpandedHeightDp(): Float = when (size) {
        SIZE_SMALL -> 140f
        SIZE_MEDIUM -> 160f
        SIZE_LARGE -> 180f
        else -> 160f
    }

    fun isAppAllowed(packageName: String): Boolean {
        if (filterAllApps) return true
        return packageName in filteredApps
    }

    fun registerOnChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
