package com.dynamicisland.overlay

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

sealed class IslandState {

    data object Idle : IslandState()

    data class Notification(
        val appName: String,
        val title: String,
        val text: String,
        val icon: Drawable?,
        val packageName: String
    ) : IslandState()

    data class Music(
        val title: String,
        val artist: String,
        val albumArt: Bitmap?,
        val isPlaying: Boolean,
        val packageName: String
    ) : IslandState()

    data class PhoneCall(
        val callerName: String?,
        val callerNumber: String,
        val isIncoming: Boolean
    ) : IslandState()

    data class Timer(
        val label: String,
        val remainingMs: Long
    ) : IslandState()
}
